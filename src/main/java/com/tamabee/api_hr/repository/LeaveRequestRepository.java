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
import java.util.Optional;

/**
 * Repository quản lý yêu cầu nghỉ phép.
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {

        /**
         * Tìm yêu cầu nghỉ phép theo ID (chưa bị xóa)
         */
        Optional<LeaveRequestEntity> findByIdAndDeletedFalse(Long id);

        /**
         * Lấy danh sách yêu cầu đang chờ duyệt của công ty (phân trang)
         */
        @Query("SELECT l FROM LeaveRequestEntity l WHERE l.deleted = false " +
                        "AND l.companyId = :companyId " +
                        "AND l.status = 'PENDING' " +
                        "ORDER BY l.createdAt DESC")
        Page<LeaveRequestEntity> findPendingByCompanyId(
                        @Param("companyId") Long companyId,
                        Pageable pageable);

        /**
         * Lấy danh sách yêu cầu của nhân viên (phân trang)
         */
        @Query("SELECT l FROM LeaveRequestEntity l WHERE l.deleted = false " +
                        "AND l.employeeId = :employeeId " +
                        "ORDER BY l.createdAt DESC")
        Page<LeaveRequestEntity> findByEmployeeId(
                        @Param("employeeId") Long employeeId,
                        Pageable pageable);

        /**
         * Lấy danh sách yêu cầu theo trạng thái của công ty (phân trang)
         */
        @Query("SELECT l FROM LeaveRequestEntity l WHERE l.deleted = false " +
                        "AND l.companyId = :companyId " +
                        "AND l.status = :status " +
                        "ORDER BY l.createdAt DESC")
        Page<LeaveRequestEntity> findByCompanyIdAndStatus(
                        @Param("companyId") Long companyId,
                        @Param("status") LeaveStatus status,
                        Pageable pageable);

        /**
         * Lấy tất cả yêu cầu của công ty (phân trang)
         */
        @Query("SELECT l FROM LeaveRequestEntity l WHERE l.deleted = false " +
                        "AND l.companyId = :companyId " +
                        "ORDER BY l.createdAt DESC")
        Page<LeaveRequestEntity> findByCompanyId(
                        @Param("companyId") Long companyId,
                        Pageable pageable);

        /**
         * Lấy danh sách nghỉ phép đã duyệt của nhân viên trong khoảng thời gian
         */
        @Query("SELECT l FROM LeaveRequestEntity l WHERE l.deleted = false " +
                        "AND l.employeeId = :employeeId " +
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
        @Query("SELECT COUNT(l) > 0 FROM LeaveRequestEntity l WHERE l.deleted = false " +
                        "AND l.employeeId = :employeeId " +
                        "AND l.status = 'APPROVED' " +
                        "AND l.startDate <= :date " +
                        "AND l.endDate >= :date")
        boolean isOnLeave(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

        /**
         * Đếm số yêu cầu đang chờ duyệt của công ty
         */
        @Query("SELECT COUNT(l) FROM LeaveRequestEntity l WHERE l.deleted = false " +
                        "AND l.companyId = :companyId " +
                        "AND l.status = 'PENDING'")
        long countPendingByCompanyId(@Param("companyId") Long companyId);

        /**
         * Tính tổng số ngày nghỉ đã duyệt của nhân viên theo loại trong năm
         */
        @Query("SELECT COALESCE(SUM(l.totalDays), 0) FROM LeaveRequestEntity l WHERE l.deleted = false " +
                        "AND l.employeeId = :employeeId " +
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
        @Query("SELECT COUNT(l) > 0 FROM LeaveRequestEntity l WHERE l.deleted = false " +
                        "AND l.employeeId = :employeeId " +
                        "AND l.status IN ('PENDING', 'APPROVED') " +
                        "AND l.startDate <= :endDate " +
                        "AND l.endDate >= :startDate")
        boolean existsOverlappingRequest(
                        @Param("employeeId") Long employeeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
