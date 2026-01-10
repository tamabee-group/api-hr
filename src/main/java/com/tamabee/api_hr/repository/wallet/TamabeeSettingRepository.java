package com.tamabee.api_hr.repository.wallet;

import com.tamabee.api_hr.entity.wallet.TamabeeSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho quản lý cấu hình hệ thống Tamabee
 */
@Repository
public interface TamabeeSettingRepository extends JpaRepository<TamabeeSettingEntity, Long> {

    /**
     * Tìm setting theo key và chưa bị xóa
     */
    Optional<TamabeeSettingEntity> findBySettingKeyAndDeletedFalse(String settingKey);

    /**
     * Lấy tất cả settings chưa bị xóa
     */
    List<TamabeeSettingEntity> findByDeletedFalse();

    /**
     * Lấy tất cả settings chưa bị xóa, sắp xếp theo createdAt
     */
    List<TamabeeSettingEntity> findByDeletedFalseOrderByIdAsc();

    /**
     * Kiểm tra setting key có tồn tại và chưa bị xóa
     */
    boolean existsBySettingKeyAndDeletedFalse(String settingKey);
}
