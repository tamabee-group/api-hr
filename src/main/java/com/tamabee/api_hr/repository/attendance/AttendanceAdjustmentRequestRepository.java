package com.tamabee.api_hr.repository.attendance;

import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.enums.AdjustmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository quản lý yêu cầu điều chỉnh chấm công.
 */
@Repository
public interface AttendanceAdjustmentRequestRepository extends JpaRepository<AttendanceAdjustmentRequestEntity, Long> {

        /**
         * Lấy danh sách yêu cầu đang chờ duyệt (phân trang)
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.status = 'PENDING' " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findPending(Pageable pageable);

        /**
         * Lấy tất cả yêu cầu (phân trang)
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findAllPaged(Pageable pageable);

        /**
         * Lấy danh sách yêu cầu đang chờ duyệt được gán cho người dùng cụ thể
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.assignedTo = :assignedTo " +
                        "AND a.status = 'PENDING' " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findPendingByAssignedTo(
                        @Param("assignedTo") Long assignedTo,
                        Pageable pageable);

        /**
         * Lấy tất cả yêu cầu được gán cho người dùng cụ thể
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.assignedTo = :assignedTo " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findByAssignedTo(
                        @Param("assignedTo") Long assignedTo,
                        Pageable pageable);

        /**
         * Lấy danh sách yêu cầu của nhân viên (phân trang)
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findByEmployeeId(
                        @Param("employeeId") Long employeeId,
                        Pageable pageable);

        /**
         * Lấy lịch sử điều chỉnh của một bản ghi chấm công
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.attendanceRecordId = :attendanceRecordId " +
                        "ORDER BY a.createdAt DESC")
        List<AttendanceAdjustmentRequestEntity> findByAttendanceRecordId(
                        @Param("attendanceRecordId") Long attendanceRecordId);

        /**
         * Lấy danh sách yêu cầu theo trạng thái (phân trang)
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.status = :status " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findByStatus(
                        @Param("status") AdjustmentStatus status,
                        Pageable pageable);

        /**
         * Đếm số yêu cầu đang chờ duyệt
         */
        @Query("SELECT COUNT(a) FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.status = 'PENDING'")
        long countPending();

        /**
         * Kiểm tra nhân viên có yêu cầu đang chờ duyệt cho bản ghi chấm công không
         */
        @Query("SELECT COUNT(a) > 0 FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.attendanceRecordId = :attendanceRecordId " +
                        "AND a.status = 'PENDING'")
        boolean existsPendingByAttendanceRecordId(@Param("attendanceRecordId") Long attendanceRecordId);

        /**
         * Kiểm tra nhân viên có yêu cầu đang chờ duyệt cho ngày làm việc không
         */
        @Query("SELECT COUNT(a) > 0 FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "AND a.workDate = :workDate " +
                        "AND a.status = 'PENDING'")
        boolean existsPendingByEmployeeIdAndWorkDate(
                        @Param("employeeId") Long employeeId,
                        @Param("workDate") LocalDate workDate);

        /**
         * Lấy danh sách yêu cầu điều chỉnh của nhân viên theo ngày làm việc
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "AND a.workDate = :workDate " +
                        "ORDER BY a.createdAt DESC")
        List<AttendanceAdjustmentRequestEntity> findByEmployeeIdAndWorkDate(
                        @Param("employeeId") Long employeeId,
                        @Param("workDate") LocalDate workDate);
}
