package com.tamabee.api_hr.service.company.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.request.payroll.AssignSalaryItemRequest;
import com.tamabee.api_hr.dto.request.payroll.UpdateSalaryItemRequest;
import com.tamabee.api_hr.dto.response.payroll.EmployeeSalaryItemResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeSalaryItemEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.SalaryItemType;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.EmployeeSalaryItemMapper;
import com.tamabee.api_hr.repository.payroll.EmployeeSalaryItemRepository;
import com.tamabee.api_hr.repository.payroll.SalaryItemTemplateRepository;
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.service.company.interfaces.IEmployeeSalaryItemService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation quản lý phụ cấp/khấu trừ của nhân viên
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeSalaryItemServiceImpl implements IEmployeeSalaryItemService {

    private final EmployeeSalaryItemRepository salaryItemRepository;
    private final SalaryItemTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final EmployeeSalaryItemMapper salaryItemMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSalaryItemResponse> getEmployeeSalaryItems(Long employeeId) {
        validateEmployeeExists(employeeId);
        List<EmployeeSalaryItemEntity> items = salaryItemRepository.findByEmployeeIdAndDeletedFalse(employeeId);
        return salaryItemMapper.toResponseList(items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSalaryItemResponse> getEmployeeAllowances(Long employeeId) {
        validateEmployeeExists(employeeId);
        List<EmployeeSalaryItemEntity> items = salaryItemRepository
                .findByEmployeeIdAndTemplateTypeAndDeletedFalse(employeeId, SalaryItemType.ALLOWANCE);
        return salaryItemMapper.toResponseList(items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSalaryItemResponse> getEmployeeDeductions(Long employeeId) {
        validateEmployeeExists(employeeId);
        List<EmployeeSalaryItemEntity> items = salaryItemRepository
                .findByEmployeeIdAndTemplateTypeAndDeletedFalse(employeeId, SalaryItemType.DEDUCTION);
        return salaryItemMapper.toResponseList(items);
    }

    @Override
    @Transactional
    public EmployeeSalaryItemResponse assignSalaryItem(Long employeeId, AssignSalaryItemRequest request) {
        validateEmployeeExists(employeeId);
        validateTemplateExists(request.getTemplateId());

        EmployeeSalaryItemEntity entity = salaryItemMapper.toEntity(request, employeeId);
        entity = salaryItemRepository.save(entity);

        // Reload để lấy template info
        entity = salaryItemRepository.findByIdAndDeletedFalse(entity.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy salary item",
                        ErrorCode.SALARY_ITEM_NOT_FOUND));

        log.info("Đã gán salary item {} cho nhân viên {}", entity.getId(), employeeId);
        return salaryItemMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public EmployeeSalaryItemResponse updateSalaryItem(Long itemId, UpdateSalaryItemRequest request) {
        EmployeeSalaryItemEntity entity = findSalaryItem(itemId);
        validateTemplateExists(request.getTemplateId());

        entity.setTemplateId(request.getTemplateId());
        entity.setAmount(request.getAmount());
        entity = salaryItemRepository.save(entity);

        // Reload để lấy template info mới
        entity = salaryItemRepository.findByIdAndDeletedFalse(entity.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy salary item",
                        ErrorCode.SALARY_ITEM_NOT_FOUND));

        log.info("Đã cập nhật salary item {}", itemId);
        return salaryItemMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteSalaryItem(Long itemId) {
        EmployeeSalaryItemEntity entity = findSalaryItem(itemId);
        entity.setDeleted(true);
        salaryItemRepository.save(entity);

        log.info("Đã xóa salary item {}", itemId);
    }

    /**
     * Kiểm tra nhân viên tồn tại
     */
    private void validateEmployeeExists(Long employeeId) {
        if (!userRepository.existsByIdAndDeletedFalse(employeeId)) {
            throw new NotFoundException(
                    "Không tìm thấy nhân viên",
                    ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * Kiểm tra template tồn tại
     */
    private void validateTemplateExists(Long templateId) {
        if (!templateRepository.existsByIdAndDeletedFalse(templateId)) {
            throw new NotFoundException(
                    "Không tìm thấy template",
                    ErrorCode.TEMPLATE_NOT_FOUND);
        }
    }

    /**
     * Tìm salary item theo ID
     */
    private EmployeeSalaryItemEntity findSalaryItem(Long itemId) {
        return salaryItemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy phụ cấp/khấu trừ",
                        ErrorCode.SALARY_ITEM_NOT_FOUND));
    }
}
