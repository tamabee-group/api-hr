package com.tamabee.api_hr.repository.leave;

import com.tamabee.api_hr.entity.leave.LeaveBalanceEntity;
import com.tamabee.api_hr.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý số ngày phép còn lại của nhân viên.
 * Entity này KHÔNG có soft delete - xóa thẳng.
 */
@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalanceEntity, Long> {

        /**
         * Lấy tất cả balance của nhân viên theo năm
         */
        @Query("SELECT b FROM LeaveBalanceEntity b " +
                        "WHERE b.employeeId = :employeeId " +
                        "AND b.year = :year " +
                        "ORDER BY b.leaveType ASC")
        List<LeaveBalanceEntity> findByEmployeeIdAndYear(
                        @Param("employeeId") Long employeeId,
                        @Param("year") Integer year);

        /**
         * Tìm balance của nhân viên theo năm và loại phép
         */
        Optional<LeaveBalanceEntity> findByEmployeeIdAndYearAndLeaveType(
                        Long employeeId, Integer year, LeaveType leaveType);

        /**
         * Kiểm tra balance đã tồn tại chưa
         */
        boolean existsByEmployeeIdAndYearAndLeaveType(
                        Long employeeId, Integer year, LeaveType leaveType);

        /**
         * Lấy tổng số ngày phép còn lại của nhân viên trong năm
         */
        @Query("SELECT COALESCE(SUM(b.remainingDays), 0) FROM LeaveBalanceEntity b " +
                        "WHERE b.employeeId = :employeeId " +
                        "AND b.year = :year")
        Integer sumRemainingDaysByEmployeeIdAndYear(
                        @Param("employeeId") Long employeeId,
                        @Param("year") Integer year);

        /**
         * Lấy số ngày phép còn lại theo loại
         */
        @Query("SELECT COALESCE(b.remainingDays, 0) FROM LeaveBalanceEntity b " +
                        "WHERE b.employeeId = :employeeId " +
                        "AND b.year = :year " +
                        "AND b.leaveType = :leaveType")
        Integer getRemainingDaysByEmployeeIdAndYearAndType(
                        @Param("employeeId") Long employeeId,
                        @Param("year") Integer year,
                        @Param("leaveType") LeaveType leaveType);
}
