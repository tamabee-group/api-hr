package com.tamabee.api_hr.repository.company;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.company.BreakSettingEntity;

/**
 * Repository quản lý cấu hình giải lao.
 * Mỗi tenant chỉ có 1 row break_settings.
 */
@Repository
public interface BreakSettingRepository extends JpaRepository<BreakSettingEntity, Long> {

    /**
     * Tìm cấu hình giải lao (chưa bị xóa) - mỗi tenant chỉ có 1 row
     */
    Optional<BreakSettingEntity> findFirstByDeletedFalse();
}
