package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.leave.LeaveRequestEntity;
import com.tamabee.api_hr.enums.LeaveStatus;
import com.tamabee.api_hr.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository quản lý yêu cầu nghỉ phép.
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {

        /**
         * Lấy danh sách yêu cầu đang chờ duyệt (phân trang)
         */
        @Query("SELECT l FROM LeaveRequestEntity l " +
                        "WHERE l.status = 'PENDING' " +
                        "ORDER BY l.createdAt DESC")
        Page<LeaveRequestEntity> findPending(Pageable pageable);

        /**
         * Lấy danh sách yêu cầu của nhân viên (phân trang)
         */
        @Query("SELECT l FROM LeaveRequestEntity l " +
                        "WHERE l.employeeId = :employeeId " +
                        "ORDER BY l.createdAt DESC")
        Page<LeaveRequestEntity> findByEmployeeId(
                        @Param("employeeId") Long employeeId,
                        Pageable pageable);

        /**
         * Lấy danh sách yêu cầu theo trạng thái (phân trang)
         */
        Page<LeaveRequestEntity> findByStatus(LeaveStatus status, Pageable pageable);

        /**
         * Lấy tất cả yêu cầu (phân trang)
         */
        Page<LeaveRequestEntity> findAll(Pageable pageable);

        /**
         * Lấy danh sách nghỉ phép đã duyệt của nhân viên trong khoảng thời gian
         */
        @Query("SELECT l FROM LeaveRequestEntity l " +
                        "WHERE l.employeeId = :employeeId " +
                        "AND l.status = 'APPROVED' " +
                        "AND l.startDate <= :endDate " +
                        "AND l.endDate >= :startDate " +
                        "ORDER BY l.startDate ASC")
        List<LeaveRequestEntity> findApprovedByEmployeeIdAndDateRange(
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * Kiểm tra nhân viên có nghỉ phép trong ngày không
         */
        @Query("SELECT COUNT(l) > 0 FROM LeaveRequestEntity l " +
                        "WHERE l.employeeId = :employeeId " +
                        "AND l.status = 'APPROVED' " +
                        "AND l.startDate <= :date " +
                        "AND l.endDate >= :date")
        boolean isOnLeave(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

        /**
         * Đếm số yêu cầu đang chờ duyệt
         */
        long countByStatus(LeaveStatus status);

        /**
         * Tính tổng số ngày nghỉ đã duyệt của nhân viên theo loại trong năm
         */
        @Query("SELECT COALESCE(SUM(l.totalDays), 0) FROM LeaveRequestEntity l " +
                        "WHERE l.employeeId = :employeeId " +
                        "AND l.leaveType = :leaveType " +
                        "AND l.status = 'APPROVED' " +
                        "AND YEAR(l.startDate) = :year")
        Integer sumApprovedDaysByEmployeeIdAndTypeAndYear(
                        @Param("employeeId") Long employeeId,
                        @Param("leaveType") LeaveType leaveType,
                        @Param("year") Integer year);

        /**
         * Kiểm tra có yêu cầu nghỉ phép trùng lặp không
         */
        @Query("SELECT COUNT(l) > 0 FROM LeaveRequestEntity l " +
                        "WHERE l.employeeId = :employeeId " +
                        "AND l.status IN ('PENDING', 'APPROVED') " +
                        "AND l.startDate <= :endDate " +
                        "AND l.endDate >= :startDate")
        boolean existsOverlappingRequest(
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
