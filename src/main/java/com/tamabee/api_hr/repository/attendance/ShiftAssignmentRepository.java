package com.tamabee.api_hr.repository.attendance;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.enums.ShiftAssignmentStatus;

/**
 * Repository quản lý phân ca làm việc cho nhân viên.
 */
@Repository
public interface ShiftAssignmentRepository
                extends JpaRepository<ShiftAssignmentEntity, Long>, JpaSpecificationExecutor<ShiftAssignmentEntity> {

        /**
         * Lấy danh sách shift assignments của nhân viên theo ngày làm việc
         */
        List<ShiftAssignmentEntity> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

        /**
         * Lấy danh sách shift assignments của nhân viên trong khoảng thời gian
         */
        List<ShiftAssignmentEntity> findByEmployeeIdAndWorkDateBetween(
                        Long employeeId, LocalDate startDate, LocalDate endDate);

        /**
         * Lấy danh sách shift assignments (phân trang)
         */
        Page<ShiftAssignmentEntity> findAll(Pageable pageable);

        /**
         * Lấy danh sách shift assignments theo ngày
         */
        List<ShiftAssignmentEntity> findByWorkDate(LocalDate workDate);

        /**
         * Lấy danh sách shift assignments trong khoảng thời gian
         */
        Page<ShiftAssignmentEntity> findByWorkDateBetween(
                        LocalDate startDate, LocalDate endDate, Pageable pageable);

        /**
         * Lấy danh sách shift assignments theo status
         */
        Page<ShiftAssignmentEntity> findByStatus(ShiftAssignmentStatus status, Pageable pageable);

        /**
         * Kiểm tra nhân viên có shift assignment vào ngày cụ thể không
         */
        boolean existsByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

        /**
         * Kiểm tra overlap thời gian ca làm việc cho nhân viên trong ngày
         * Kiểm tra xem ca mới có trùng giờ với các ca đã có không
         */
        @Query("SELECT COUNT(sa) > 0 FROM ShiftAssignmentEntity sa " +
                        "JOIN ShiftTemplateEntity st ON sa.shiftTemplateId = st.id " +
                        "WHERE sa.employeeId = :employeeId " +
                        "AND sa.workDate = :workDate " +
                        "AND sa.status != 'CANCELLED' " +
                        "AND (" +
                        "  (st.startTime < :endTime AND st.endTime > :startTime)" +
                        ")")
        boolean existsTimeOverlap(
                        @Param("employeeId") Long employeeId,
                        @Param("workDate") LocalDate workDate,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime);

        /**
         * Đếm số shift assignments của nhân viên trong khoảng thời gian
         */
        long countByEmployeeIdAndWorkDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

        /**
         * Lấy shift assignment theo shift template
         */
        List<ShiftAssignmentEntity> findByShiftTemplateId(Long shiftTemplateId);

        /**
         * Kiểm tra shift template có đang được sử dụng không
         */
        boolean existsByShiftTemplateId(Long shiftTemplateId);

        /**
         * Tìm shift assignments theo nhân viên, ngày và status
         */
        List<ShiftAssignmentEntity> findByEmployeeIdAndWorkDateAndStatus(
                        Long employeeId, LocalDate workDate, ShiftAssignmentStatus status);

        /**
         * Tìm các ca làm việc có thể đổi (cùng ngày, khác nhân viên, status SCHEDULED)
         */
        @Query("SELECT sa FROM ShiftAssignmentEntity sa " +
                        "WHERE sa.employeeId != :excludeEmployeeId " +
                        "AND sa.workDate = :workDate " +
                        "AND sa.status = 'SCHEDULED'")
        List<ShiftAssignmentEntity> findAvailableForSwap(
                        @Param("excludeEmployeeId") Long excludeEmployeeId,
                        @Param("workDate") LocalDate workDate);
}
