package com.tamabee.api_hr.repository;

import com.tamabee.api_hr.entity.audit.AuditLogEntity;
import com.tamabee.api_hr.enums.AuditAction;
import com.tamabee.api_hr.enums.AuditEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho AuditLogEntity.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    // Tìm audit log theo ID
    Optional<AuditLogEntity> findByIdAndDeletedFalse(Long id);

    // Tìm audit logs theo company
    Page<AuditLogEntity> findByDeletedFalseAndCompanyIdOrderByTimestampDesc(
            Long companyId, Pageable pageable);

    // Tìm audit logs theo entity type và entity ID
    List<AuditLogEntity> findByDeletedFalseAndEntityTypeAndEntityIdOrderByTimestampDesc(
            AuditEntityType entityType, Long entityId);

    // Tìm audit logs theo company và entity type
    Page<AuditLogEntity> findByDeletedFalseAndCompanyIdAndEntityTypeOrderByTimestampDesc(
            Long companyId, AuditEntityType entityType, Pageable pageable);

    // Tìm audit logs theo company và action
    Page<AuditLogEntity> findByDeletedFalseAndCompanyIdAndActionOrderByTimestampDesc(
            Long companyId, AuditAction action, Pageable pageable);

    // Tìm audit logs theo user
    Page<AuditLogEntity> findByDeletedFalseAndUserIdOrderByTimestampDesc(
            Long userId, Pageable pageable);

    // Tìm audit logs theo khoảng thời gian
    @Query("SELECT a FROM AuditLogEntity a WHERE a.deleted = false " +
            "AND a.companyId = :companyId " +
            "AND a.timestamp BETWEEN :startTime AND :endTime " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLogEntity> findByCompanyIdAndTimestampBetween(
            @Param("companyId") Long companyId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    // Tìm audit logs với nhiều filter
    @Query("SELECT a FROM AuditLogEntity a WHERE a.deleted = false " +
            "AND a.companyId = :companyId " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:userId IS NULL OR a.userId = :userId) " +
            "AND (:startTime IS NULL OR a.timestamp >= :startTime) " +
            "AND (:endTime IS NULL OR a.timestamp <= :endTime) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLogEntity> findByFilters(
            @Param("companyId") Long companyId,
            @Param("entityType") AuditEntityType entityType,
            @Param("action") AuditAction action,
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    // Đếm số audit logs theo entity
    long countByDeletedFalseAndEntityTypeAndEntityId(AuditEntityType entityType, Long entityId);
}
