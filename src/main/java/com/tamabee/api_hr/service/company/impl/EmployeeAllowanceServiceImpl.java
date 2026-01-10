package com.tamabee.api_hr.service.company.impl;

import com.tamabee.api_hr.dto.request.payroll.AllowanceAssignmentRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeAllowanceResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeAllowanceEntity;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.EmployeeAllowanceMapper;
import com.tamabee.api_hr.repository.payroll.EmployeeAllowanceRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IEmployeeAllowanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation của service quản lý phụ cấp cá nhân của nhân viên
 */
@Service
@RequiredArgsConstructor
public class EmployeeAllowanceServiceImpl implements IEmployeeAllowanceService {

    private final EmployeeAllowanceRepository allowanceRepository;
    private final UserRepository userRepository;
    private final EmployeeAllowanceMapper allowanceMapper;

    @Override
    @Transactional
    public EmployeeAllowanceResponse assignAllowance(Long employeeId, AllowanceAssignmentRequest request) {
        // Kiểm tra nhân viên có tồn tại không
        UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên", ErrorCode.USER_NOT_FOUND));

        // Tạo entity từ request
        EmployeeAllowanceEntity entity = allowanceMapper.toEntity(request, employeeId);

        // Lưu vào database
        entity = allowanceRepository.save(entity);

        // Trả về response
        return allowanceMapper.toResponse(entity, employee);
    }

    @Override
    @Transactional
    public EmployeeAllowanceResponse updateAllowance(Long assignmentId, AllowanceAssignmentRequest request) {
        // Tìm phụ cấp cần cập nhật
        EmployeeAllowanceEntity entity = allowanceRepository.findByIdAndDeletedFalse(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phụ cấp", ErrorCode.ALLOWANCE_NOT_FOUND));

        // Lấy thông tin nhân viên
        UserEntity employee = userRepository.findByIdAndDeletedFalse(entity.getEmployeeId())
                .orElse(null);

        // Cập nhật entity từ request
        allowanceMapper.updateEntity(entity, request);

        // Lưu vào database
        entity = allowanceRepository.save(entity);

        // Trả về response
        return allowanceMapper.toResponse(entity, employee);
    }

    @Override
    @Transactional
    public void deactivateAllowance(Long assignmentId) {
        // Tìm phụ cấp cần vô hiệu hóa
        EmployeeAllowanceEntity entity = allowanceRepository.findByIdAndDeletedFalse(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phụ cấp", ErrorCode.ALLOWANCE_NOT_FOUND));

        // Soft deactivation: set isActive = false và effectiveTo = hôm nay
        entity.setIsActive(false);
        if (entity.getEffectiveTo() == null || entity.getEffectiveTo().isAfter(LocalDate.now())) {
            entity.setEffectiveTo(LocalDate.now());
        }

        // Lưu vào database
        allowanceRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeAllowanceResponse> getEmployeeAllowances(Long employeeId, boolean includeInactive) {
        // Kiểm tra nhân viên có tồn tại không
        UserEntity employee = userRepository.findByIdAndDeletedFalse(employeeId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên", ErrorCode.USER_NOT_FOUND));

        // Lấy danh sách phụ cấp
        List<EmployeeAllowanceEntity> entities;
        if (includeInactive) {
            entities = allowanceRepository.findByEmployeeIdAndDeletedFalse(employeeId);
        } else {
            entities = allowanceRepository.findActiveByEmployeeId(employeeId, LocalDate.now());
        }

        // Chuyển đổi sang response
        return entities.stream()
                .map(entity -> allowanceMapper.toResponse(entity, employee))
                .collect(Collectors.toList());
    }
}
