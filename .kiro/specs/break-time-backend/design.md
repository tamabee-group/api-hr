# Design Document: Break Time Management

## Overview

Hệ thống quản lý giờ giải lao được thiết kế để tích hợp với hệ thống Attendance & Payroll hiện có. Break time configuration được lưu trong CompanySettings và ảnh hưởng đến cách tính working hours và payroll.

### Design Principles

1. **Integration with Existing System**: Tích hợp với CompanySettings, WorkSchedule, AttendanceRecord
2. **Legal Compliance**: Đảm bảo tuân thủ quy định pháp luật về giờ giải lao
3. **Flexibility**: Hỗ trợ nhiều chính sách giải lao khác nhau
4. **Audit Trail**: Lưu trữ đầy đủ thông tin để audit

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Controller Layer                         │
├─────────────────────────────────────────────────────────────────┤
│  CompanySettingsController (existing)  │  AttendanceController  │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Service Layer                           │
├─────────────────────────────────────────────────────────────────┤
│  ICompanySettingsService  │  IAttendanceService  │  IBreakService  │
└─────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Calculator Modules                        │
├─────────────────────────────────────────────────────────────────┤
│  BreakCalculator  │  WorkingHoursCalculator (updated)  │        │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Repository Layer                         │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Break Service

```java
public interface IBreakService {

    // Break record management
    BreakRecordResponse startBreak(Long employeeId, StartBreakRequest request);
    BreakRecordResponse endBreak(Long employeeId, Long breakRecordId);

    // Query
    List<BreakRecordResponse> getBreakRecordsByAttendance(Long attendanceRecordId);
    BreakSummaryResponse getBreakSummary(Long employeeId, LocalDate date);

    // Validation
    void validateBreakDuration(Long companyId, Integer breakMinutes);
    Integer getLegalMinimumBreak(String locale, Integer workingHours);
    Integer getEffectiveMinimumBreak(Long companyId, Integer workingHours);
}
```

### 2. Break Calculator

```java
public interface IBreakCalculator {

    // Tính tổng thời gian giải lao
    Integer calculateTotalBreakMinutes(List<BreakRecord> breakRecords);

    // Tính thời gian giải lao hiệu lực (sau khi áp dụng min/max)
    Integer calculateEffectiveBreakMinutes(
        Integer actualBreakMinutes,
        BreakConfig config,
        Integer workingHours,
        Boolean isNightShift
    );

    // Tính working hours sau khi trừ break
    Integer calculateNetWorkingMinutes(
        Integer grossWorkingMinutes,
        Integer breakMinutes,
        BreakConfig config
    );

    // Lấy legal minimum break theo locale
    Integer getLegalMinimumBreak(String locale, Integer workingHours, Boolean isNightShift);

    // Kiểm tra xem shift có phải là night shift không
    Boolean isNightShift(LocalTime shiftStart, LocalTime shiftEnd, BreakConfig config);

    // Tính working hours cho shift qua đêm
    Integer calculateWorkingMinutesForOvernightShift(
        LocalDateTime checkIn,
        LocalDateTime checkOut
    );
}
```

### 3. Updated Working Hours Calculator

```java
public interface IWorkingHoursCalculator {

    // Tính working hours có tính đến break
    WorkingHoursResult calculateWorkingHours(
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        List<BreakRecord> breakRecords,
        BreakConfig breakConfig,
        WorkSchedule schedule
    );

    // Tính working hours cho overnight shift (qua đêm)
    WorkingHoursResult calculateOvernightWorkingHours(
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        List<BreakRecord> breakRecords,
        BreakConfig breakConfig,
        WorkSchedule schedule
    );

    // Kiểm tra xem shift có qua đêm không
    Boolean isOvernightShift(LocalTime startTime, LocalTime endTime);
}
```

### 4. Overtime Calculator

```java
public interface IOvertimeCalculator {

    // Tính overtime với break và night shift
    OvertimeResult calculateOvertime(
        Integer netWorkingMinutes,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        List<BreakRecord> breakRecords,
        OvertimeConfig config,
        Boolean isHoliday
    );

    // Tính số phút làm trong giờ đêm (sau khi trừ break)
    Integer calculateNightMinutes(
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        List<BreakRecord> breakRecords,
        OvertimeConfig config
    );

    // Lấy legal minimum multipliers theo locale
    OvertimeMultipliers getLegalMinimumMultipliers(String locale);

    // Validate custom multipliers không thấp hơn legal minimum
    void validateMultipliers(OvertimeConfig config);
}

@Data
@Builder
public class OvertimeResult {
    private Integer regularMinutes;           // Giờ làm thường (trong 8h)
    private Integer regularOvertimeMinutes;   // Tăng ca thường (>8h, không phải đêm)
    private Integer nightMinutes;             // Giờ làm đêm (trong 8h)
    private Integer nightOvertimeMinutes;     // Tăng ca đêm (>8h, trong giờ đêm)
    private Integer holidayMinutes;           // Giờ làm ngày lễ
    private Integer holidayNightMinutes;      // Giờ làm đêm ngày lễ

    private BigDecimal regularOvertimeAmount;
    private BigDecimal nightWorkAmount;
    private BigDecimal nightOvertimeAmount;
    private BigDecimal holidayOvertimeAmount;
    private BigDecimal holidayNightOvertimeAmount;
    private BigDecimal totalOvertimeAmount;
}

@Data
@Builder
public class OvertimeMultipliers {
    private BigDecimal regularOvertime;       // Tăng ca thường
    private BigDecimal nightWork;             // Làm đêm
    private BigDecimal nightOvertime;         // Tăng ca đêm
    private BigDecimal holidayOvertime;       // Tăng ca ngày lễ
    private BigDecimal holidayNightOvertime;  // Tăng ca đêm ngày lễ
}
```

## Data Models

### Entity Relationship Diagram

```
┌─────────────────────┐       ┌─────────────────────┐
│ CompanySettingsEntity│       │   BreakConfig       │
│                     │──JSON──│   (in JSON)         │
└─────────────────────┘       └─────────────────────┘

┌─────────────────────┐       ┌─────────────────────┐
│ AttendanceRecordEntity│──1:N──│  BreakRecordEntity  │
└─────────────────────┘       └─────────────────────┘

┌─────────────────────┐       ┌─────────────────────┐
│ WorkScheduleEntity  │       │   BreakPeriod       │
│                     │──JSON──│   (in scheduleData) │
└─────────────────────┘       └─────────────────────┘
```

### New Entity: BreakRecordEntity

```java
@Entity
@Table(name = "break_records")
public class BreakRecordEntity extends BaseEntity {

    @Column(nullable = false)
    private Long attendanceRecordId;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private LocalDate workDate;

    // Thời gian giải lao
    private LocalDateTime breakStart;
    private LocalDateTime breakEnd;

    // Thời gian tính toán
    private Integer actualBreakMinutes;      // Thời gian thực tế
    private Integer effectiveBreakMinutes;   // Thời gian sau khi áp dụng min/max

    // Audit
    private String notes;
}
```

### Updated Configuration DTOs

```java
@Data
@Builder
public class BreakConfig {

    // Bật/tắt giờ giải lao
    private Boolean breakEnabled = true;

    // Loại giải lao: PAID hoặc UNPAID
    private BreakType breakType = BreakType.UNPAID;

    // Thời gian giải lao mặc định (phút) - dùng khi không tracking
    private Integer defaultBreakMinutes = 60;

    // Thời gian giải lao tối thiểu (phút)
    private Integer minimumBreakMinutes = 45;

    // Thời gian giải lao tối đa (phút)
    private Integer maximumBreakMinutes = 90;

    // Sử dụng legal minimum hay custom
    private Boolean useLegalMinimum = true;

    // Bật/tắt tracking giờ giải lao
    private Boolean breakTrackingEnabled = false;

    // Locale cho legal requirements (vi, ja, en)
    private String locale = "ja";

    // Fixed break mode - tự động áp dụng break mà không cần tracking
    private Boolean fixedBreakMode = false;

    // Số lần giải lao trong 1 lần chấm công (1, 2, 3...)
    private Integer breakPeriodsPerAttendance = 1;

    // Danh sách các khoảng giải lao cố định (dùng khi fixedBreakMode = true)
    private List<BreakPeriod> fixedBreakPeriods = new ArrayList<>();

    // Night shift configuration
    private LocalTime nightShiftStartTime = LocalTime.of(22, 0);  // 22:00
    private LocalTime nightShiftEndTime = LocalTime.of(5, 0);     // 05:00

    // Night shift break requirements (có thể khác với day shift)
    private Integer nightShiftMinimumBreakMinutes = 45;
    private Integer nightShiftDefaultBreakMinutes = 60;
}

@Data
@Builder
public class OvertimeConfig {

    // Bật/tắt tính overtime
    private Boolean overtimeEnabled = true;

    // Số giờ làm việc tiêu chuẩn (mặc định 8 tiếng)
    private Integer standardWorkingHours = 8;

    // Giờ bắt đầu/kết thúc ca đêm
    private LocalTime nightStartTime = LocalTime.of(22, 0);  // 22:00
    private LocalTime nightEndTime = LocalTime.of(5, 0);     // 05:00

    // Overtime multipliers - có thể cấu hình linh hoạt
    private BigDecimal regularOvertimeRate = new BigDecimal("1.25");      // Tăng ca thường
    private BigDecimal nightWorkRate = new BigDecimal("1.25");            // Làm đêm (không tăng ca)
    private BigDecimal nightOvertimeRate = new BigDecimal("1.50");        // Tăng ca đêm
    private BigDecimal holidayOvertimeRate = new BigDecimal("1.35");      // Tăng ca ngày lễ
    private BigDecimal holidayNightOvertimeRate = new BigDecimal("1.60"); // Tăng ca đêm ngày lễ

    // Sử dụng legal minimum hay custom
    private Boolean useLegalMinimum = true;

    // Locale cho legal requirements (vi, ja)
    private String locale = "ja";
}

@Data
@Builder
public class BreakPeriod {
    private String name;             // Tên break period (e.g., "Morning Break", "Lunch", "Afternoon Break")
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private Boolean isFlexible;      // true = employee chọn thời điểm
    private Integer order;           // Thứ tự break trong ngày (1, 2, 3...)
}

// Thêm vào WorkScheduleData
@Data
@Builder
public class WorkScheduleData {
    // ... existing fields ...

    // Break periods
    private List<BreakPeriod> breakPeriods = new ArrayList<>();
    private Integer totalBreakMinutes;
}
```

### Enums

```java
public enum BreakType {
    PAID,    // Giải lao có lương
    UNPAID   // Giải lao không lương
}
```

### Updated AttendanceRecordEntity

```java
@Entity
@Table(name = "attendance_records")
public class AttendanceRecordEntity extends BaseEntity {
    // ... existing fields ...

    // Break time fields
    private Integer totalBreakMinutes;       // Tổng thời gian giải lao
    private Integer effectiveBreakMinutes;   // Thời gian giải lao hiệu lực

    @Enumerated(EnumType.STRING)
    private BreakType breakType;             // Loại giải lao áp dụng

    private Boolean breakCompliant;          // Có tuân thủ minimum break không
}
```

### Updated PayrollRecordEntity

```java
@Entity
@Table(name = "payroll_records")
public class PayrollRecordEntity extends BaseEntity {
    // ... existing fields ...

    // Break time tracking
    private Integer totalBreakMinutes;       // Tổng giờ giải lao trong kỳ

    @Enumerated(EnumType.STRING)
    private BreakType breakType;             // Loại giải lao áp dụng

    private BigDecimal breakDeductionAmount; // Số tiền khấu trừ do giải lao (nếu unpaid)
}
```

## Legal Break Requirements

### Japanese Labor Law (労働基準法)

```java
public class JapaneseBreakRequirements {

    public static Integer getMinimumBreak(Integer workingHours) {
        if (workingHours <= 6) {
            return 0;  // Không bắt buộc
        } else if (workingHours <= 8) {
            return 45; // Tối thiểu 45 phút
        } else {
            return 60; // Tối thiểu 60 phút
        }
    }
}
```

### Vietnamese Labor Law (Bộ luật Lao động)

```java
public class VietnameseBreakRequirements {

    public static Integer getMinimumBreak(Integer workingHours, Boolean isNightShift) {
        if (workingHours <= 6) {
            return 0;  // Không bắt buộc
        }

        if (isNightShift) {
            return 45; // Ca đêm: tối thiểu 45 phút
        }

        return 30; // Ca ngày: tối thiểu 30 phút
    }
}
```

### Default/Other Locales

```java
public class DefaultBreakRequirements {

    public static Integer getMinimumBreak(Integer workingHours) {
        if (workingHours <= 6) {
            return 0;
        }
        return 30; // Mặc định 30 phút
    }
}
```

## Legal Overtime Requirements

### Japanese Labor Law (労働基準法)

```java
public class JapaneseOvertimeRequirements {

    public static OvertimeMultipliers getMinimumMultipliers() {
        return OvertimeMultipliers.builder()
            .regularOvertime(new BigDecimal("1.25"))      // 25% tăng thêm
            .nightWork(new BigDecimal("1.25"))            // 25% tăng thêm (22:00-05:00)
            .nightOvertime(new BigDecimal("1.50"))        // 25% + 25% = 50% tăng thêm
            .holidayOvertime(new BigDecimal("1.35"))      // 35% tăng thêm
            .holidayNightOvertime(new BigDecimal("1.60")) // 35% + 25% = 60% tăng thêm
            .build();
    }
}
```

### Vietnamese Labor Law (Bộ luật Lao động)

```java
public class VietnameseOvertimeRequirements {

    public static OvertimeMultipliers getMinimumMultipliers() {
        return OvertimeMultipliers.builder()
            .regularOvertime(new BigDecimal("1.50"))      // 150% lương
            .nightWork(new BigDecimal("1.30"))            // 130% lương (22:00-06:00)
            .nightOvertime(new BigDecimal("1.95"))        // 150% * 130% = 195%
            .holidayOvertime(new BigDecimal("2.00"))      // 200% lương (ngày lễ)
            .holidayNightOvertime(new BigDecimal("2.60")) // 200% * 130% = 260%
            .build();
    }
}
```

### Default/Other Locales

```java
public class DefaultOvertimeRequirements {

    public static OvertimeMultipliers getMinimumMultipliers() {
        return OvertimeMultipliers.builder()
            .regularOvertime(new BigDecimal("1.25"))
            .nightWork(new BigDecimal("1.25"))
            .nightOvertime(new BigDecimal("1.50"))
            .holidayOvertime(new BigDecimal("1.50"))
            .holidayNightOvertime(new BigDecimal("1.75"))
            .build();
    }
}
```

## Correctness Properties

### Property 1: Break Duration Non-Negative

_For any_ break record, the break duration SHALL be non-negative.

**Validates: Requirements 7.2**

### Property 2: Break Time Within Working Hours

_For any_ fixed break period in a work schedule, the break start and end times SHALL fall within the working hours.

**Validates: Requirements 3.5**

### Property 3: Minimum Break Enforcement

_For any_ break configuration, the minimum break duration SHALL NOT exceed the maximum break duration.

**Validates: Requirements 7.3**

### Property 4: Legal Minimum Compliance

_For any_ break configuration with useLegalMinimum=true, the effective minimum break SHALL be at least the legal minimum for the company's locale.

**Validates: Requirements 2.5, 7.4**

### Property 5: Working Hours Calculation with Unpaid Break

_For any_ attendance record with unpaid break, the net working hours SHALL equal (gross working hours - effective break duration).

**Validates: Requirements 5.1**

### Property 6: Working Hours Calculation with Paid Break

_For any_ attendance record with paid break, the net working hours SHALL equal gross working hours (no deduction).

**Validates: Requirements 5.2**

### Property 7: Break Deduction Consistency

_For any_ payroll calculation with unpaid breaks, the break deduction SHALL be consistent with the break policy and recorded break duration.

**Validates: Requirements 6.1, 6.2**

### Property 8: Effective Break Capping

_For any_ break record where actual break exceeds maximum, the effective break SHALL be capped at maximum.

**Validates: Requirements 4.6**

### Property 9: Break Record Audit Trail

_For any_ break record, both the actual break duration and effective break duration SHALL be stored.

**Validates: Requirements 4.7**

### Property 10: Total Break Minutes Invariant

_For any_ attendance record with multiple break records, the total break minutes SHALL equal the sum of individual break durations.

**Validates: Requirements 4.4**

### Property 11: Night Shift Detection

_For any_ work schedule where start time is after end time (e.g., 17:00 to 07:00), the system SHALL correctly identify it as an overnight shift.

**Validates: Requirements 9.1, 9.2**

### Property 12: Overnight Working Hours Calculation

_For any_ overnight shift, the working hours SHALL be calculated correctly across midnight (e.g., 17:00 to 07:00 = 14 hours).

**Validates: Requirements 9.2**

### Property 13: Night Shift Break Requirements

_For any_ shift that falls within night hours (22:00-05:00), the system SHALL apply night shift break requirements.

**Validates: Requirements 9.5, 9.6, 9.8**

### Property 14: Break Period Across Midnight

_For any_ break period that spans midnight (e.g., 23:30 to 00:30), the break duration SHALL be calculated correctly (60 minutes).

**Validates: Requirements 9.7**

### Property 15: Overtime Multiplier Validation

_For any_ overtime configuration, the custom multipliers SHALL NOT be below the legal minimum for the company's locale.

**Validates: Requirements 12.6**

### Property 16: Night Minutes Calculation

_For any_ shift that includes night hours (22:00-05:00), the night minutes SHALL be calculated correctly after deducting breaks that fall within night hours.

**Validates: Requirements 11.5**

### Property 17: Overtime Amount Calculation

_For any_ overtime calculation, the overtime amount SHALL equal (overtime minutes _ hourly rate _ overtime multiplier).

**Validates: Requirements 11.3, 12.10**

### Property 18: Overnight Shift Hour Split

_For any_ overnight shift (e.g., 17:00 to 07:00), the system SHALL correctly split hours into regular, night, and morning segments.

**Validates: Requirements 11.4**

## API Endpoints

### Break Configuration (trong CompanySettingsController)

```
PUT /api/company/settings/break
Request: BreakConfigRequest
Response: BreakConfigResponse
```

### Overtime Configuration (trong CompanySettingsController)

```
PUT /api/company/settings/overtime
Request: OvertimeConfigRequest
Response: OvertimeConfigResponse

GET /api/company/settings/overtime
Response: OvertimeConfigResponse
```

### Break Recording (trong AttendanceController)

```
POST /api/employee/attendance/break/start
Request: StartBreakRequest { notes?: string }
Response: BreakRecordResponse

POST /api/employee/attendance/break/{breakRecordId}/end
Response: BreakRecordResponse

GET /api/employee/attendance/{date}/breaks
Response: List<BreakRecordResponse>
```

### Break Reports (trong ReportController)

```
GET /api/company/reports/break/daily?date={date}
Response: DailyBreakReportResponse

GET /api/company/reports/break/monthly?yearMonth={YYYY-MM}
Response: MonthlyBreakReportResponse
```

## Error Handling

### Error Codes

```java
// Break Configuration errors
INVALID_BREAK_CONFIG("BREAK_001", "Invalid break configuration"),
BREAK_MINIMUM_EXCEEDS_MAXIMUM("BREAK_002", "Minimum break cannot exceed maximum break"),
BREAK_BELOW_LEGAL_MINIMUM("BREAK_003", "Break duration below legal minimum"),

// Break Recording errors
BREAK_ALREADY_STARTED("BREAK_004", "Break already in progress"),
NO_ACTIVE_BREAK("BREAK_005", "No active break to end"),
BREAK_OUTSIDE_WORKING_HOURS("BREAK_006", "Break time outside working hours"),
BREAK_RECORD_NOT_FOUND("BREAK_007", "Break record not found"),

// Break Validation errors
INVALID_BREAK_DURATION("BREAK_008", "Invalid break duration"),
BREAK_START_AFTER_END("BREAK_009", "Break start time must be before end time"),

// Overtime Configuration errors
INVALID_OVERTIME_CONFIG("OT_001", "Invalid overtime configuration"),
OVERTIME_RATE_BELOW_LEGAL_MINIMUM("OT_002", "Overtime rate below legal minimum"),
INVALID_NIGHT_HOURS_CONFIG("OT_003", "Invalid night hours configuration")
```

## Migration Strategy

### Database Migration

```sql
-- V{n}__create_break_records_table.sql
CREATE TABLE IF NOT EXISTS break_records (
    id BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    break_start TIMESTAMP,
    break_end TIMESTAMP,
    actual_break_minutes INTEGER,
    effective_break_minutes INTEGER,
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_break_records_attendance ON break_records(attendance_record_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_break_records_employee_date ON break_records(employee_id, work_date) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_break_records_deleted ON break_records(deleted);

-- V{n}__add_break_fields_to_attendance.sql
ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS total_break_minutes INTEGER;
ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS effective_break_minutes INTEGER;
ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS break_type VARCHAR(20);
ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS break_compliant BOOLEAN;

-- V{n}__add_break_fields_to_payroll.sql
ALTER TABLE payroll_records ADD COLUMN IF NOT EXISTS total_break_minutes INTEGER;
ALTER TABLE payroll_records ADD COLUMN IF NOT EXISTS break_type VARCHAR(20);
ALTER TABLE payroll_records ADD COLUMN IF NOT EXISTS break_deduction_amount DECIMAL(15,2);
```

## Testing Strategy

### Property-Based Tests

```java
@Property
void breakDurationNonNegative(@ForAll @IntRange(min = 0, max = 480) int breakMinutes) {
    assertThat(breakMinutes).isGreaterThanOrEqualTo(0);
}

@Property
void workingHoursWithUnpaidBreak(
    @ForAll @IntRange(min = 60, max = 720) int grossMinutes,
    @ForAll @IntRange(min = 0, max = 120) int breakMinutes
) {
    BreakConfig config = BreakConfig.builder()
        .breakEnabled(true)
        .breakType(BreakType.UNPAID)
        .build();

    int netMinutes = breakCalculator.calculateNetWorkingMinutes(grossMinutes, breakMinutes, config);
    assertThat(netMinutes).isEqualTo(grossMinutes - breakMinutes);
}

@Property
void workingHoursWithPaidBreak(
    @ForAll @IntRange(min = 60, max = 720) int grossMinutes,
    @ForAll @IntRange(min = 0, max = 120) int breakMinutes
) {
    BreakConfig config = BreakConfig.builder()
        .breakEnabled(true)
        .breakType(BreakType.PAID)
        .build();

    int netMinutes = breakCalculator.calculateNetWorkingMinutes(grossMinutes, breakMinutes, config);
    assertThat(netMinutes).isEqualTo(grossMinutes);
}
```

### Integration Tests

- Test break recording flow
- Test working hours calculation with different break policies
- Test payroll calculation with break deductions
- Test legal minimum enforcement
