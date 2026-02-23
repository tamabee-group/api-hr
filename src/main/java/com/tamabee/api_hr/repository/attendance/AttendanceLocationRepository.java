package com.tamabee.api_hr.repository.attendance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.company.AttendanceLocationEntity;

/**
 * Repository quản lý vị trí chấm công.
 * Hỗ trợ soft delete và phân trang.
 */
@Repository
public interface AttendanceLocationRepository extends JpaRepository<AttendanceLocationEntity, Long> {

    /**
     * Lấy danh sách vị trí chấm công (phân trang, chưa bị xóa)
     */
    Page<AttendanceLocationEntity> findByDeletedFalse(Pageable pageable);

    /**
     * Tìm vị trí chấm công theo ID (chưa bị xóa)
     */
    Optional<AttendanceLocationEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách vị trí chấm công đang hoạt động (chưa bị xóa)
     */
    List<AttendanceLocationEntity> findByDeletedFalseAndIsActiveTrue();
}
