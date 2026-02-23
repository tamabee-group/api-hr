package com.tamabee.api_hr.repository.company;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.company.AttendanceSettingEntity;

/**
 * Repository quản lý cấu hình chấm công.
 * Mỗi tenant chỉ có 1 row attendance_settings.
 */
@Repository
public interface AttendanceSettingRepository extends JpaRepository<AttendanceSettingEntity, Long> {

    /**
     * Tìm cấu hình chấm công (chưa bị xóa) - mỗi tenant chỉ có 1 row
     */
    Optional<AttendanceSettingEntity> findFirstByDeletedFalse();
}
