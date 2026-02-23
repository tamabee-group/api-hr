package com.tamabee.api_hr.repository.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.attendance.AttendanceRecordEntity;
import com.tamabee.api_hr.enums.AttendanceStatus;

/**
 * Repository quản lý bản ghi chấm công của nhân viên.
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, Long> {

        /**
         * Tìm bản ghi chấm công của nhân viên theo ngày
         */
        Optional<AttendanceRecordEntity> findByEmployeeIdAndWorkDate(
                        Long employeeId, LocalDate workDate);

        /**
         * Lấy danh sách chấm công trong khoảng thời gian (phân trang)
         */
        @Query("SELECT a FROM AttendanceRecordEntity a " +
                        "WHERE a.workDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY a.workDate DESC, a.employeeId ASC")
        Page<AttendanceRecordEntity> findByWorkDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        /**
         * Lấy danh sách chấm công có vị trí trong khoảng thời gian (phân trang)
         * Bao gồm records có vị trí check-in/check-out hoặc có break records có vị trí
         */
        @Query("SELECT DISTINCT a FROM AttendanceRecordEntity a " +
                        "LEFT JOIN BreakRecordEntity b ON b.attendanceRecordId = a.id " +
                        "WHERE a.workDate BETWEEN :startDate AND :endDate " +
                        "AND (a.checkInLatitude IS NOT NULL OR a.checkOutLatitude IS NOT NULL " +
                        "OR b.breakStartLatitude IS NOT NULL OR b.breakEndLatitude IS NOT NULL) " +
                        "ORDER BY a.workDate DESC, a.employeeId ASC")
        Page<AttendanceRecordEntity> findWithLocationByWorkDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        /**
         * Lấy danh sách chấm công của nhân viên trong khoảng thời gian
         */
        @Query("SELECT a FROM AttendanceRecordEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "AND a.workDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY a.workDate DESC")
        List<AttendanceRecordEntity> findByEmployeeIdAndWorkDateBetween(
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Lấy danh sách chấm công của nhân viên trong khoảng thời gian (phân trang)
         */
        @Query("SELECT a FROM AttendanceRecordEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "AND a.workDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY a.workDate DESC")
        Page<AttendanceRecordEntity> findByEmployeeIdAndWorkDateBetweenPaged(
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        /**
         * Đếm số ngày làm việc của nhân viên trong khoảng thời gian
         */
        @Query("SELECT COUNT(a) FROM AttendanceRecordEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "AND a.workDate BETWEEN :startDate AND :endDate " +
                        "AND a.status = :status")
        long countByEmployeeIdAndWorkDateBetweenAndStatus(
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("status") AttendanceStatus status);

        /**
         * Tính tổng số phút làm việc của nhân viên trong khoảng thời gian
         */
        @Query("SELECT COALESCE(SUM(a.workingMinutes), 0) FROM AttendanceRecordEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "AND a.workDate BETWEEN :startDate AND :endDate")
        Integer sumWorkingMinutesByEmployeeIdAndWorkDateBetween(
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Tính tổng số phút tăng ca của nhân viên trong khoảng thời gian
         */
        @Query("SELECT COALESCE(SUM(a.overtimeMinutes), 0) FROM AttendanceRecordEntity a " +
                        "WHERE a.employeeId = :employeeId " +
                        "AND a.workDate BETWEEN :startDate AND :endDate")
        Integer sumOvertimeMinutesByEmployeeIdAndWorkDateBetween(
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Kiểm tra nhân viên đã chấm công ngày hôm nay chưa
         */
        boolean existsByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

        /**
         * Lấy danh sách chấm công theo ngày (phân trang, cho kiosk)
         */
        @Query("SELECT a FROM AttendanceRecordEntity a " +
                        "WHERE a.workDate = :workDate " +
                        "ORDER BY a.createdAt DESC")
        List<AttendanceRecordEntity> findByWorkDate(
                        @Param("workDate") LocalDate workDate,
                        Pageable pageable);

        /**
         * Lấy tất cả bản ghi chấm công theo ngày (không phân trang)
         */
        @Query("SELECT a FROM AttendanceRecordEntity a " +
                        "WHERE a.workDate = :workDate")
        List<AttendanceRecordEntity> findAllByWorkDate(
                        @Param("workDate") LocalDate workDate);
        /**
         * Tính tổng số phút làm việc của toàn bộ nhân viên trong khoảng thời gian
         */
        @Query("SELECT COALESCE(SUM(a.workingMinutes), 0) FROM AttendanceRecordEntity a " +
                        "WHERE a.workDate BETWEEN :startDate AND :endDate")
        long sumWorkingMinutesByWorkDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Tính tổng số phút tăng ca của toàn bộ nhân viên trong khoảng thời gian
         */
        @Query("SELECT COALESCE(SUM(a.overtimeMinutes), 0) FROM AttendanceRecordEntity a " +
                        "WHERE a.workDate BETWEEN :startDate AND :endDate")
        long sumOvertimeMinutesByWorkDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
