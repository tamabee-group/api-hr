package com.tamabee.api_hr.repository.company;

import com.tamabee.api_hr.entity.company.OvertimeSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý cấu hình tăng ca.
 * Mỗi tenant chỉ có 1 row overtime_settings.
 */
@Repository
public interface OvertimeSettingRepository extends JpaRepository<OvertimeSettingEntity, Long> {

    /**
     * Tìm cấu hình tăng ca (chưa bị xóa) - mỗi tenant chỉ có 1 row
     */
    Optional<OvertimeSettingEntity> findFirstByDeletedFalse();
}
