package com.tamabee.api_hr.mapper.company;

import org.springframework.stereotype.Component;

import com.tamabee.api_hr.entity.company.CompanySettingEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mapper cho CompanySettingEntity.
 * Giữ lại mapper cho initializeDefaultSettings().
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanySettingMapper {

    /**
     * Tạo entity mới với default values
     */
    public CompanySettingEntity toEntity() {
        CompanySettingEntity entity = new CompanySettingEntity();
        return entity;
    }
}
