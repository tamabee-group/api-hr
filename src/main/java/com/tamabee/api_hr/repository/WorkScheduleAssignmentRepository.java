package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.attendance.WorkScheduleAssignmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý việc gán lịch làm việc cho nhân viên.
 */
@Repository
public interface WorkScheduleAssignmentRepository extends JpaRepository<WorkScheduleAssignmentEntity, Long> {

    /**
     * Tìm assignment theo ID (chưa bị xóa)
     */
    Optional<WorkScheduleAssignmentEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Tìm lịch làm việc hiệu lực của nhân viên tại một ngày cụ thể
     * Điều kiện: effectiveFrom <= date AND (effectiveTo IS NULL OR effectiveTo >=
     * date)
     */
    @Query("SELECT a FROM WorkScheduleAssignmentEntity a WHERE a.deleted = false " +
            "AND a.employeeId = :employeeId " +
            "AND a.effectiveFrom <= :date " +
            "AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date) " +
            "ORDER BY a.effectiveFrom DESC")
    List<WorkScheduleAssignmentEntity> findByEmployeeIdAndEffectiveDate(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date);

    /**
     * Tìm các assignment của nhân viên trong khoảng thời gian
     */
    @Query("SELECT a FROM WorkScheduleAssignmentEntity a WHERE a.deleted = false " +
            "AND a.employeeId = :employeeId " +
            "AND a.effectiveFrom <= :endDate " +
            "AND (a.effectiveTo IS NULL OR a.effectiveTo >= :startDate) " +
            "ORDER BY a.effectiveFrom DESC")
    List<WorkScheduleAssignmentEntity> findByEmployeeIdAndEffectiveDateRange(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Lấy danh sách assignment theo scheduleId (phân trang)
     */
    Page<WorkScheduleAssignmentEntity> findByScheduleIdAndDeletedFalse(Long scheduleId, Pageable pageable);

    /**
     * Lấy tất cả assignment theo scheduleId
     */
    List<WorkScheduleAssignmentEntity> findAllByScheduleIdAndDeletedFalse(Long scheduleId);

    /**
     * Kiểm tra nhân viên đã có assignment trong khoảng thời gian chưa
     */
    @Query("SELECT COUNT(a) > 0 FROM WorkScheduleAssignmentEntity a WHERE a.deleted = false " +
            "AND a.employeeId = :employeeId " +
            "AND a.effectiveFrom <= :endDate " +
            "AND (a.effectiveTo IS NULL OR a.effectiveTo >= :startDate)")
    boolean existsOverlappingAssignment(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
