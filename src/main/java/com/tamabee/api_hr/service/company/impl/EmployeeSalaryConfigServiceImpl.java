package com.tamabee.api_hr.service.company.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.request.payroll.SalaryConfigRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeSalaryConfigResponse;
import com.tamabee.api_hr.dto.response.payroll.SalaryConfigValidationResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeSalaryEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.SalaryType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.EmployeeSalaryMapper;
import com.tamabee.api_hr.repository.payroll.EmployeeSalaryRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IEmployeeSalaryConfigService;

import lombok.RequiredArgsConstructor;

/**
 * Service implementation cho quản lý cấu hình lương nhân viên
 */
@Service
@RequiredArgsConstructor
public class EmployeeSalaryConfigServiceImpl implements IEmployeeSalaryConfigService {

    private final EmployeeSalaryRepository salaryRepository;
    private final UserRepository userRepository;
    private final EmployeeSalaryMapper salaryMapper;

    @Override
    @Transactional
    public EmployeeSalaryConfigResponse createSalaryConfig(Long employeeId, SalaryConfigRequest request) {
        // Validate employee exists
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Validate salary amount based on salary type
        validateSalaryAmount(request);

        // Tạo entity mới (không xóa config cũ, để người dùng tự quản lý)
        EmployeeSalaryEntity entity = salaryMapper.toEntity(request, employeeId);

        // Lưu vào database
        entity = salaryRepository.save(entity);

        return salaryMapper.toResponse(entity, employee);
    }

    @Override
    @Transactional
    public EmployeeSalaryConfigResponse updateSalaryConfig(Long configId, SalaryConfigRequest request) {
        // Tìm config hiện tại
        EmployeeSalaryEntity currentConfig = salaryRepository.findById(configId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALARY_CONFIG_NOT_FOUND));

        // Kiểm tra quyền sửa (chỉ cho phép nếu chưa được sử dụng để tính lương)
        if (Boolean.TRUE.equals(currentConfig.getUsedInPayroll())) {
            throw new BadRequestException("Không thể sửa cấu hình lương đã được sử dụng để tính lương",
                    ErrorCode.SALARY_CONFIG_CANNOT_MODIFY);
        }

        // Validate employee exists
        UserEntity employee = userRepository.findById(currentConfig.getEmployeeId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Validate salary amount based on salary type
        validateSalaryAmount(request);

        // Cập nhật config hiện tại (không tạo mới)
        currentConfig.setSalaryType(request.getSalaryType());
        currentConfig.setMonthlySalary(request.getMonthlySalary());
        currentConfig.setDailyRate(request.getDailyRate());
        currentConfig.setHourlyRate(request.getHourlyRate());
        currentConfig.setShiftRate(request.getShiftRate());
        currentConfig.setEffectiveFrom(request.getEffectiveFrom());
        currentConfig.setEffectiveTo(request.getEffectiveTo());
        currentConfig.setNote(request.getNote());

        currentConfig = salaryRepository.save(currentConfig);

        return salaryMapper.toResponse(currentConfig, employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeSalaryConfigResponse getCurrentSalaryConfig(Long employeeId) {
        // Validate employee exists
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Tìm config đang active
        EmployeeSalaryEntity currentConfig = salaryRepository.findAll().stream()
                .filter(config -> !config.getDeleted() 
                        && config.getEmployeeId().equals(employeeId) 
                        && Boolean.TRUE.equals(config.getActive()))
                .findFirst()
                .orElse(null);

        if (currentConfig == null) {
            return null;
        }

        return salaryMapper.toResponse(currentConfig, employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSalaryConfigResponse> getSalaryConfigHistory(Long employeeId) {
        // Validate employee exists
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Lấy tất cả config của employee, sắp xếp theo effectiveFrom giảm dần, sau đó createdAt giảm dần
        List<EmployeeSalaryEntity> configs = salaryRepository.findAll().stream()
                .filter(config -> !config.getDeleted() && config.getEmployeeId().equals(employeeId))
                .sorted((a, b) -> {
                    int cmp = b.getEffectiveFrom().compareTo(a.getEffectiveFrom());
                    if (cmp != 0) return cmp;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        return configs.stream()
                .map(config -> salaryMapper.toResponse(config, employee))
                .collect(Collectors.toList());
    }

    /**
     * Validate rằng mức lương phù hợp với loại lương và ngày hiệu lực hợp lệ
     */
    private void validateSalaryAmount(SalaryConfigRequest request) {
        SalaryType salaryType = request.getSalaryType();
        BigDecimal monthlySalary = request.getMonthlySalary();
        BigDecimal dailyRate = request.getDailyRate();
        BigDecimal hourlyRate = request.getHourlyRate();
        BigDecimal shiftRate = request.getShiftRate();

        switch (salaryType) {
            case MONTHLY:
                if (monthlySalary == null || monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Lương tháng phải được nhập cho loại lương MONTHLY",
                            ErrorCode.SALARY_AMOUNT_REQUIRED);
                }
                break;
            case DAILY:
                if (dailyRate == null || dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Lương ngày phải được nhập cho loại lương DAILY",
                            ErrorCode.SALARY_AMOUNT_REQUIRED);
                }
                break;
            case HOURLY:
                if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Lương giờ phải được nhập cho loại lương HOURLY",
                            ErrorCode.SALARY_AMOUNT_REQUIRED);
                }
                break;
            case SHIFT_BASED:
                if (shiftRate == null || shiftRate.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Lương theo ca phải được nhập cho loại lương SHIFT_BASED",
                            ErrorCode.SALARY_AMOUNT_REQUIRED);
                }
                break;
            default:
                throw new BadRequestException(ErrorCode.INVALID_SALARY_TYPE);
        }

        // Validate effectiveTo > effectiveFrom
        LocalDate effectiveFrom = request.getEffectiveFrom();
        LocalDate effectiveTo = request.getEffectiveTo();
        if (effectiveFrom != null && effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu",
                    ErrorCode.INVALID_DATE_RANGE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SalaryConfigValidationResponse validateSalaryConfig(Long employeeId, SalaryConfigRequest request) {
        // Validate employee exists
        userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Validate salary amount
        try {
            validateSalaryAmount(request);
        } catch (BadRequestException e) {
            return SalaryConfigValidationResponse.builder()
                    .isValid(false)
                    .affectsCurrentPayroll(false)
                    .hasOverlappingConfigs(false)
                    .overlappingConfigsCount(0)
                    .message(e.getMessage())
                    .build();
        }

        LocalDate effectiveFrom = request.getEffectiveFrom();
        YearMonth currentPeriod = YearMonth.now();
        LocalDate periodEnd = currentPeriod.atEndOfMonth();

        // Kiểm tra có config cũ bị trùng không
        List<EmployeeSalaryEntity> overlappingConfigs = salaryRepository.findEffectiveSalaries(employeeId,
                effectiveFrom);
        int overlappingCount = overlappingConfigs.size();
        boolean hasOverlapping = overlappingCount > 0;

        // Kiểm tra có ảnh hưởng kỳ lương hiện tại không
        boolean affectsCurrentPayroll = !effectiveFrom.isAfter(periodEnd);

        String periodString = currentPeriod.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        return SalaryConfigValidationResponse.builder()
                .isValid(true)
                .affectsCurrentPayroll(affectsCurrentPayroll)
                .currentPayrollPeriod(periodString)
                .hasOverlappingConfigs(hasOverlapping)
                .overlappingConfigsCount(overlappingCount)
                .message(null)
                .build();
    }

    @Override
    @Transactional
    public void deleteSalaryConfig(Long configId) {
        EmployeeSalaryEntity config = salaryRepository.findById(configId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALARY_CONFIG_NOT_FOUND));

        // Kiểm tra quyền xóa (chỉ cho phép nếu chưa được sử dụng để tính lương)
        if (Boolean.TRUE.equals(config.getUsedInPayroll())) {
            throw new BadRequestException("Không thể xóa cấu hình lương đã được sử dụng để tính lương",
                    ErrorCode.SALARY_CONFIG_CANNOT_MODIFY);
        }

        salaryRepository.delete(config);
    }

    @Override
    @Transactional
    public EmployeeSalaryConfigResponse applySalaryConfig(Long configId) {
        EmployeeSalaryEntity config = salaryRepository.findById(configId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALARY_CONFIG_NOT_FOUND));

        // Kiểm tra quyền sửa (chỉ cho phép nếu chưa được sử dụng để tính lương)
        if (Boolean.TRUE.equals(config.getUsedInPayroll())) {
            throw new BadRequestException("Không thể áp dụng cấu hình lương đã được sử dụng để tính lương",
                    ErrorCode.SALARY_CONFIG_CANNOT_MODIFY);
        }

        // Kiểm tra config có quá hạn không (effectiveTo < today)
        LocalDate today = LocalDate.now();
        if (config.getEffectiveTo() != null && config.getEffectiveTo().isBefore(today)) {
            throw new BadRequestException("Không thể áp dụng cấu hình lương đã hết hiệu lực",
                    ErrorCode.SALARY_CONFIG_EXPIRED);
        }

        // Validate employee exists
        UserEntity employee = userRepository.findById(config.getEmployeeId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Deactivate tất cả config khác của employee
        Long employeeIdToDeactivate = config.getEmployeeId();
        salaryRepository.findAll().stream()
                .filter(c -> !c.getDeleted() 
                        && c.getEmployeeId().equals(employeeIdToDeactivate) 
                        && Boolean.TRUE.equals(c.getActive())
                        && !c.getId().equals(configId))
                .forEach(c -> {
                    c.setActive(false);
                    salaryRepository.save(c);
                });

        // Activate config này
        config.setActive(true);
        EmployeeSalaryEntity savedConfig = salaryRepository.save(config);

        return salaryMapper.toResponse(savedConfig, employee);
    }
}
