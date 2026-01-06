package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.attendance.AttendanceAdjustmentRequestEntity;
import com.tamabee.api_hr.enums.AdjustmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý yêu cầu điều chỉnh chấm công.
 * Entity này KHÔNG có soft delete - xóa thẳng.
 */
@Repository
public interface AttendanceAdjustmentRequestRepository extends JpaRepository<AttendanceAdjustmentRequestEntity, Long> {

        /**
         * Lấy danh sách yêu cầu đang chờ duyệt của công ty (phân trang)
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.companyId = :companyId " +
                        "AND a.status = 'PENDING' " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findPendingByCompanyId(
                        @Param("companyId") Long companyId,
                        Pageable pageable);

        /**
         * Lấy tất cả yêu cầu của công ty (phân trang)
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.companyId = :companyId " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findByCompanyId(
                        @Param("companyId") Long companyId,
                        Pageable pageable);

        /**
         * Lấy danh sách yêu cầu đang chờ duyệt được gán cho người dùng cụ thể
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.companyId = :companyId " +
                        "AND a.assignedTo = :assignedTo " +
                        "AND a.status = 'PENDING' " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findPendingByCompanyIdAndAssignedTo(
                        @Param("companyId") Long companyId,
                        @Param("assignedTo") Long assignedTo,
                        Pageable pageable);

        /**
         * Lấy tất cả yêu cầu được gán cho người dùng cụ thể
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.companyId = :companyId " +
                        "AND a.assignedTo = :assignedTo " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findByCompanyIdAndAssignedTo(
                        @Param("companyId") Long companyId,
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
         * Lấy danh sách yêu cầu theo trạng thái của công ty (phân trang)
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.companyId = :companyId " +
                        "AND a.status = :status " +
                        "ORDER BY a.createdAt DESC")
        Page<AttendanceAdjustmentRequestEntity> findByCompanyIdAndStatus(
                        @Param("companyId") Long companyId,
                        @Param("status") AdjustmentStatus status,
                        Pageable pageable);

        /**
         * Đếm số yêu cầu đang chờ duyệt của công ty
         */
        @Query("SELECT COUNT(a) FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.companyId = :companyId " +
                        "AND a.status = 'PENDING'")
        long countPendingByCompanyId(@Param("companyId") Long companyId);

        /**
         * Kiểm tra nhân viên có yêu cầu đang chờ duyệt cho bản ghi chấm công không
         */
        @Query("SELECT COUNT(a) > 0 FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.attendanceRecordId = :attendanceRecordId " +
                        "AND a.status = 'PENDING'")
        boolean existsPendingByAttendanceRecordId(@Param("attendanceRecordId") Long attendanceRecordId);

        /**
         * Kiểm tra nhân viên có yêu cầu đang chờ duyệt cho ngày làm việc không (khi
         * không có attendanceRecordId)
         */
        @Query("SELECT COUNT(a) > 0 FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "AND a.workDate = :workDate " +
                        "AND a.status = 'PENDING'")
        boolean existsPendingByEmployeeIdAndWorkDate(
                        @Param("employeeId") Long employeeId,
                        @Param("workDate") java.time.LocalDate workDate);

        /**
         * Lấy danh sách yêu cầu điều chỉnh của nhân viên theo ngày làm việc
         */
        @Query("SELECT a FROM AttendanceAdjustmentRequestEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "AND a.workDate = :workDate " +
                        "ORDER BY a.createdAt DESC")
        List<AttendanceAdjustmentRequestEntity> findByEmployeeIdAndWorkDate(
                        @Param("employeeId") Long employeeId,
                        @Param("workDate") java.time.LocalDate workDate);
}
