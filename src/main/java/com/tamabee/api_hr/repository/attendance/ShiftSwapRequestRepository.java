package com.tamabee.api_hr.repository.attendance;

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
 */
@Repository
public interface ShiftSwapRequestRepository
                extends JpaRepository<ShiftSwapRequestEntity, Long>, JpaSpecificationExecutor<ShiftSwapRequestEntity> {

        /**
         * Lấy danh sách swap requests theo status (phân trang)
         */
        Page<ShiftSwapRequestEntity> findByStatus(SwapRequestStatus status, Pageable pageable);

        /**
         * Lấy tất cả swap requests (phân trang)
         */
        @Query("SELECT ssr FROM ShiftSwapRequestEntity ssr ORDER BY ssr.createdAt DESC")
        Page<ShiftSwapRequestEntity> findAllPaged(Pageable pageable);

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
         * Lấy danh sách swap requests pending
         */
        @Query("SELECT ssr FROM ShiftSwapRequestEntity ssr " +
                        "WHERE ssr.status = 'PENDING' " +
                        "ORDER BY ssr.createdAt ASC")
        List<ShiftSwapRequestEntity> findPendingRequests();

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
