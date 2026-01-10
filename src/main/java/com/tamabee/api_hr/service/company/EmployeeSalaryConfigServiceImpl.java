package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.SalaryConfigRequest;
import com.tamabee.api_hr.dto.response.EmployeeSalaryConfigResponse;
import com.tamabee.api_hr.dto.response.SalaryConfigValidationResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeSalaryEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.SalaryType;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.EmployeeSalaryMapper;
import com.tamabee.api_hr.repository.EmployeeSalaryRepository;
import com.tamabee.api_hr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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

        // Xóa/vô hiệu hóa các config cũ bị trùng thời gian
        deactivateOverlappingConfigs(employeeId, request.getEffectiveFrom());

        // Tạo entity mới
        EmployeeSalaryEntity entity = salaryMapper.toEntity(request, employeeId);

        // Lưu vào database
        entity = salaryRepository.save(entity);

        return salaryMapper.toResponse(entity, employee);
    }

    private void deactivateOverlappingConfigs(Long employeeId, LocalDate newEffectiveFrom) {
        List<EmployeeSalaryEntity> existingConfigs = salaryRepository.findEffectiveSalaries(employeeId,
                newEffectiveFrom);
        salaryRepository.deleteAll(existingConfigs);
    }

    @Override
    @Transactional
    public EmployeeSalaryConfigResponse updateSalaryConfig(Long configId, SalaryConfigRequest request) {
        // Tìm config hiện tại
        EmployeeSalaryEntity currentConfig = salaryRepository.findById(configId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALARY_CONFIG_NOT_FOUND));

        // Validate employee exists
        UserEntity employee = userRepository.findById(currentConfig.getEmployeeId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Validate salary amount based on salary type
        validateSalaryAmount(request);

        // Đóng config cũ (set effectiveTo = ngày trước ngày bắt đầu của config mới)
        LocalDate newEffectiveFrom = request.getEffectiveFrom();
        currentConfig.setEffectiveTo(newEffectiveFrom.minusDays(1));
        salaryRepository.save(currentConfig);

        // Tạo config mới
        EmployeeSalaryEntity newConfig = salaryMapper.toEntity(
                request,
                currentConfig.getEmployeeId());

        newConfig = salaryRepository.save(newConfig);

        return salaryMapper.toResponse(newConfig, employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeSalaryConfigResponse getCurrentSalaryConfig(Long employeeId) {
        // Validate employee exists
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Tìm config hiện tại (effectiveFrom <= today AND (effectiveTo IS NULL OR
        // effectiveTo >= today))
        LocalDate today = LocalDate.now();
        EmployeeSalaryEntity currentConfig = salaryRepository.findEffectiveSalary(employeeId, today)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SALARY_CONFIG_NOT_FOUND));

        return salaryMapper.toResponse(currentConfig, employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSalaryConfigResponse> getSalaryConfigHistory(Long employeeId) {
        // Validate employee exists
        UserEntity employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        // Lấy tất cả config của employee, sắp xếp theo effectiveFrom giảm dần
        List<EmployeeSalaryEntity> configs = salaryRepository.findAll().stream()
                .filter(config -> !config.getDeleted() && config.getEmployeeId().equals(employeeId))
                .sorted((a, b) -> b.getEffectiveFrom().compareTo(a.getEffectiveFrom()))
                .collect(Collectors.toList());

        return configs.stream()
                .map(config -> salaryMapper.toResponse(config, employee))
                .collect(Collectors.toList());
    }

    /**
     * Validate rằng mức lương phù hợp với loại lương
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
        salaryRepository.delete(config);
    }
}
