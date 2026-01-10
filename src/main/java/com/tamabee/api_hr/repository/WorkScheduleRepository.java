package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.attendance.WorkScheduleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý lịch làm việc.
 */
@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkScheduleEntity, Long> {

    /**
     * Tìm lịch làm việc theo ID (chưa bị xóa)
     */
    Optional<WorkScheduleEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách lịch làm việc (phân trang)
     */
    Page<WorkScheduleEntity> findByDeletedFalse(Pageable pageable);

    /**
     * Lấy tất cả lịch làm việc đang active
     */
    List<WorkScheduleEntity> findByDeletedFalseAndIsActiveTrue();

    /**
     * Tìm lịch làm việc mặc định
     */
    @Query("SELECT w FROM WorkScheduleEntity w WHERE w.deleted = false AND w.isDefault = true")
    Optional<WorkScheduleEntity> findDefault();

    /**
     * Tìm lịch làm việc mặc định (alias)
     */
    @Query("SELECT w FROM WorkScheduleEntity w WHERE w.deleted = false AND w.isDefault = true")
    Optional<WorkScheduleEntity> findDefaultSchedule();

    /**
     * Kiểm tra tên lịch đã tồn tại chưa
     */
    boolean existsByNameAndDeletedFalse(String name);

    /**
     * Đếm số lịch làm việc
     */
    long countByDeletedFalse();
}
