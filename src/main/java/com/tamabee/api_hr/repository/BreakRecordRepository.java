package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.attendance.BreakRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý bản ghi giờ giải lao của nhân viên.
 */
@Repository
public interface BreakRecordRepository extends JpaRepository<BreakRecordEntity, Long> {

        /**
         * Tìm bản ghi giải lao theo ID (chưa bị xóa)
         */
        Optional<BreakRecordEntity> findByIdAndDeletedFalse(Long id);

        /**
         * Lấy danh sách bản ghi giải lao theo bản ghi chấm công
         */
        @Query("SELECT b FROM BreakRecordEntity b WHERE b.deleted = false " +
                        "AND b.attendanceRecordId = :attendanceRecordId " +
                        "ORDER BY b.breakStart ASC")
        List<BreakRecordEntity> findByAttendanceRecordIdAndDeletedFalse(
                        @Param("attendanceRecordId") Long attendanceRecordId);

        /**
         * Lấy danh sách bản ghi giải lao của nhân viên theo ngày làm việc
         */
        @Query("SELECT b FROM BreakRecordEntity b WHERE b.deleted = false " +
                        "AND b.employeeId = :employeeId " +
                        "AND b.workDate = :workDate " +
                        "ORDER BY b.breakStart ASC")
        List<BreakRecordEntity> findByEmployeeIdAndWorkDateAndDeletedFalse(
                        @Param("employeeId") Long employeeId,
                        @Param("workDate") LocalDate workDate);

        /**
         * Lấy danh sách bản ghi giải lao của công ty trong khoảng thời gian
         */
        @Query("SELECT b FROM BreakRecordEntity b WHERE b.deleted = false " +
                        "AND b.companyId = :companyId " +
                        "AND b.workDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY b.workDate DESC, b.employeeId ASC, b.breakStart ASC")
        List<BreakRecordEntity> findByCompanyIdAndWorkDateBetweenAndDeletedFalse(
                        @Param("companyId") Long companyId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Tìm break đang active (breakEnd is null) của nhân viên theo ngày làm việc
         */
        @Query("SELECT b FROM BreakRecordEntity b WHERE b.deleted = false " +
                        "AND b.employeeId = :employeeId " +
                        "AND b.workDate = :workDate " +
                        "AND b.breakEnd IS NULL")
        Optional<BreakRecordEntity> findActiveBreakByEmployeeIdAndWorkDate(
                        @Param("employeeId") Long employeeId,
                        @Param("workDate") LocalDate workDate);

        /**
         * Đếm số breaks của một attendance record
         */
        long countByAttendanceRecordIdAndDeletedFalse(Long attendanceRecordId);

        /**
         * Lấy breakNumber lớn nhất của một attendance record
         */
        @Query("SELECT COALESCE(MAX(b.breakNumber), 0) FROM BreakRecordEntity b " +
                        "WHERE b.deleted = false " +
                        "AND b.attendanceRecordId = :attendanceRecordId")
        Integer findMaxBreakNumberByAttendanceRecordId(
                        @Param("attendanceRecordId") Long attendanceRecordId);

        /**
         * Lấy danh sách completed breaks (có breakEnd) của một attendance record
         */
        @Query("SELECT b FROM BreakRecordEntity b WHERE b.deleted = false " +
                        "AND b.attendanceRecordId = :attendanceRecordId " +
                        "AND b.breakEnd IS NOT NULL " +
                        "ORDER BY b.breakStart ASC")
        List<BreakRecordEntity> findCompletedBreaksByAttendanceRecordId(
                        @Param("attendanceRecordId") Long attendanceRecordId);
}
