package com.tamabee.api_hr.repository.company;

import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý cấu hình chấm công và tính lương.
 */
@Repository
public interface CompanySettingsRepository extends JpaRepository<CompanySettingEntity, Long> {

    /**
     * Tìm settings (chưa bị xóa) - mỗi tenant chỉ có 1 settings
     */
    Optional<CompanySettingEntity> findFirstByDeletedFalse();

    /**
     * Kiểm tra settings đã tồn tại chưa
     */
    boolean existsByDeletedFalse();
}
