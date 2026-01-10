package com.tamabee.api_hr.repository.attendance;

import com.tamabee.api_hr.entity.attendance.ScheduleSelectionEntity;
import com.tamabee.api_hr.enums.SelectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý yêu cầu chọn lịch làm việc của nhân viên.
 */
@Repository
public interface ScheduleSelectionRepository extends JpaRepository<ScheduleSelectionEntity, Long> {

        /**
         * Lấy danh sách yêu cầu đang chờ duyệt (phân trang)
         */
        @Query("SELECT s FROM ScheduleSelectionEntity s " +
                        "WHERE s.status = 'PENDING' " +
                        "ORDER BY s.createdAt DESC")
        Page<ScheduleSelectionEntity> findPending(Pageable pageable);

        /**
         * Lấy lịch sử chọn lịch của nhân viên (sắp xếp theo thời gian tạo giảm dần)
         */
        @Query("SELECT s FROM ScheduleSelectionEntity s " +
                        "WHERE s.employeeId = :employeeId " +
                        "ORDER BY s.createdAt DESC")
        List<ScheduleSelectionEntity> findByEmployeeIdOrderByCreatedAtDesc(
                        @Param("employeeId") Long employeeId);

        /**
         * Lấy lịch sử chọn lịch của nhân viên (phân trang)
         */
        @Query("SELECT s FROM ScheduleSelectionEntity s " +
                        "WHERE s.employeeId = :employeeId " +
                        "ORDER BY s.createdAt DESC")
        Page<ScheduleSelectionEntity> findByEmployeeIdOrderByCreatedAtDescPaged(
                        @Param("employeeId") Long employeeId,
                        Pageable pageable);

        /**
         * Lấy danh sách yêu cầu theo trạng thái (phân trang)
         */
        @Query("SELECT s FROM ScheduleSelectionEntity s " +
                        "WHERE s.status = :status " +
                        "ORDER BY s.createdAt DESC")
        Page<ScheduleSelectionEntity> findByStatus(
                        @Param("status") SelectionStatus status,
                        Pageable pageable);

        /**
         * Đếm số yêu cầu đang chờ duyệt
         */
        @Query("SELECT COUNT(s) FROM ScheduleSelectionEntity s " +
                        "WHERE s.status = 'PENDING'")
        long countPending();

        /**
         * Lấy các lịch đã được nhân viên chọn và được duyệt (để gợi ý)
         */
        @Query("SELECT DISTINCT s.scheduleId FROM ScheduleSelectionEntity s " +
                        "WHERE s.employeeId = :employeeId " +
                        "AND s.status = 'APPROVED'")
        List<Long> findApprovedScheduleIdsByEmployeeId(@Param("employeeId") Long employeeId);

        /**
         * Kiểm tra nhân viên có yêu cầu đang chờ duyệt không
         */
        @Query("SELECT COUNT(s) > 0 FROM ScheduleSelectionEntity s " +
                        "WHERE s.employeeId = :employeeId " +
                        "AND s.status = 'PENDING'")
        boolean existsPendingByEmployeeId(@Param("employeeId") Long employeeId);
}
