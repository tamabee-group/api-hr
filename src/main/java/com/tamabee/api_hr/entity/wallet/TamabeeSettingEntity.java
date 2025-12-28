package com.tamabee.api_hr.entity.wallet;

import com.tamabee.api_hr.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity cho cấu hình hệ thống Tamabee
 * Lưu trữ các thông số như: FREE_TRIAL_MONTHS, REFERRAL_BONUS_MONTHS,
 * COMMISSION_AMOUNT
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "tamabee_settings", indexes = {
        @Index(name = "idx_tamabee_settings_deleted", columnList = "deleted")
})
public class TamabeeSettingEntity extends BaseEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false)
    private String settingValue;

    @Column(length = 500)
    private String description;

    // Loại giá trị: INTEGER, DECIMAL, STRING, BOOLEAN
    @Column(name = "value_type", nullable = false, length = 50)
    private String valueType = "STRING";
}
