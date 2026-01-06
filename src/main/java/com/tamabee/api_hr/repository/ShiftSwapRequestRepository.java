package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.attendance.ShiftSwapRequestEntity;
import com.tamabee.api_hr.enums.SwapRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý yêu cầu đổi ca làm việc.
 * Entity này KHÔNG có soft delete - xóa thẳng.
 */
@Repository
public interface ShiftSwapRequestRepository
                extends JpaRepository<ShiftSwapRequestEntity, Long>, JpaSpecificationExecutor<ShiftSwapRequestEntity> {

        /**
         * Lấy danh sách swap requests của công ty theo status (phân trang)
         */
        @Query("SELECT ssr FROM ShiftSwapRequestEntity ssr " +
                        "JOIN ShiftAssignmentEntity sa ON ssr.requesterAssignmentId = sa.id " +
                        "WHERE sa.companyId = :companyId " +
                        "AND ssr.status = :status " +
                        "ORDER BY ssr.createdAt DESC")
        Page<ShiftSwapRequestEntity> findByCompanyIdAndStatus(
                        @Param("companyId") Long companyId,
                        @Param("status") SwapRequestStatus status,
                        Pageable pageable);

        /**
         * Lấy danh sách swap requests của công ty (tất cả status, phân trang)
         */
        @Query("SELECT ssr FROM ShiftSwapRequestEntity ssr " +
                        "JOIN ShiftAssignmentEntity sa ON ssr.requesterAssignmentId = sa.id " +
                        "WHERE sa.companyId = :companyId " +
                        "ORDER BY ssr.createdAt DESC")
        Page<ShiftSwapRequestEntity> findByCompanyId(
                        @Param("companyId") Long companyId,
                        Pageable pageable);

        /**
         * Lấy danh sách swap requests của requester
         */
        List<ShiftSwapRequestEntity> findByRequesterId(Long requesterId);

        /**
         * Lấy danh sách swap requests của requester theo status
         */
        List<ShiftSwapRequestEntity> findByRequesterIdAndStatus(Long requesterId, SwapRequestStatus status);

        /**
         * Lấy danh sách swap requests của target employee
         */
        List<ShiftSwapRequestEntity> findByTargetEmployeeId(Long targetEmployeeId);

        /**
         * Lấy danh sách swap requests của target employee theo status
         */
        List<ShiftSwapRequestEntity> findByTargetEmployeeIdAndStatus(Long targetEmployeeId, SwapRequestStatus status);

        /**
         * Lấy danh sách swap requests liên quan đến nhân viên (requester hoặc target)
         */
        @Query("SELECT ssr FROM ShiftSwapRequestEntity ssr " +
                        "WHERE ssr.requesterId = :employeeId OR ssr.targetEmployeeId = :employeeId " +
                        "ORDER BY ssr.createdAt DESC")
        List<ShiftSwapRequestEntity> findByEmployeeId(@Param("employeeId") Long employeeId);

        /**
         * Lấy danh sách swap requests pending của công ty
         */
        @Query("SELECT ssr FROM ShiftSwapRequestEntity ssr " +
                        "JOIN ShiftAssignmentEntity sa ON ssr.requesterAssignmentId = sa.id " +
                        "WHERE sa.companyId = :companyId " +
                        "AND ssr.status = 'PENDING' " +
                        "ORDER BY ssr.createdAt ASC")
        List<ShiftSwapRequestEntity> findPendingRequestsByCompanyId(@Param("companyId") Long companyId);

        /**
         * Kiểm tra có swap request pending cho assignment không
         */
        boolean existsByRequesterAssignmentIdAndStatus(Long assignmentId, SwapRequestStatus status);

        /**
         * Kiểm tra có swap request pending giữa 2 assignments không
         */
        @Query("SELECT COUNT(ssr) > 0 FROM ShiftSwapRequestEntity ssr " +
                        "WHERE ssr.status = 'PENDING' " +
                        "AND ((ssr.requesterAssignmentId = :assignment1Id AND ssr.targetAssignmentId = :assignment2Id) "
                        +
                        "OR (ssr.requesterAssignmentId = :assignment2Id AND ssr.targetAssignmentId = :assignment1Id))")
        boolean existsPendingSwapBetweenAssignments(
                        @Param("assignment1Id") Long assignment1Id,
                        @Param("assignment2Id") Long assignment2Id);
}
