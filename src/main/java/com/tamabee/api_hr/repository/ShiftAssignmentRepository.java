package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.attendance.ShiftAssignmentEntity;
import com.tamabee.api_hr.enums.ShiftAssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository quản lý phân ca làm việc cho nhân viên.
 * Entity này KHÔNG có soft delete - xóa thẳng.
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
         * Lấy danh sách shift assignments của công ty (phân trang)
         */
        Page<ShiftAssignmentEntity> findByCompanyId(Long companyId, Pageable pageable);

        /**
         * Lấy danh sách shift assignments của công ty theo ngày
         */
        List<ShiftAssignmentEntity> findByCompanyIdAndWorkDate(Long companyId, LocalDate workDate);

        /**
         * Lấy danh sách shift assignments của công ty trong khoảng thời gian
         */
        Page<ShiftAssignmentEntity> findByCompanyIdAndWorkDateBetween(
                        Long companyId, LocalDate startDate, LocalDate endDate, Pageable pageable);

        /**
         * Lấy danh sách shift assignments theo status
         */
        Page<ShiftAssignmentEntity> findByCompanyIdAndStatus(
                        Long companyId, ShiftAssignmentStatus status, Pageable pageable);

        /**
         * Kiểm tra nhân viên có shift assignment vào ngày cụ thể không
         */
        boolean existsByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

        /**
         * Kiểm tra overlap shift assignments cho nhân viên trong ngày
         * (dùng để validate không có 2 ca trùng nhau)
         */
        @Query("SELECT COUNT(sa) > 0 FROM ShiftAssignmentEntity sa " +
                        "JOIN ShiftTemplateEntity st ON sa.shiftTemplateId = st.id " +
                        "WHERE sa.employeeId = :employeeId " +
                        "AND sa.workDate = :workDate " +
                        "AND sa.id != :excludeId " +
                        "AND sa.status != 'CANCELLED'")
        boolean existsOverlappingAssignment(
                        @Param("employeeId") Long employeeId,
                        @Param("workDate") LocalDate workDate,
                        @Param("excludeId") Long excludeId);

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
         * Tìm các ca làm việc có thể đổi (cùng công ty, cùng ngày, khác nhân viên,
         * status SCHEDULED)
         */
        @Query("SELECT sa FROM ShiftAssignmentEntity sa " +
                        "WHERE sa.companyId = :companyId " +
                        "AND sa.employeeId != :excludeEmployeeId " +
                        "AND sa.workDate = :workDate " +
                        "AND sa.status = 'SCHEDULED'")
        List<ShiftAssignmentEntity> findAvailableForSwap(
                        @Param("companyId") Long companyId,
                        @Param("excludeEmployeeId") Long excludeEmployeeId,
                        @Param("workDate") LocalDate workDate);
}
