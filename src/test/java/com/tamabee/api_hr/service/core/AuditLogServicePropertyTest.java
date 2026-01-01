package com.tamabee.api_hr.service.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamabee.api_hr.dto.response.AuditLogResponse;
import com.tamabee.api_hr.entity.audit.AuditLogEntity;
import com.tamabee.api_hr.enums.AuditAction;
import com.tamabee.api_hr.enums.AuditEntityType;
import com.tamabee.api_hr.repository.AuditLogRepository;
import com.tamabee.api_hr.service.core.impl.AuditLogServiceImpl;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho AuditLogService
 * Feature: attendance-payroll-backend
 * Property 20: Audit Log Creation
 */
public class AuditLogServicePropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Property 20: Audit Log Creation
     * For any modification to attendance or payroll records, an audit log entry
     * SHALL be created with timestamp, user, and change details.
     *
     * Test: Khi log attendance change, audit log phải được tạo với đầy đủ thông tin
     */
    @Property(tries = 100)
    void auditLogCreation_attendanceChange_createsLogWithTimestampUserAndDetails(
            @ForAll("companyIds") Long companyId,
            @ForAll("entityIds") Long entityId,
            @ForAll("userIds") Long userId,
            @ForAll("userNames") String userName,
            @ForAll("auditActions") AuditAction action,
            @ForAll("descriptions") String description) {

        // Setup mock
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        // Capture saved entity
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(invocation -> {
            AuditLogEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        AuditLogServiceImpl service = new AuditLogServiceImpl(auditLogRepository, objectMapper);

        // Tạo before/after values
        Map<String, Object> beforeValue = Map.of(
                "checkIn", "09:00",
                "checkOut", "18:00",
                "workingMinutes", 480);
        Map<String, Object> afterValue = Map.of(
                "checkIn", "08:30",
                "checkOut", "18:30",
                "workingMinutes", 540);

        // Thực hiện log
        service.logAttendanceChange(companyId, entityId, action, userId, userName,
                beforeValue, afterValue, description);

        // Verify: audit log được lưu
        verify(auditLogRepository).save(any(AuditLogEntity.class));

        // Verify: entity được tạo với đầy đủ thông tin
        AuditLogEntity savedEntity = captor.getValue();

        // Verify: companyId được set
        assertEquals(companyId, savedEntity.getCompanyId(),
                "CompanyId must be set correctly");

        // Verify: entityType là ATTENDANCE_RECORD
        assertEquals(AuditEntityType.ATTENDANCE_RECORD, savedEntity.getEntityType(),
                "EntityType must be ATTENDANCE_RECORD for attendance changes");

        // Verify: entityId được set
        assertEquals(entityId, savedEntity.getEntityId(),
                "EntityId must be set correctly");

        // Verify: action được set
        assertEquals(action, savedEntity.getAction(),
                "Action must be set correctly");

        // Verify: userId được set
        assertEquals(userId, savedEntity.getUserId(),
                "UserId must be set correctly");

        // Verify: userName được set
        assertEquals(userName, savedEntity.getUserName(),
                "UserName must be set correctly");

        // Verify: timestamp được set và không null
        assertNotNull(savedEntity.getTimestamp(),
                "Timestamp must be set");

        // Verify: timestamp gần với thời điểm hiện tại (trong vòng 5 giây)
        LocalDateTime now = LocalDateTime.now();
        assertTrue(savedEntity.getTimestamp().isAfter(now.minusSeconds(5)),
                "Timestamp should be recent");
        assertTrue(savedEntity.getTimestamp().isBefore(now.plusSeconds(5)),
                "Timestamp should not be in the future");

        // Verify: beforeValue được serialize và lưu
        assertNotNull(savedEntity.getBeforeValue(),
                "BeforeValue must be stored");
        assertTrue(savedEntity.getBeforeValue().contains("checkIn"),
                "BeforeValue should contain original data");

        // Verify: afterValue được serialize và lưu
        assertNotNull(savedEntity.getAfterValue(),
                "AfterValue must be stored");
        assertTrue(savedEntity.getAfterValue().contains("checkIn"),
                "AfterValue should contain new data");

        // Verify: description được set
        assertEquals(description, savedEntity.getDescription(),
                "Description must be set correctly");
    }

    /**
     * Property 20: Audit Log Creation
     * Test: Khi log payroll change, audit log phải được tạo với đầy đủ thông tin
     */
    @Property(tries = 100)
    void auditLogCreation_payrollChange_createsLogWithTimestampUserAndDetails(
            @ForAll("companyIds") Long companyId,
            @ForAll("entityIds") Long entityId,
            @ForAll("userIds") Long userId,
            @ForAll("userNames") String userName,
            @ForAll("auditActions") AuditAction action,
            @ForAll("descriptions") String description) {

        // Setup mock
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(invocation -> {
            AuditLogEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        AuditLogServiceImpl service = new AuditLogServiceImpl(auditLogRepository, objectMapper);

        // Tạo before/after values cho payroll
        Map<String, Object> beforeValue = Map.of(
                "baseSalary", 5000000,
                "netSalary", 4500000,
                "status", "DRAFT");
        Map<String, Object> afterValue = Map.of(
                "baseSalary", 5000000,
                "netSalary", 4500000,
                "status", "FINALIZED");

        // Thực hiện log
        service.logPayrollChange(companyId, entityId, action, userId, userName,
                beforeValue, afterValue, description);

        // Verify
        verify(auditLogRepository).save(any(AuditLogEntity.class));

        AuditLogEntity savedEntity = captor.getValue();

        // Verify: entityType là PAYROLL_RECORD
        assertEquals(AuditEntityType.PAYROLL_RECORD, savedEntity.getEntityType(),
                "EntityType must be PAYROLL_RECORD for payroll changes");

        // Verify: các trường bắt buộc
        assertEquals(companyId, savedEntity.getCompanyId());
        assertEquals(entityId, savedEntity.getEntityId());
        assertEquals(action, savedEntity.getAction());
        assertEquals(userId, savedEntity.getUserId());
        assertEquals(userName, savedEntity.getUserName());
        assertNotNull(savedEntity.getTimestamp());
        assertNotNull(savedEntity.getBeforeValue());
        assertNotNull(savedEntity.getAfterValue());
    }

    /**
     * Property 20: Audit Log Creation
     * Test: Khi log settings change, audit log phải được tạo với đầy đủ thông tin
     */
    @Property(tries = 100)
    void auditLogCreation_settingsChange_createsLogWithTimestampUserAndDetails(
            @ForAll("companyIds") Long companyId,
            @ForAll("entityIds") Long entityId,
            @ForAll("userIds") Long userId,
            @ForAll("userNames") String userName,
            @ForAll("descriptions") String description) {

        // Setup mock
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(invocation -> {
            AuditLogEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        AuditLogServiceImpl service = new AuditLogServiceImpl(auditLogRepository, objectMapper);

        // Tạo before/after values cho settings
        Map<String, Object> beforeValue = Map.of(
                "lateGraceMinutes", 5,
                "enableRounding", false);
        Map<String, Object> afterValue = Map.of(
                "lateGraceMinutes", 10,
                "enableRounding", true);

        // Thực hiện log
        service.logSettingsChange(companyId, entityId, AuditAction.UPDATE, userId, userName,
                beforeValue, afterValue, description);

        // Verify
        verify(auditLogRepository).save(any(AuditLogEntity.class));

        AuditLogEntity savedEntity = captor.getValue();

        // Verify: entityType là COMPANY_SETTINGS
        assertEquals(AuditEntityType.COMPANY_SETTINGS, savedEntity.getEntityType(),
                "EntityType must be COMPANY_SETTINGS for settings changes");

        // Verify: action là UPDATE
        assertEquals(AuditAction.UPDATE, savedEntity.getAction(),
                "Action must be UPDATE for settings changes");

        // Verify: before/after values được lưu
        assertNotNull(savedEntity.getBeforeValue(),
                "BeforeValue must be stored for settings changes");
        assertNotNull(savedEntity.getAfterValue(),
                "AfterValue must be stored for settings changes");
        assertTrue(savedEntity.getBeforeValue().contains("lateGraceMinutes"),
                "BeforeValue should contain settings data");
        assertTrue(savedEntity.getAfterValue().contains("lateGraceMinutes"),
                "AfterValue should contain settings data");
    }

    /**
     * Property 20: Audit Log Creation
     * Test: Before/after values phải được serialize đúng cách
     */
    @Property(tries = 100)
    void auditLogCreation_beforeAfterValues_areSerializedCorrectly(
            @ForAll("companyIds") Long companyId,
            @ForAll("entityIds") Long entityId,
            @ForAll("userIds") Long userId,
            @ForAll("userNames") String userName) {

        // Setup mock
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(invocation -> {
            AuditLogEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        AuditLogServiceImpl service = new AuditLogServiceImpl(auditLogRepository, objectMapper);

        // Test với null beforeValue (CREATE action)
        service.logAttendanceChange(companyId, entityId, AuditAction.CREATE, userId, userName,
                null, Map.of("status", "PRESENT"), "Created new record");

        AuditLogEntity savedEntity = captor.getValue();
        assertNull(savedEntity.getBeforeValue(),
                "BeforeValue should be null for CREATE action");
        assertNotNull(savedEntity.getAfterValue(),
                "AfterValue should not be null for CREATE action");

        // Reset captor
        reset(auditLogRepository);
        when(auditLogRepository.save(captor.capture())).thenAnswer(invocation -> {
            AuditLogEntity entity = invocation.getArgument(0);
            entity.setId(2L);
            return entity;
        });

        // Test với null afterValue (DELETE action)
        service.logAttendanceChange(companyId, entityId, AuditAction.DELETE, userId, userName,
                Map.of("status", "PRESENT"), null, "Deleted record");

        savedEntity = captor.getValue();
        assertNotNull(savedEntity.getBeforeValue(),
                "BeforeValue should not be null for DELETE action");
        assertNull(savedEntity.getAfterValue(),
                "AfterValue should be null for DELETE action");
    }

    /**
     * Property 20: Audit Log Creation
     * Test: Query audit logs theo entity type và entity ID
     */
    @Property(tries = 100)
    void auditLogQuery_byEntityTypeAndId_returnsCorrectHistory(
            @ForAll("entityIds") Long entityId,
            @ForAll("auditEntityTypes") AuditEntityType entityType) {

        // Setup mock
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

        // Tạo mock audit logs
        AuditLogEntity log1 = createMockAuditLog(1L, 1L, entityType, entityId, AuditAction.CREATE);
        AuditLogEntity log2 = createMockAuditLog(2L, 1L, entityType, entityId, AuditAction.UPDATE);

        when(auditLogRepository.findByDeletedFalseAndEntityTypeAndEntityIdOrderByTimestampDesc(
                entityType, entityId))
                .thenReturn(List.of(log2, log1));

        AuditLogServiceImpl service = new AuditLogServiceImpl(auditLogRepository, objectMapper);

        // Query history
        List<AuditLogResponse> history = service.getEntityHistory(entityType, entityId);

        // Verify
        assertEquals(2, history.size(),
                "Should return all audit logs for the entity");
        assertEquals(entityType, history.get(0).getEntityType(),
                "EntityType should match");
        assertEquals(entityId, history.get(0).getEntityId(),
                "EntityId should match");
    }

    // === Helper methods ===

    private AuditLogEntity createMockAuditLog(Long id, Long companyId, AuditEntityType entityType,
            Long entityId, AuditAction action) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(id);
        entity.setCompanyId(companyId);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setAction(action);
        entity.setUserId(1L);
        entity.setUserName("Test User");
        entity.setTimestamp(LocalDateTime.now());
        entity.setDescription("Test description");
        return entity;
    }

    // === Generators ===

    @Provide
    Arbitrary<Long> companyIds() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    @Provide
    Arbitrary<Long> entityIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    @Provide
    Arbitrary<Long> userIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<String> userNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(50)
                .map(s -> "User " + s);
    }

    @Provide
    Arbitrary<AuditAction> auditActions() {
        return Arbitraries.of(AuditAction.CREATE, AuditAction.UPDATE, AuditAction.DELETE,
                AuditAction.APPROVE, AuditAction.REJECT, AuditAction.FINALIZE, AuditAction.PAYMENT);
    }

    @Provide
    Arbitrary<AuditEntityType> auditEntityTypes() {
        return Arbitraries.of(AuditEntityType.ATTENDANCE_RECORD, AuditEntityType.PAYROLL_RECORD,
                AuditEntityType.COMPANY_SETTINGS, AuditEntityType.WORK_SCHEDULE,
                AuditEntityType.LEAVE_REQUEST, AuditEntityType.ADJUSTMENT_REQUEST,
                AuditEntityType.SCHEDULE_SELECTION, AuditEntityType.HOLIDAY);
    }

    @Provide
    Arbitrary<String> descriptions() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(10)
                .ofMaxLength(200)
                .map(s -> "Description: " + s);
    }
}
