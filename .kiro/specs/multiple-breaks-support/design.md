# Design Document: Multiple Breaks Support

## Overview

Mở rộng hệ thống Break Time Management để hỗ trợ nhiều lần nghỉ giải lao trong 1 ngày làm việc. Thiết kế này bao gồm:

- Thêm `breakNumber` vào BreakRecordEntity để đánh số thứ tự breaks
- Cập nhật CreateAdjustmentRequest để hỗ trợ `breakRecordId`
- Validation cho overlapping breaks và maximum breaks per day
- Cập nhật responses để trả về danh sách breaks

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Controller Layer                          │
│  EmployeeBreakController  │  AttendanceAdjustmentController     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Service Layer                             │
│  BreakServiceImpl  │  AttendanceAdjustmentServiceImpl           │
│  - startBreak() validates no active break                        │
│  - validates no overlap with existing breaks                     │
│  - assigns breakNumber sequentially                              │
│  - enforces maxBreaksPerDay from CompanySettings                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Repository Layer                          │
│  BreakRecordRepository                                           │
│  - findByAttendanceRecordIdAndDeletedFalse() returns List        │
│  - findActiveBreak() for checking active breaks                  │
│  - countByAttendanceRecordId() for max breaks check              │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. BreakRecordEntity Changes

```java
@Entity
@Table(name = "break_records")
public class BreakRecordEntity extends BaseEntity {
    // Existing fields...

    // NEW: Số thứ tự break trong ngày (1, 2, 3, ...)
    @Column(nullable = false)
    private Integer breakNumber;
}
```

### 2. CreateAdjustmentRequest Changes

```java
public class CreateAdjustmentRequest {
    // Existing fields...

    // NEW: ID của break record cần điều chỉnh (bắt buộc khi điều chỉnh break)
    private Long breakRecordId;
}
```

### 3. AdjustmentRequestResponse Changes

```java
public class AdjustmentRequestResponse {
    // Existing fields...

    // NEW: ID của break record được điều chỉnh
    private Long breakRecordId;

    // NEW: Số thứ tự break được điều chỉnh
    private Integer breakNumber;
}
```

### 4. AttendanceAdjustmentRequestEntity Changes

```java
@Entity
@Table(name = "attendance_adjustment_requests")
public class AttendanceAdjustmentRequestEntity extends BaseEntity {
    // Existing fields...

    // NEW: ID của break record cần điều chỉnh
    private Long breakRecordId;
}
```

### 5. DailyBreakReportResponse Changes

```java
public class DailyBreakReportResponse {
    // Existing fields...

    // NEW: Danh sách các break sessions
    private List<BreakSessionInfo> breakSessions;

    @Data
    public static class BreakSessionInfo {
        private Long breakRecordId;
        private Integer breakNumber;
        private LocalDateTime breakStart;
        private LocalDateTime breakEnd;
        private Integer durationMinutes;
    }
}
```

### 6. CompanySettings Changes

```java
// Trong breakConfig JSON
{
    "minBreakMinutes": 45,
    "maxBreakMinutes": 90,
    "defaultBreakMinutes": 60,
    "autoDeductBreak": true,
    "maxBreaksPerDay": 3  // NEW: Giới hạn số breaks/ngày
}
```

### 7. BreakServiceImpl Changes

```java
public interface IBreakService {
    // Existing methods...

    // Cập nhật startBreak để validate và assign breakNumber
    BreakRecordResponse startBreak(Long employeeId, Long companyId);

    // NEW: Lấy tất cả breaks của 1 attendance record
    List<BreakRecordResponse> getBreaksByAttendanceRecord(Long attendanceRecordId);

    // NEW: Tính tổng thời gian break
    int calculateTotalBreakMinutes(Long attendanceRecordId);
}
```

### 8. AttendanceAdjustmentServiceImpl Changes

```java
public AdjustmentRequestResponse createAdjustmentRequest(
        Long employeeId, Long companyId, CreateAdjustmentRequest request) {

    // Validate: nếu có break fields thì phải có breakRecordId
    if (hasBreakFields(request) && request.getBreakRecordId() == null) {
        throw new BadRequestException(
            "Must specify breakRecordId when adjusting break times",
            ErrorCode.BREAK_RECORD_ID_REQUIRED);
    }

    // Nếu có breakRecordId, validate break record tồn tại
    if (request.getBreakRecordId() != null) {
        BreakRecordEntity breakRecord = findBreakRecord(request.getBreakRecordId());
        // Validate break thuộc về attendance record này
        if (!breakRecord.getAttendanceRecordId().equals(request.getAttendanceRecordId())) {
            throw new BadRequestException(
                "Break record does not belong to this attendance record",
                ErrorCode.INVALID_BREAK_RECORD);
        }
    }

    // ... rest of implementation
}
```

## Data Models

### Database Schema Changes

```sql
-- Thêm cột breakNumber vào break_records
ALTER TABLE break_records ADD COLUMN break_number INTEGER NOT NULL DEFAULT 1;

-- Thêm cột breakRecordId vào attendance_adjustment_requests
ALTER TABLE attendance_adjustment_requests ADD COLUMN break_record_id BIGINT;

-- Index cho breakNumber
CREATE INDEX idx_break_records_break_number ON break_records(attendance_record_id, break_number);
```

### Break Validation Flow

```
startBreak(employeeId, companyId)
    │
    ├── Check: Có break đang active không? (breakEnd IS NULL)
    │   └── Nếu có → throw "Cannot start new break while another is active"
    │
    ├── Check: Đã đạt maxBreaksPerDay chưa?
    │   └── Nếu đạt → throw "Maximum breaks per day reached"
    │
    ├── Get next breakNumber = count(existing breaks) + 1
    │
    └── Create new BreakRecordEntity với breakNumber
```

### Adjustment Validation Flow

```
createAdjustmentRequest(request)
    │
    ├── Check: Có break fields không?
    │   └── Nếu có và breakRecordId == null → throw "Must specify breakRecordId"
    │
    ├── Check: breakRecordId có tồn tại không?
    │   └── Nếu không → throw "Break record not found"
    │
    ├── Check: breakRecord thuộc về attendanceRecord không?
    │   └── Nếu không → throw "Invalid break record"
    │
    └── Create adjustment request với breakRecordId
```

## Correctness Properties

_A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees._

### Property 1: Sequential Break Number Assignment

_For any_ attendance record with N existing breaks, when a new break is started, the new break SHALL have breakNumber = N + 1.

**Validates: Requirements 1.1, 1.3**

### Property 2: Total Break Duration Calculation

_For any_ attendance record with multiple break records, the total break duration SHALL equal the sum of all individual break durations (actualBreakMinutes).

**Validates: Requirements 1.2, 4.3**

### Property 3: Breaks Ordered by Break Number

_For any_ query returning break records for an attendance record, the results SHALL be ordered by breakNumber in ascending order.

**Validates: Requirements 1.4, 5.2**

### Property 4: Specific Break Adjustment

_For any_ adjustment request with a valid breakRecordId, when approved, only that specific break record SHALL be updated, and all other break records SHALL remain unchanged.

**Validates: Requirements 2.2, 2.4**

### Property 5: Break Record ID Required for Break Adjustment

_For any_ adjustment request that includes break time fields (requestedBreakStart or requestedBreakEnd) but has null breakRecordId, the system SHALL reject the request with error code BREAK_RECORD_ID_REQUIRED.

**Validates: Requirements 2.3**

### Property 6: No Concurrent Active Breaks

_For any_ attempt to start a new break while another break is active (breakEnd is null), the system SHALL reject the request with appropriate error.

**Validates: Requirements 3.1**

### Property 7: No Overlapping Break Times

_For any_ new break with start time T, if there exists a completed break where breakStart <= T <= breakEnd, the system SHALL reject the request.

**Validates: Requirements 3.2, 3.3**

### Property 8: Maximum Breaks Per Day Enforcement

_For any_ attendance record that has reached maxBreaksPerDay (from CompanySettings), attempting to start a new break SHALL be rejected.

**Validates: Requirements 3.4**

### Property 9: Payroll Uses Aggregated Break Time

_For any_ payroll calculation, the break deduction SHALL use the sum of all break durations for the attendance record, not just the first break.

**Validates: Requirements 4.4**

## Error Handling

| Error Code               | Message                                                | Condition                                      |
| ------------------------ | ------------------------------------------------------ | ---------------------------------------------- |
| BREAK_RECORD_ID_REQUIRED | Must specify breakRecordId when adjusting break times  | Break fields provided without breakRecordId    |
| INVALID_BREAK_RECORD     | Break record does not belong to this attendance record | breakRecordId doesn't match attendanceRecordId |
| BREAK_ALREADY_ACTIVE     | Cannot start new break while another is active         | Attempting to start break with active break    |
| BREAK_OVERLAP            | Break times cannot overlap with existing breaks        | New break overlaps with existing               |
| MAX_BREAKS_REACHED       | Maximum breaks per day reached                         | Exceeded maxBreaksPerDay limit                 |

## Testing Strategy

### Unit Tests

- Test breakNumber assignment logic
- Test validation for breakRecordId requirement
- Test overlap detection algorithm
- Test max breaks per day check

### Property-Based Tests (jqwik)

- Property 1: Generate random attendance records, add breaks, verify sequential numbering
- Property 2: Generate multiple breaks with random durations, verify sum calculation
- Property 3: Generate breaks in random order, verify sorted output
- Property 4: Generate adjustment requests, verify only specified break is modified
- Property 5: Generate requests with break fields but no breakRecordId, verify rejection
- Property 6: Generate scenarios with active breaks, verify rejection of new breaks
- Property 7: Generate overlapping break times, verify rejection
- Property 8: Generate scenarios at max breaks limit, verify rejection
- Property 9: Generate payroll calculations with multiple breaks, verify aggregation

### Integration Tests

- End-to-end flow: start multiple breaks, adjust specific break, verify reports
- Concurrent break start attempts
- Adjustment approval flow with breakRecordId
