package com.tamabee.api_hr.repository.company;

import com.tamabee.api_hr.entity.company.PayrollSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý cấu hình tính lương.
 * Mỗi tenant chỉ có 1 row payroll_settings.
 */
@Repository
public interface PayrollSettingRepository extends JpaRepository<PayrollSettingEntity, Long> {

    /**
     * Tìm cấu hình tính lương (chưa bị xóa) - mỗi tenant chỉ có 1 row
     */
    Optional<PayrollSettingEntity> findFirstByDeletedFalse();
}
