package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.attendance.ScheduleSelectionEntity;
import com.tamabee.api_hr.enums.SelectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý yêu cầu chọn lịch làm việc của nhân viên.
 */
@Repository
public interface ScheduleSelectionRepository extends JpaRepository<ScheduleSelectionEntity, Long> {

    /**
     * Tìm yêu cầu chọn lịch theo ID (chưa bị xóa)
     */
    Optional<ScheduleSelectionEntity> findByIdAndDeletedFalse(Long id);

    /**
     * Lấy danh sách yêu cầu đang chờ duyệt của công ty (phân trang)
     */
    @Query("SELECT s FROM ScheduleSelectionEntity s WHERE s.deleted = false " +
            "AND s.companyId = :companyId " +
            "AND s.status = 'PENDING' " +
            "ORDER BY s.createdAt DESC")
    Page<ScheduleSelectionEntity> findPendingByCompanyId(
            @Param("companyId") Long companyId,
            Pageable pageable);

    /**
     * Lấy lịch sử chọn lịch của nhân viên (sắp xếp theo thời gian tạo giảm dần)
     */
    @Query("SELECT s FROM ScheduleSelectionEntity s WHERE s.deleted = false " +
            "AND s.employeeId = :employeeId " +
            "ORDER BY s.createdAt DESC")
    List<ScheduleSelectionEntity> findByEmployeeIdOrderByCreatedAtDesc(
            @Param("employeeId") Long employeeId);

    /**
     * Lấy lịch sử chọn lịch của nhân viên (phân trang)
     */
    @Query("SELECT s FROM ScheduleSelectionEntity s WHERE s.deleted = false " +
            "AND s.employeeId = :employeeId " +
            "ORDER BY s.createdAt DESC")
    Page<ScheduleSelectionEntity> findByEmployeeIdOrderByCreatedAtDescPaged(
            @Param("employeeId") Long employeeId,
            Pageable pageable);

    /**
     * Lấy danh sách yêu cầu theo trạng thái của công ty (phân trang)
     */
    @Query("SELECT s FROM ScheduleSelectionEntity s WHERE s.deleted = false " +
            "AND s.companyId = :companyId " +
            "AND s.status = :status " +
            "ORDER BY s.createdAt DESC")
    Page<ScheduleSelectionEntity> findByCompanyIdAndStatus(
            @Param("companyId") Long companyId,
            @Param("status") SelectionStatus status,
            Pageable pageable);

    /**
     * Đếm số yêu cầu đang chờ duyệt của công ty
     */
    @Query("SELECT COUNT(s) FROM ScheduleSelectionEntity s WHERE s.deleted = false " +
            "AND s.companyId = :companyId " +
            "AND s.status = 'PENDING'")
    long countPendingByCompanyId(@Param("companyId") Long companyId);

    /**
     * Lấy các lịch đã được nhân viên chọn và được duyệt (để gợi ý)
     */
    @Query("SELECT DISTINCT s.scheduleId FROM ScheduleSelectionEntity s WHERE s.deleted = false " +
            "AND s.employeeId = :employeeId " +
            "AND s.status = 'APPROVED'")
    List<Long> findApprovedScheduleIdsByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Kiểm tra nhân viên có yêu cầu đang chờ duyệt không
     */
    @Query("SELECT COUNT(s) > 0 FROM ScheduleSelectionEntity s WHERE s.deleted = false " +
            "AND s.employeeId = :employeeId " +
            "AND s.status = 'PENDING'")
    boolean existsPendingByEmployeeId(@Param("employeeId") Long employeeId);
}
