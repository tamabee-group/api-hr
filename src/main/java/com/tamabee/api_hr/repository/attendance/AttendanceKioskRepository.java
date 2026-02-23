package com.tamabee.api_hr.repository.attendance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.company.AttendanceKioskEntity;

/**
 * Repository quản lý máy chấm công cố định (kiosk).
 */
@Repository
public interface AttendanceKioskRepository extends JpaRepository<AttendanceKioskEntity, Long> {

    Page<AttendanceKioskEntity> findByDeletedFalse(Pageable pageable);

    Optional<AttendanceKioskEntity> findByIdAndDeletedFalse(Long id);

    List<AttendanceKioskEntity> findByDeletedFalseAndIsActiveTrue();

    Optional<AttendanceKioskEntity> findByPinCodeAndDeletedFalseAndIsActiveTrue(String pinCode);

    boolean existsByPinCodeAndDeletedFalse(String pinCode);
}
