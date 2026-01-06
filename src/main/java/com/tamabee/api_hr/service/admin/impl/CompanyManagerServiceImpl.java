package com.tamabee.api_hr.service.admin.impl;

import com.tamabee.api_hr.datasource.TenantProvisioningService;
import com.tamabee.api_hr.dto.request.UpdateCompanyRequest;
import com.tamabee.api_hr.dto.response.CompanyResponse;
import com.tamabee.api_hr.entity.company.CompanyEntity;
import com.tamabee.api_hr.enums.CompanyStatus;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.core.CompanyMapper;
import com.tamabee.api_hr.repository.CompanyRepository;
import com.tamabee.api_hr.service.admin.ICompanyManagerService;
import com.tamabee.api_hr.service.core.IUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service quản lý công ty cho admin Tamabee
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyManagerServiceImpl implements ICompanyManagerService {

    private static final String TAMABEE_TENANT = "tamabee";

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final IUploadService uploadService;
    private final TenantProvisioningService tenantProvisioningService;

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyResponse> getAllCompanies(Pageable pageable) {
        Page<CompanyEntity> companies = companyRepository.findAll(pageable);
        return companies.map(companyMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> NotFoundException.company(id));
        return companyMapper.toResponse(company);
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(Long id, UpdateCompanyRequest request) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> NotFoundException.company(id));

        // Cập nhật các trường nếu có giá trị
        if (request.getName() != null) {
            company.setName(request.getName());
        }
        if (request.getOwnerName() != null) {
            company.setOwnerName(request.getOwnerName());
        }
        if (request.getEmail() != null) {
            company.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            company.setPhone(request.getPhone());
        }
        if (request.getIndustry() != null) {
            company.setIndustry(request.getIndustry());
        }
        if (request.getLocale() != null) {
            company.setLocale(request.getLocale());
        }
        if (request.getLanguage() != null) {
            company.setLanguage(request.getLanguage());
        }
        if (request.getZipcode() != null) {
            company.setZipcode(request.getZipcode());
        }
        if (request.getAddress() != null) {
            company.setAddress(request.getAddress());
        }

        CompanyEntity savedCompany = companyRepository.save(company);
        return companyMapper.toResponse(savedCompany);
    }

    @Override
    @Transactional
    public String uploadLogo(Long id, MultipartFile file) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> NotFoundException.company(id));

        // Xóa logo cũ nếu có
        if (company.getLogo() != null) {
            uploadService.deleteFile(company.getLogo());
        }

        // Upload file mới
        String logoUrl = uploadService.uploadFile(file, "logo", "company-" + id);

        // Cập nhật vào database
        company.setLogo(logoUrl);
        companyRepository.save(company);

        return logoUrl;
    }

    @Override
    @Transactional
    public CompanyResponse deactivateCompany(Long id) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> NotFoundException.company(id));

        // Không cho phép deactivate Tamabee company
        if (TAMABEE_TENANT.equals(company.getTenantDomain())) {
            throw BadRequestException.custom("CANNOT_DEACTIVATE_TAMABEE",
                    "Không thể deactivate Tamabee company");
        }

        // Kiểm tra company đã inactive chưa
        if (company.getStatus() == CompanyStatus.INACTIVE) {
            throw BadRequestException.custom("COMPANY_ALREADY_INACTIVE",
                    "Company đã ở trạng thái INACTIVE");
        }

        // Set status = INACTIVE và ghi nhận thời điểm deactivate
        company.setStatus(CompanyStatus.INACTIVE);
        company.setDeactivatedAt(java.time.LocalDateTime.now());
        CompanyEntity savedCompany = companyRepository.save(company);

        // Remove DataSource khỏi pool (database vẫn giữ lại cho compliance)
        tenantProvisioningService.deprovisionTenant(company.getTenantDomain());
        log.info("Deactivated company: {} (tenantDomain: {})", id, company.getTenantDomain());

        return companyMapper.toResponse(savedCompany);
    }

    @Override
    @Transactional
    public CompanyResponse reactivateCompany(Long id) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> NotFoundException.company(id));

        // Kiểm tra company có đang inactive không
        if (company.getStatus() != CompanyStatus.INACTIVE) {
            throw BadRequestException.custom("COMPANY_NOT_INACTIVE",
                    "Company không ở trạng thái INACTIVE");
        }

        // Restore DataSource vào pool
        try {
            tenantProvisioningService.provisionTenant(company.getTenantDomain());
        } catch (TenantProvisioningService.TenantProvisioningException e) {
            log.error("Failed to restore DataSource for company: {}", id, e);
            throw BadRequestException.custom("REACTIVATION_FAILED",
                    "Không thể khôi phục DataSource cho company: " + e.getMessage());
        }

        // Set status = ACTIVE và xóa thời điểm deactivate
        company.setStatus(CompanyStatus.ACTIVE);
        company.setDeactivatedAt(null);
        CompanyEntity savedCompany = companyRepository.save(company);
        log.info("Reactivated company: {} (tenantDomain: {})", id, company.getTenantDomain());

        return companyMapper.toResponse(savedCompany);
    }
}
