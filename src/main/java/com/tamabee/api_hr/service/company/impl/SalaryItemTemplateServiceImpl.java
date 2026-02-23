package com.tamabee.api_hr.service.company.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamabee.api_hr.dto.request.payroll.CreateSalaryItemTemplateRequest;
import com.tamabee.api_hr.dto.request.payroll.UpdateSalaryItemTemplateRequest;
import com.tamabee.api_hr.dto.response.payroll.SalaryItemTemplateResponse;
import com.tamabee.api_hr.entity.payroll.EmployeeSalaryItemEntity;
import com.tamabee.api_hr.entity.payroll.SalaryItemTemplateEntity;
import com.tamabee.api_hr.enums.ErrorCode;
import com.tamabee.api_hr.enums.SalaryItemType;
import com.tamabee.api_hr.exception.NotFoundException;
import com.tamabee.api_hr.mapper.company.SalaryItemTemplateMapper;
import com.tamabee.api_hr.repository.payroll.EmployeeSalaryItemRepository;
import com.tamabee.api_hr.repository.payroll.SalaryItemTemplateRepository;
import com.tamabee.api_hr.service.company.interfaces.ISalaryItemTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation quản lý template phụ cấp/khấu trừ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryItemTemplateServiceImpl implements ISalaryItemTemplateService {

    private final SalaryItemTemplateRepository templateRepository;
    private final EmployeeSalaryItemRepository salaryItemRepository;
    private final SalaryItemTemplateMapper templateMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SalaryItemTemplateResponse> getAllTemplates() {
        List<SalaryItemTemplateEntity> templates = templateRepository.findByDeletedFalse();
        return templateMapper.toResponseList(templates);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryItemTemplateResponse> getTemplatesByType(SalaryItemType type) {
        List<SalaryItemTemplateEntity> templates = templateRepository.findByTypeAndDeletedFalse(type);
        return templateMapper.toResponseList(templates);
    }

    @Override
    @Transactional
    public SalaryItemTemplateResponse createTemplate(CreateSalaryItemTemplateRequest request) {
        SalaryItemTemplateEntity entity = templateMapper.toEntity(request);
        entity = templateRepository.save(entity);

        log.info("Đã tạo template {} với type {}", entity.getId(), entity.getType());
        return templateMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public SalaryItemTemplateResponse updateTemplate(Long id, UpdateSalaryItemTemplateRequest request) {
        SalaryItemTemplateEntity entity = findTemplate(id);
        entity.setName(request.getName());
        entity = templateRepository.save(entity);

        log.info("Đã cập nhật template {}", id);
        return templateMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        SalaryItemTemplateEntity entity = findTemplate(id);

        // Xóa tất cả employee salary items đang sử dụng template này
        List<EmployeeSalaryItemEntity> salaryItems = salaryItemRepository.findByTemplateIdAndDeletedFalse(id);
        for (EmployeeSalaryItemEntity item : salaryItems) {
            item.setDeleted(true);
        }
        if (!salaryItems.isEmpty()) {
            salaryItemRepository.saveAll(salaryItems);
            log.info("Đã xóa {} employee salary items của template {}", salaryItems.size(), id);
        }

        entity.setDeleted(true);
        templateRepository.save(entity);

        log.info("Đã xóa template {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public long getEmployeeCountByTemplateId(Long id) {
        // Kiểm tra template tồn tại
        findTemplate(id);
        return salaryItemRepository.countEmployeesByTemplateId(id);
    }

    /**
     * Tìm template theo ID
     */
    private SalaryItemTemplateEntity findTemplate(Long id) {
        return templateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy template",
                        ErrorCode.TEMPLATE_NOT_FOUND));
    }
}
