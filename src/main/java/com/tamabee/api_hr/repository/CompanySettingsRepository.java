package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.company.CompanySettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý cấu hình chấm công và tính lương của công ty.
 */
@Repository
public interface CompanySettingsRepository extends JpaRepository<CompanySettingEntity, Long> {

    /**
     * Tìm settings của công ty theo companyId (chưa bị xóa)
     */
    Optional<CompanySettingEntity> findByCompanyIdAndDeletedFalse(Long companyId);

    /**
     * Kiểm tra settings đã tồn tại cho công ty chưa
     */
    boolean existsByCompanyIdAndDeletedFalse(Long companyId);
}
