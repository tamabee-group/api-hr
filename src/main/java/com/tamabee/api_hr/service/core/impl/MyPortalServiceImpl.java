package com.tamabee.api_hr.service.core.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.tamabee.api_hr.datasource.RegionContext;
import com.tamabee.api_hr.util.RegionUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.dto.request.user.UpdateMyProfileRequest;
import com.tamabee.api_hr.dto.response.payroll.PayrollItemResponse;
import com.tamabee.api_hr.dto.response.payroll.PayrollRecordResponse;
import com.tamabee.api_hr.dto.response.portal.ContractResponse;
import com.tamabee.api_hr.dto.response.portal.DocumentResponse;
import com.tamabee.api_hr.dto.response.portal.MyProfileResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.entity.payroll.PayrollItemEntity;
import com.tamabee.api_hr.entity.payroll.PayrollPeriodEntity;
import com.tamabee.api_hr.entity.user.EmployeeDocumentEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.entity.user.UserProfileEntity;
import com.tamabee.api_hr.enums.DocumentType;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.PayrollItemStatus;
import com.tamabee.api_hr.enums.PayrollPeriodStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.PayrollPeriodMapper;
import com.tamabee.api_hr.mapper.core.MyPortalMapper;
import com.tamabee.api_hr.repository.contract.EmploymentContractRepository;
import com.tamabee.api_hr.repository.payroll.PayrollItemRepository;
import com.tamabee.api_hr.repository.payroll.PayrollPeriodRepository;
import com.tamabee.api_hr.repository.user.EmployeeDocumentRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.core.PayslipPdfGenerator;
import com.tamabee.api_hr.service.core.interfaces.IMyPortalService;
import com.tamabee.api_hr.service.core.interfaces.IUploadService;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation cho Employee Portal - xử lý các thao tác self-service của nhân viên
 */
@Slf4j
@Service
public class MyPortalServiceImpl implements IMyPortalService {

    // Các loại file avatar được phép upload
    private static final Set<String> AVATAR_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    // Các loại file document được phép upload (bao gồm cả PDF)
    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf"
    );

    private final PayrollItemRepository payrollItemRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final UserRepository userRepository;
    private final EmploymentContractRepository employmentContractRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final MyPortalMapper myPortalMapper;
    private final PayrollPeriodMapper payrollPeriodMapper;
    private final PayslipPdfGenerator pdfGenerator;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate masterJdbcTemplate;
    private final IUploadService uploadService;

    public MyPortalServiceImpl(
            PayrollItemRepository payrollItemRepository,
            PayrollPeriodRepository payrollPeriodRepository,
            UserRepository userRepository,
            EmploymentContractRepository employmentContractRepository,
            EmployeeDocumentRepository employeeDocumentRepository,
            MyPortalMapper myPortalMapper,
            PayrollPeriodMapper payrollPeriodMapper,
            PayslipPdfGenerator pdfGenerator,
            ObjectMapper objectMapper,
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            IUploadService uploadService) {
        this.payrollItemRepository = payrollItemRepository;
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.userRepository = userRepository;
        this.employmentContractRepository = employmentContractRepository;
        this.employeeDocumentRepository = employeeDocumentRepository;
        this.myPortalMapper = myPortalMapper;
        this.payrollPeriodMapper = payrollPeriodMapper;
        this.pdfGenerator = pdfGenerator;
        this.objectMapper = objectMapper;
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.uploadService = uploadService;
    }

    // ==================== Payslip Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollItemResponse> getMyPayslips(Long employeeId, Integer year, String status, Pageable pageable) {
        // Nhân viên chỉ được xem phiếu lương từ kỳ lương đã PAID (đã thanh toán)
        List<PayrollPeriodStatus> visibleStatuses = List.of(PayrollPeriodStatus.PAID);

        // Lấy danh sách periods theo năm (nếu có filter), chỉ lấy periods đã duyệt
        List<PayrollPeriodEntity> periods;
        if (year != null) {
            periods = payrollPeriodRepository.findByYearAndStatusIn(year, visibleStatuses);
        } else {
            periods = payrollPeriodRepository.findByStatusIn(visibleStatuses);
        }

        // Nếu không có period nào, trả về empty page
        if (periods.isEmpty()) {
            return Page.empty(pageable);
        }

        // Tạo map period ID -> period entity để lookup nhanh
        Map<Long, PayrollPeriodEntity> periodMap = periods.stream()
                .collect(Collectors.toMap(PayrollPeriodEntity::getId, p -> p));
        Set<Long> periodIds = periodMap.keySet();

        // Lấy thông tin employee để map
        UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));
        Map<Long, UserEntity> userMap = Map.of(employeeId, employee);

        // Query payroll items theo employee và period IDs
        Page<PayrollItemEntity> itemsPage;
        
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "payrollPeriodId")
        );

        if (status != null && !status.isEmpty()) {
            PayrollItemStatus itemStatus = PayrollItemStatus.valueOf(status);
            List<PayrollItemEntity> allItems = payrollItemRepository.findByEmployeeId(employeeId).stream()
                    .filter(item -> periodIds.contains(item.getPayrollPeriodId()))
                    .filter(item -> item.getStatus() == itemStatus)
                    .collect(Collectors.toList());
            
            // Sort theo year, month DESC
            allItems.sort((a, b) -> {
                PayrollPeriodEntity periodA = periodMap.get(a.getPayrollPeriodId());
                PayrollPeriodEntity periodB = periodMap.get(b.getPayrollPeriodId());
                int yearCompare = periodB.getYear().compareTo(periodA.getYear());
                if (yearCompare != 0) {
                    return yearCompare;
                }
                return periodB.getMonth().compareTo(periodA.getMonth());
            });

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allItems.size());
            List<PayrollItemEntity> pagedItems = start < allItems.size() 
                    ? allItems.subList(start, end) 
                    : List.of();
            
            itemsPage = new PageImpl<>(pagedItems, pageable, allItems.size());
        } else {
            itemsPage = payrollItemRepository.findByEmployeeIdAndPayrollPeriodIdIn(
                    employeeId, periodIds, sortedPageable);
        }

        // Map sang PayrollItemResponse sử dụng PayrollPeriodMapper
        List<PayrollItemResponse> responses = itemsPage.getContent().stream()
                .map(item -> {
                    PayrollPeriodEntity period = periodMap.get(item.getPayrollPeriodId());
                    return payrollPeriodMapper.toItemResponse(item, userMap, 
                            period.getYear(), period.getMonth(), period.getPaidAt());
                })
                .collect(Collectors.toList());

        // Sort responses theo year, month DESC
        if (status == null || status.isEmpty()) {
            responses.sort((a, b) -> {
                int yearCompare = b.getYear().compareTo(a.getYear());
                if (yearCompare != 0) {
                    return yearCompare;
                }
                return b.getMonth().compareTo(a.getMonth());
            });
        }

        return new PageImpl<>(responses, pageable, itemsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollItemResponse getPayslipDetail(Long employeeId, Long itemId) {
        // Tìm payroll item theo ID
        PayrollItemEntity item = payrollItemRepository.findById(itemId)
                .orElseThrow(() -> NotFoundException.payrollRecord(itemId));

        // Kiểm tra quyền sở hữu - trả về 404 nếu không phải của nhân viên này
        if (!item.getEmployeeId().equals(employeeId)) {
            throw NotFoundException.payrollRecord(itemId);
        }

        // Lấy thông tin period để có year, month và payment date
        PayrollPeriodEntity period = payrollPeriodRepository.findById(item.getPayrollPeriodId())
                .orElseThrow(() -> NotFoundException.payrollRecord(itemId));

        // Nhân viên chỉ được xem phiếu lương từ kỳ lương đã PAID
        if (period.getStatus() != PayrollPeriodStatus.PAID) {
            throw NotFoundException.payrollRecord(itemId);
        }

        // Lấy thông tin employee
        UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));
        Map<Long, UserEntity> userMap = Map.of(employeeId, employee);

        return payrollPeriodMapper.toItemResponse(item, userMap, period.getYear(), period.getMonth(), period.getPaidAt());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadPayslipPdf(Long employeeId, Long itemId) {
        // Tìm payroll item theo ID
        PayrollItemEntity item = payrollItemRepository.findById(itemId)
                .orElseThrow(() -> NotFoundException.payrollRecord(itemId));

        // Kiểm tra quyền sở hữu - trả về 404 nếu không phải của nhân viên này
        if (!item.getEmployeeId().equals(employeeId)) {
            throw NotFoundException.payrollRecord(itemId);
        }

        // Lấy thông tin period để có year, month
        PayrollPeriodEntity period = payrollPeriodRepository.findById(item.getPayrollPeriodId())
                .orElseThrow(() -> NotFoundException.payrollRecord(itemId));

        // Nhân viên chỉ được download phiếu lương từ kỳ lương đã PAID
        if (period.getStatus() != PayrollPeriodStatus.PAID) {
            throw NotFoundException.payrollRecord(itemId);
        }

        // Lấy thông tin employee với profile
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));

        // Lấy thông tin company từ master DB bằng JDBC
        String tenantDomain = TenantContext.getCurrentTenant();
        CompanyEntity company = null;
        if (tenantDomain != null) {
            company = getCompanyFromMasterDb(tenantDomain);
        }

        // Convert PayrollItemEntity sang PayrollRecordResponse để dùng với PayslipPdfGenerator
        PayrollRecordResponse recordResponse = convertItemToRecordResponse(item, period);

        // Sử dụng PayslipPdfGenerator để tạo PDF
        return pdfGenerator.generate(recordResponse, employee, company);
    }

    /**
     * Lấy thông tin company từ master DB bằng JDBC
     */
    private CompanyEntity getCompanyFromMasterDb(String tenantDomain) {
        String sql = "SELECT id, name, email, region, logo FROM companies WHERE tenant_domain = ? AND deleted = false";
        
        try {
            return masterJdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                CompanyEntity company = new CompanyEntity();
                company.setId(rs.getLong("id"));
                company.setName(rs.getString("name"));
                company.setEmail(rs.getString("email"));
                company.setRegion(rs.getString("region"));
                company.setLogo(rs.getString("logo"));
                return company;
            }, tenantDomain);
        } catch (Exception e) {
            log.warn("Không tìm thấy company với tenant: {}", tenantDomain);
            return null;
        }
    }

    /**
     * Convert PayrollItemEntity sang PayrollRecordResponse để tương thích với PayslipPdfGenerator
     */
    private PayrollRecordResponse convertItemToRecordResponse(PayrollItemEntity item, PayrollPeriodEntity period) {
        PayrollRecordResponse response = new PayrollRecordResponse();
        
        response.setYear(period.getYear());
        response.setMonth(period.getMonth());
        response.setEmployeeId(item.getEmployeeId());
        response.setSalaryType(item.getSalaryType());
        response.setSalaryRate(item.getBaseSalary());
        response.setBaseSalary(item.getCalculatedBaseSalary());
        response.setWorkingDays(item.getWorkingDays());
        response.setWorkingHours(item.getWorkingMinutes());
        response.setRegularOvertimeHours(item.getRegularOvertimeMinutes());
        response.setNightOvertimeHours(item.getNightOvertimeMinutes());
        response.setHolidayOvertimeHours(item.getHolidayOvertimeMinutes());
        response.setTotalOvertimePay(item.getTotalOvertimePay());
        response.setGrossSalary(item.getGrossSalary());
        response.setTotalDeductions(item.getTotalDeductions());
        response.setNetSalary(item.getNetSalary());

        // Parse allowance details từ JSON
        if (item.getAllowanceDetails() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<PayrollItemResponse.AllowanceDetailResponse> allowanceDetails = 
                    objectMapper.readValue(item.getAllowanceDetails(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, 
                            PayrollItemResponse.AllowanceDetailResponse.class));
                
                List<PayrollRecordResponse.AllowanceItemResponse> allowances = 
                    allowanceDetails.stream()
                        .map(a -> {
                            PayrollRecordResponse.AllowanceItemResponse allowanceItem = 
                                new PayrollRecordResponse.AllowanceItemResponse();
                            allowanceItem.setCode(a.getCode());
                            allowanceItem.setName(a.getName());
                            allowanceItem.setAmount(a.getAmount());
                            return allowanceItem;
                        })
                        .collect(Collectors.toList());
                response.setAllowanceDetails(allowances);
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse allowance details", e);
            }
        }

        // Parse deduction details từ JSON
        if (item.getDeductionDetails() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<PayrollItemResponse.DeductionDetailResponse> deductionDetails = 
                    objectMapper.readValue(item.getDeductionDetails(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, 
                            PayrollItemResponse.DeductionDetailResponse.class));
                
                List<PayrollRecordResponse.DeductionItemResponse> deductions = 
                    deductionDetails.stream()
                        .map(d -> {
                            PayrollRecordResponse.DeductionItemResponse deductionItem = 
                                new PayrollRecordResponse.DeductionItemResponse();
                            deductionItem.setCode(d.getCode());
                            deductionItem.setName(d.getName());
                            deductionItem.setAmount(d.getCalculatedAmount() != null ? d.getCalculatedAmount() : d.getAmount());
                            return deductionItem;
                        })
                        .collect(Collectors.toList());
                response.setDeductionDetails(deductions);
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse deduction details", e);
            }
        }

        return response;
    }

    // ==================== Profile Operations ====================

    @Override
    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long employeeId) {
        // Lấy thông tin user với profile (profile được fetch EAGER)
        UserEntity user = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));

        // Lấy tên manager từ department (nếu có)
        String managerName = getManagerName(user);

        // Map sang response
        return myPortalMapper.toMyProfileResponse(user, managerName);
    }

    /**
     * Lấy tên manager của nhân viên từ department
     */
    private String getManagerName(UserEntity user) {
        if (user.getProfile() == null) {
            return null;
        }

        var department = user.getProfile().getDepartmentEntity();
        if (department == null || department.getManager() == null) {
            return null;
        }

        var managerProfile = department.getManager().getProfile();
        if (managerProfile == null) {
            return null;
        }

        return managerProfile.getName();
    }

    @Override
    @Transactional
    public MyProfileResponse updateMyProfile(Long employeeId, UpdateMyProfileRequest request) {
        // Lấy thông tin user với profile
        UserEntity user = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));

        // Lấy hoặc tạo profile nếu chưa có
        UserProfileEntity profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfileEntity();
            profile.setUser(user);
            profile.setDeleted(false);
            user.setProfile(profile);
        }

        // Cập nhật chỉ các trường được phép chỉnh sửa (không update readonly fields)
        myPortalMapper.updateUserProfile(profile, request);

        // Tính lại phần trăm hoàn thiện profile
        user.calculateProfileCompleteness();

        // Lưu user (cascade sẽ lưu profile)
        userRepository.save(user);

        // Lấy tên manager để trả về response
        String managerName = getManagerName(user);

        // Trả về response với thông tin đã cập nhật
        return myPortalMapper.toMyProfileResponse(user, managerName);
    }

    @Override
    @Transactional
    public String uploadAvatar(Long employeeId, MultipartFile file) {
        // Validate loại file (chỉ chấp nhận JPEG, PNG, WebP)
        validateFileType(file, AVATAR_TYPES);

        // Lấy thông tin user với profile
        UserEntity user = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));

        // Lấy hoặc tạo profile nếu chưa có
        UserProfileEntity profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfileEntity();
            profile.setUser(user);
            profile.setDeleted(false);
            user.setProfile(profile);
        }

        // Xóa avatar cũ nếu có
        String oldAvatarUrl = profile.getAvatar();
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
            uploadService.deleteFile(oldAvatarUrl);
        }

        // Upload file mới
        String newAvatarUrl = uploadService.uploadFile(file, "avatar", user.getEmployeeCode());

        // Cập nhật profile với URL avatar mới
        profile.setAvatar(newAvatarUrl);

        // Lưu user (cascade sẽ lưu profile)
        userRepository.save(user);

        return newAvatarUrl;
    }

    /**
     * Validate loại file upload
     * @param file File cần validate
     * @param allowedTypes Các loại MIME type được phép
     * @throws BadRequestException nếu loại file không hợp lệ
     */
    private void validateFileType(MultipartFile file, Set<String> allowedTypes) {
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BadRequestException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    // ==================== Contract Operations ====================

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getCurrentContract(Long employeeId) {
        // Tìm contract đang active của nhân viên
        // Contract active: status = ACTIVE và ngày hiện tại nằm trong khoảng startDate - endDate
        LocalDate currentDate = LocalDate.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));
        
        return employmentContractRepository.findActiveByEmployeeId(employeeId, currentDate)
                .map(myPortalMapper::toContractResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getContractHistory(Long employeeId) {
        // Lấy tất cả contracts của nhân viên, đã sort theo startDate DESC (newest first)
        return employmentContractRepository.findByEmployeeIdAndDeletedFalseOrderByStartDateDesc(employeeId)
                .stream()
                .map(myPortalMapper::toContractResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContractDetail(Long employeeId, Long contractId) {
        // Tìm contract theo ID
        var contract = employmentContractRepository.findByIdAndDeletedFalse(contractId)
                .orElseThrow(() -> NotFoundException.contract(contractId));

        // Kiểm tra quyền sở hữu - trả về 404 nếu contract không thuộc về nhân viên này
        if (!contract.getEmployeeId().equals(employeeId)) {
            throw NotFoundException.contract(contractId);
        }

        // Map sang response
        return myPortalMapper.toContractResponse(contract);
    }

    // ==================== Document Operations ====================

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getMyDocuments(Long employeeId, Pageable pageable) {
        // Query documents của nhân viên với pagination
        Page<EmployeeDocumentEntity> documentsPage = employeeDocumentRepository.findByEmployeeId(employeeId, pageable);
        
        // Map sang Page<DocumentResponse> sử dụng Page.map()
        return documentsPage.map(myPortalMapper::toDocumentResponse);
    }

    @Override
    @Transactional
    public DocumentResponse uploadDocument(Long employeeId, MultipartFile file, String documentType) {
        // Validate loại file (chỉ chấp nhận JPEG, PNG, WebP, PDF)
        validateFileType(file, DOCUMENT_TYPES);

        // Validate documentType là giá trị hợp lệ của enum DocumentType
        validateDocumentType(documentType);

        // Lấy thông tin user để lấy employeeCode cho đường dẫn file
        UserEntity user = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> NotFoundException.user(employeeId));

        // Upload file sử dụng upload service
        String fileUrl = uploadService.uploadFile(file, "documents", user.getEmployeeCode());

        // Tạo entity từ thông tin upload
        EmployeeDocumentEntity entity = myPortalMapper.toDocumentEntity(employeeId, file, documentType, fileUrl);

        // Lưu entity vào database
        EmployeeDocumentEntity savedEntity = employeeDocumentRepository.save(entity);

        // Trả về response
        return myPortalMapper.toDocumentResponse(savedEntity);
    }

    /**
     * Validate documentType là giá trị hợp lệ của enum DocumentType
     * @param documentType Loại tài liệu cần validate
     * @throws BadRequestException nếu documentType không hợp lệ
     */
    private void validateDocumentType(String documentType) {
        if (documentType == null || documentType.isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_DOCUMENT_TYPE);
        }
        try {
            DocumentType.valueOf(documentType);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.INVALID_DOCUMENT_TYPE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentDetail(Long employeeId, Long documentId) {
        // Tìm document theo ID
        EmployeeDocumentEntity document = employeeDocumentRepository.findById(documentId)
                .orElseThrow(() -> NotFoundException.document(documentId));

        // Kiểm tra quyền sở hữu - trả về 404 nếu document không thuộc về nhân viên này
        if (!document.getEmployeeId().equals(employeeId)) {
            throw NotFoundException.document(documentId);
        }

        // Map sang response
        return myPortalMapper.toDocumentResponse(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long employeeId, Long documentId) {
        // Tìm document theo ID
        EmployeeDocumentEntity document = employeeDocumentRepository.findById(documentId)
                .orElseThrow(() -> NotFoundException.document(documentId));

        // Kiểm tra quyền sở hữu - trả về 404 nếu document không thuộc về nhân viên này
        if (!document.getEmployeeId().equals(employeeId)) {
            throw NotFoundException.document(documentId);
        }

        // Xóa file vật lý từ storage
        if (document.getFileUrl() != null) {
            uploadService.deleteFile(document.getFileUrl());
        }

        // Xóa record trong database (hard delete - EmployeeDocument không có soft delete)
        employeeDocumentRepository.delete(document);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadDocument(Long employeeId, Long documentId) {
        // Tìm document theo ID
        EmployeeDocumentEntity document = employeeDocumentRepository.findById(documentId)
                .orElseThrow(() -> NotFoundException.document(documentId));

        // Kiểm tra quyền sở hữu - trả về 404 nếu document không thuộc về nhân viên này
        if (!document.getEmployeeId().equals(employeeId)) {
            throw NotFoundException.document(documentId);
        }

        // Đọc file từ storage
        return readFileFromStorage(document.getFileUrl(), documentId);
    }

    /**
     * Đọc nội dung file từ storage dựa trên đường dẫn tương đối
     * @param fileUrl Đường dẫn tương đối của file (vd: /uploads/tenant/documents/file.pdf)
     * @param documentId ID của document để log và throw exception
     * @return byte array của nội dung file
     * @throws NotFoundException nếu file không tồn tại
     */
    private byte[] readFileFromStorage(String fileUrl, Long documentId) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.warn("File URL rỗng cho document: {}", documentId);
            throw NotFoundException.document(documentId);
        }

        try {
            // Chuyển đường dẫn tương đối thành đường dẫn tuyệt đối
            // fileUrl format: /uploads/{tenant-domain}/{folder}/{filename}
            // uploadPath default: uploads
            String absolutePath = fileUrl.replace("/uploads/", "uploads/");
            Path path = Paths.get(absolutePath);

            if (!Files.exists(path)) {
                log.warn("File không tồn tại: {} cho document: {}", fileUrl, documentId);
                throw NotFoundException.document(documentId);
            }

            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("Lỗi đọc file: {} cho document: {}", e.getMessage(), documentId, e);
            throw NotFoundException.document(documentId);
        }
    }
}
