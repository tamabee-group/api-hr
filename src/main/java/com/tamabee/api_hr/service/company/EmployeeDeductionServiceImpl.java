package com.tamabee.api_hr.service.company;

import com.tamabee.api_hr.dto.request.DeductionAssignmentRequest;
import com.tamabee.api_hr.dto.response.EmployeeDeductionResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeDeductionEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.DeductionType;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.BadRequestException;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.EmployeeDeductionMapper;
import com.tamabee.api_hr.repository.EmployeeDeductionRepository;
import com.tamabee.api_hr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation của service quản lý khấu trừ cá nhân của nhân viên
 */
@Service
@RequiredArgsConstructor
public class EmployeeDeductionServiceImpl implements IEmployeeDeductionService {

    private final EmployeeDeductionRepository deductionRepository;
    private final UserRepository userRepository;
    private final EmployeeDeductionMapper deductionMapper;

    @Override
    @Transactional
    public EmployeeDeductionResponse assignDeduction(Long employeeId, DeductionAssignmentRequest request) {
        // Kiểm tra nhân viên có tồn tại không
        UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên", ErrorCode.USER_NOT_FOUND));

        // Validate: phải có amount hoặc percentage tùy theo type
        validateDeductionRequest(request);

        // Tạo entity từ request
        EmployeeDeductionEntity entity = deductionMapper.toEntity(request, employeeId, employee.getCompanyId());

        // Lưu vào database
        entity = deductionRepository.save(entity);

        // Trả về response
        return deductionMapper.toResponse(entity, employee);
    }

    @Override
    @Transactional
    public EmployeeDeductionResponse updateDeduction(Long assignmentId, DeductionAssignmentRequest request) {
        // Tìm khấu trừ cần cập nhật
        EmployeeDeductionEntity entity = deductionRepository.findByIdAndDeletedFalse(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khấu trừ", ErrorCode.DEDUCTION_NOT_FOUND));

        // Validate: phải có amount hoặc percentage tùy theo type
        validateDeductionRequest(request);

        // Lấy thông tin nhân viên
        UserEntity employee = userRepository.findByIdAndDeletedFalse(entity.getEmployeeId())
                .orElse(null);

        // Cập nhật entity từ request
        deductionMapper.updateEntity(entity, request);

        // Lưu vào database
        entity = deductionRepository.save(entity);

        // Trả về response
        return deductionMapper.toResponse(entity, employee);
    }

    @Override
    @Transactional
    public void deactivateDeduction(Long assignmentId) {
        // Tìm khấu trừ cần vô hiệu hóa
        EmployeeDeductionEntity entity = deductionRepository.findByIdAndDeletedFalse(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khấu trừ", ErrorCode.DEDUCTION_NOT_FOUND));

        // Soft deactivation: set isActive = false và effectiveTo = hôm nay
        entity.setIsActive(false);
        if (entity.getEffectiveTo() == null || entity.getEffectiveTo().isAfter(LocalDate.now())) {
            entity.setEffectiveTo(LocalDate.now());
        }

        // Lưu vào database
        deductionRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDeductionResponse> getEmployeeDeductions(Long employeeId, boolean includeInactive) {
        // Kiểm tra nhân viên có tồn tại không
        UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên", ErrorCode.USER_NOT_FOUND));

        // Lấy danh sách khấu trừ
        List<EmployeeDeductionEntity> entities;
        if (includeInactive) {
            entities = deductionRepository.findByEmployeeIdAndDeletedFalse(employeeId);
        } else {
            entities = deductionRepository.findActiveByEmployeeId(employeeId, LocalDate.now());
        }

        // Chuyển đổi sang response
        return entities.stream()
                .map(entity -> deductionMapper.toResponse(entity, employee))
                .collect(Collectors.toList());
    }

    /**
     * Validate deduction request
     * - FIXED type: phải có amount
     * - PERCENTAGE type: phải có percentage
     */
    private void validateDeductionRequest(DeductionAssignmentRequest request) {
        if (request.getDeductionType() == DeductionType.FIXED) {
            if (request.getAmount() == null || request.getAmount().signum() <= 0) {
                throw new BadRequestException(
                        "Khấu trừ cố định phải có số tiền",
                        ErrorCode.DEDUCTION_AMOUNT_OR_PERCENTAGE_REQUIRED);
            }
        } else if (request.getDeductionType() == DeductionType.PERCENTAGE) {
            if (request.getPercentage() == null || request.getPercentage().signum() <= 0) {
                throw new BadRequestException(
                        "Khấu trừ theo phần trăm phải có tỷ lệ",
                        ErrorCode.DEDUCTION_AMOUNT_OR_PERCENTAGE_REQUIRED);
            }
        }
    }
}
