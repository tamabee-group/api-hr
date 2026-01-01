# Design Document: Attendance & Payroll Backend System

## Overview

Hệ thống chấm công và tính lương được thiết kế theo kiến trúc **Configuration-driven** với JSON-based settings cho mỗi công ty. Điều này cho phép linh hoạt tối đa trong việc tuỳ chỉnh quy tắc mà không cần thay đổi code.

### Design Principles

1. **Configuration over Code**: Mọi quy tắc business được lưu trong config, không hardcode
2. **Single Source of Truth**: Mỗi công ty có 1 bộ settings duy nhất
3. **Immutable Calculations**: Kết quả tính toán được lưu snapshot, không tính lại
4. **Audit Everything**: Mọi thay đổi đều được log

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Controller Layer                         │
├─────────────────────────────────────────────────────────────────┤
│  CompanySettingsController  │  AttendanceController  │  PayrollController  │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Service Layer                           │
├─────────────────────────────────────────────────────────────────┤
│  ICompanySettingsService  │  IAttendanceService  │  IPayrollService  │
└─────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Calculator Modules                        │
├─────────────────────────────────────────────────────────────────┤
│  TimeRoundingCalculator  │  OvertimeCalculator  │  PayrollCalculator  │
│  AllowanceCalculator     │  DeductionCalculator │                     │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Repository Layer                         │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Company Settings Service

```java
public interface ICompanySettingsService {

    // Lấy settings của công ty
    CompanySettingsResponse getSettings(Long companyId);

    // Cập nhật từng loại config
    AttendanceConfigResponse updateAttendanceConfig(Long companyId, AttendanceConfigRequest request);
    PayrollConfigResponse updatePayrollConfig(Long companyId, PayrollConfigRequest request);
    OvertimeConfigResponse updateOvertimeConfig(Long companyId, OvertimeConfigRequest request);
    AllowanceConfigResponse updateAllowanceConfig(Long companyId, AllowanceConfigRequest request);
    DeductionConfigResponse updateDeductionConfig(Long companyId, DeductionConfigRequest request);

    // Khởi tạo default settings cho company mới
    void initializeDefaultSettings(Long companyId);
}
```

### 2. Work Schedule Service

```java
public interface IWorkScheduleService {

    // CRUD operations
    WorkScheduleResponse createSchedule(Long companyId, CreateWorkScheduleRequest request);
    WorkScheduleResponse updateSchedule(Long scheduleId, UpdateWorkScheduleRequest request);
    void deleteSchedule(Long scheduleId);

    // Query
    Page<WorkScheduleResponse> getSchedules(Long companyId, Pageable pageable);
    WorkScheduleResponse getScheduleById(Long scheduleId);

    // Lấy schedule của employee (hoặc default nếu chưa assign)
    WorkScheduleResponse getEffectiveSchedule(Long employeeId, LocalDate date);

    // Assign schedule cho employee
    void assignScheduleToEmployee(Long scheduleId, Long employeeId);
    void assignScheduleToEmployees(Long scheduleId, List<Long> employeeIds);
}
```

### 3. Attendance Service

```java
public interface IAttendanceService {

    // Check-in/Check-out
    AttendanceRecordResponse checkIn(Long employeeId, CheckInRequest request);
    AttendanceRecordResponse checkOut(Long employeeId, CheckOutRequest request);

    // Manual adjustment (by admin)
    AttendanceRecordResponse adjustAttendance(Long recordId, AdjustAttendanceRequest request);

    // Query
    AttendanceRecordResponse getAttendanceRecordById(Long recordId);
    AttendanceRecordResponse getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date);
    Page<AttendanceRecordResponse> getAttendanceRecords(Long companyId, AttendanceQueryRequest request, Pageable pageable);
    Page<AttendanceRecordResponse> getEmployeeAttendanceRecords(Long employeeId, AttendanceQueryRequest request, Pageable pageable);
    AttendanceSummaryResponse getAttendanceSummary(Long employeeId, YearMonth period);

    // Validation
    boolean validateDevice(Long companyId, String deviceId);
    boolean validateLocation(Long companyId, Double latitude, Double longitude);
}
```

### 4. Attendance Adjustment Service

```java
public interface IAttendanceAdjustmentService {

    // Employee tạo request
    AdjustmentRequestResponse createAdjustmentRequest(Long employeeId, CreateAdjustmentRequest request);

    // Manager approve/reject
    AdjustmentRequestResponse approveAdjustment(Long requestId, Long managerId, String comment);
    AdjustmentRequestResponse rejectAdjustment(Long requestId, Long managerId, String reason);

    // Query
    AdjustmentRequestResponse getRequestById(Long requestId);
    Page<AdjustmentRequestResponse> getPendingRequests(Long companyId, Pageable pageable);
    Page<AdjustmentRequestResponse> getEmployeeRequests(Long employeeId, Pageable pageable);
    List<AdjustmentRequestResponse> getAdjustmentHistoryByAttendanceRecord(Long attendanceRecordId);
}
```

### 5. Payroll Service

```java
public interface IPayrollService {

    // Tính lương preview (chưa finalize)
    PayrollPreviewResponse previewPayroll(Long companyId, YearMonth period);
    PayrollPreviewResponse previewEmployeePayroll(Long employeeId, YearMonth period);

    // Finalize payroll
    PayrollResponse finalizePayroll(Long companyId, YearMonth period);

    // Payment processing
    void markAsPaid(Long companyId, YearMonth period);
    void markEmployeeAsPaid(Long payrollRecordId);
    void retryPayment(Long payrollRecordId);

    // Salary notification
    void sendSalaryNotifications(Long companyId, YearMonth period);

    // Query
    PayrollRecordResponse getPayrollRecordById(Long recordId);
    PayrollPeriodSummaryResponse getPayrollPeriodSummary(Long companyId, YearMonth period);
    Page<PayrollRecordResponse> getPayrollRecords(Long companyId, YearMonth period, Pageable pageable);
    PayrollRecordResponse getEmployeePayroll(Long employeeId, YearMonth period);
    Page<PayrollRecordResponse> getEmployeePayrollHistory(Long employeeId, Pageable pageable);

    // Export
    byte[] exportPayrollCsv(Long companyId, YearMonth period);
    byte[] exportPayrollPdf(Long companyId, YearMonth period);
    byte[] generatePayslip(Long payrollRecordId);
}
```

### 6. Schedule Selection Service

````java
public interface IScheduleSelectionService {

    // Employee chọn lịch
    ScheduleSelectionResponse selectSchedule(Long employeeId, SelectScheduleRequest request);

    // Manager approve/reject
    ScheduleSelectionResponse approveSelection(Long selectionId, Long managerId);
    ScheduleSelectionResponse rejectSelection(Long selectionId, Long managerId, String reason);

    // Suggestions
    List<WorkScheduleResponse> getSuggestedSchedules(Long employeeId);
    List<WorkScheduleResponse> getAvailableSchedules(Long companyId, LocalDate date);

    // Query
    ScheduleSelectionResponse getSelectionById(Long selectionId);
    Page<ScheduleSelectionResponse> getPendingSelections(Long companyId, Pageable pageable);
    List<ScheduleSelectionResponse> getEmployeeSelectionHistory(Long employeeId);
}

### 6b. Holiday Service

```java
public interface IHolidayService {

    // CRUD
    HolidayResponse createHoliday(Long companyId, CreateHolidayRequest request);
    HolidayResponse updateHoliday(Long holidayId, UpdateHolidayRequest request);
    void deleteHoliday(Long holidayId);

    // Query
    HolidayResponse getHolidayById(Long holidayId);
    Page<HolidayResponse> getHolidays(Long companyId, Pageable pageable);
    List<HolidayResponse> getHolidaysByDateRange(Long companyId, LocalDate startDate, LocalDate endDate);
    List<HolidayResponse> getNationalHolidays(LocalDate startDate, LocalDate endDate);
}

### 6c. Leave Service

```java
public interface ILeaveService {

    // Employee tạo request
    LeaveRequestResponse createLeaveRequest(Long employeeId, CreateLeaveRequest request);
    LeaveRequestResponse cancelLeaveRequest(Long requestId, Long employeeId);

    // Manager approve/reject
    LeaveRequestResponse approveLeave(Long requestId, Long managerId);
    LeaveRequestResponse rejectLeave(Long requestId, Long managerId, String reason);

    // Query
    LeaveRequestResponse getLeaveRequestById(Long requestId);
    Page<LeaveRequestResponse> getPendingLeaveRequests(Long companyId, Pageable pageable);
    Page<LeaveRequestResponse> getEmployeeLeaveRequests(Long employeeId, Pageable pageable);

    // Balance
    List<LeaveBalanceResponse> getLeaveBalance(Long employeeId, Integer year);
    void updateLeaveBalance(Long employeeId, LeaveType type, Integer year, Integer adjustment);
}
````

### 7. Plan Feature Service

```java
public interface IPlanFeatureService {

    // Check feature access
    boolean hasFeatureAccess(Long companyId, FeatureCode feature);
    void validateFeatureAccess(Long companyId, FeatureCode feature);

    // Get plan features
    List<FeatureCode> getCompanyFeatures(Long companyId);
    PlanFeaturesResponse getPlanFeatures(Long planId);
}
```

### 8. Calculator Modules

```java
// Làm tròn giờ
public interface ITimeRoundingCalculator {
    LocalDateTime roundTime(LocalDateTime time, RoundingConfig config, RoundingTarget target);
}

// Tính tăng ca
public interface IOvertimeCalculator {
    OvertimeResult calculateOvertime(
        List<AttendanceRecord> records,
        WorkSchedule schedule,
        OvertimeConfig config,
        YearMonth period
    );
}

// Tính phụ cấp
public interface IAllowanceCalculator {
    AllowanceResult calculateAllowances(
        Employee employee,
        AttendanceSummary attendance,
        AllowanceConfig config,
        YearMonth period
    );
}

// Tính khấu trừ
public interface IDeductionCalculator {
    DeductionResult calculateDeductions(
        Employee employee,
        AttendanceSummary attendance,
        BigDecimal grossSalary,
        DeductionConfig config,
        YearMonth period
    );
}

// Tính lương tổng hợp
public interface IPayrollCalculator {
    PayrollResult calculatePayroll(
        Employee employee,
        AttendanceSummary attendance,
        CompanySettings settings,
        YearMonth period
    );
}
```

## Data Models

### Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────────┐
│   CompanyEntity │──1:1──│ CompanySettingsEntity│
└─────────────────┘       └─────────────────────┘
        │                           │
        │ 1:N                       │ JSON configs
        ▼                           ▼
┌─────────────────┐       ┌─────────────────────┐
│   UserEntity    │       │ - attendanceConfig  │
│   (Employee)    │       │ - payrollConfig     │
└─────────────────┘       │ - overtimeConfig    │
        │                 │ - allowanceConfig   │
        │ 1:N             │ - deductionConfig   │
        ▼                 └─────────────────────┘
┌─────────────────┐
│WorkScheduleAssign│
└─────────────────┘
        │
        │ N:1
        ▼
┌─────────────────┐
│ WorkScheduleEntity│
└─────────────────┘

┌─────────────────┐       ┌─────────────────────┐
│   UserEntity    │──1:N──│AttendanceRecordEntity│
└─────────────────┘       └─────────────────────┘
        │
        │ 1:N
        ▼
┌─────────────────┐
│PayrollRecordEntity│
└─────────────────┘
```

### Core Entities

```java
@Entity
@Table(name = "company_settings")
public class CompanySettingsEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long companyId;

    @Column(columnDefinition = "jsonb")
    private String attendanceConfig;  // AttendanceConfig JSON

    @Column(columnDefinition = "jsonb")
    private String payrollConfig;     // PayrollConfig JSON

    @Column(columnDefinition = "jsonb")
    private String overtimeConfig;    // OvertimeConfig JSON

    @Column(columnDefinition = "jsonb")
    private String allowanceConfig;   // AllowanceConfig JSON

    @Column(columnDefinition = "jsonb")
    private String deductionConfig;   // DeductionConfig JSON
}

@Entity
@Table(name = "work_schedules")
public class WorkScheduleEntity extends BaseEntity {

    private Long companyId;
    private String name;

    @Enumerated(EnumType.STRING)
    private ScheduleType type;  // FIXED, FLEXIBLE, SHIFT

    private Boolean isDefault;

    @Column(columnDefinition = "jsonb")
    private String scheduleData;  // WorkScheduleData JSON
}

@Entity
@Table(name = "work_schedule_assignments")
public class WorkScheduleAssignmentEntity extends BaseEntity {

    private Long employeeId;
    private Long scheduleId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}

@Entity
@Table(name = "attendance_records")
public class AttendanceRecordEntity extends BaseEntity {

    private Long employeeId;
    private Long companyId;
    private LocalDate workDate;

    // Thời gian gốc
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;

    // Thời gian sau khi làm tròn
    private LocalDateTime roundedCheckIn;
    private LocalDateTime roundedCheckOut;

    // Tính toán
    private Integer workingMinutes;
    private Integer overtimeMinutes;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;

    // Trạng thái
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;  // PRESENT, ABSENT, LEAVE, HOLIDAY

    // Device & Location
    private String deviceId;
    private Double checkInLatitude;
    private Double checkInLongitude;
    private Double checkOutLatitude;
    private Double checkOutLongitude;

    // Audit
    private String adjustmentReason;
    private Long adjustedBy;
}

@Entity
@Table(name = "payroll_records")
public class PayrollRecordEntity extends BaseEntity {

    private Long employeeId;
    private Long companyId;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month;

    // Lương cơ bản
    @Enumerated(EnumType.STRING)
    private SalaryType salaryType;
    private BigDecimal baseSalary;

    // Tăng ca
    private BigDecimal regularOvertimePay;
    private BigDecimal nightOvertimePay;
    private BigDecimal holidayOvertimePay;
    private BigDecimal weekendOvertimePay;
    private BigDecimal totalOvertimePay;

    // Phụ cấp
    @Column(columnDefinition = "jsonb")
    private String allowanceDetails;  // List<AllowanceItem> JSON
    private BigDecimal totalAllowances;

    // Khấu trừ
    @Column(columnDefinition = "jsonb")
    private String deductionDetails;  // List<DeductionItem> JSON
    private BigDecimal totalDeductions;

    // Tổng kết
    private BigDecimal grossSalary;
    private BigDecimal netSalary;

    // Trạng thái
    @Enumerated(EnumType.STRING)
    private PayrollStatus status;  // DRAFT, FINALIZED, PAID

    // Payment tracking
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;  // PENDING, PAID, FAILED
    private LocalDateTime paidAt;
    private String paymentReference;

    // Notification
    private Boolean notificationSent = false;
    private LocalDateTime notificationSentAt;

    private LocalDateTime finalizedAt;
    private Long finalizedBy;
}

@Entity
@Table(name = "schedule_selections")
public class ScheduleSelectionEntity extends BaseEntity {

    private Long employeeId;
    private Long companyId;
    private Long scheduleId;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    private SelectionStatus status;  // PENDING, APPROVED, REJECTED

    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
}

@Entity
@Table(name = "attendance_adjustment_requests")
public class AttendanceAdjustmentRequestEntity extends BaseEntity {

    private Long employeeId;
    private Long companyId;
    private Long attendanceRecordId;

    // Thời gian gốc
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;

    // Thời gian yêu cầu thay đổi
    private LocalDateTime requestedCheckIn;
    private LocalDateTime requestedCheckOut;

    // Lý do
    @Column(length = 500)
    private String reason;

    // Trạng thái
    @Enumerated(EnumType.STRING)
    private AdjustmentStatus status;  // PENDING, APPROVED, REJECTED

    // Approval info
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String approverComment;
    private String rejectionReason;
}

@Entity
@Table(name = "holidays")
public class HolidayEntity extends BaseEntity {

    private Long companyId;  // null = national holiday
    private LocalDate date;
    private String name;

    @Enumerated(EnumType.STRING)
    private HolidayType type;  // NATIONAL, COMPANY

    private Boolean isPaid;
}

@Entity
@Table(name = "leave_requests")
public class LeaveRequestEntity extends BaseEntity {

    private Long employeeId;
    private Long companyId;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;  // ANNUAL, SICK, PERSONAL, UNPAID

    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status;  // PENDING, APPROVED, REJECTED

    private Long approvedBy;
    private LocalDateTime approvedAt;
}

@Entity
@Table(name = "leave_balances")
public class LeaveBalanceEntity extends BaseEntity {

    private Long employeeId;
    private Integer year;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    private Integer totalDays;
    private Integer usedDays;
    private Integer remainingDays;
}
```

### Configuration DTOs

```java
@Data
@Builder
public class AttendanceConfig {
    // Giờ làm việc mặc định
    private LocalTime defaultWorkStartTime = LocalTime.of(9, 0);
    private LocalTime defaultWorkEndTime = LocalTime.of(18, 0);
    private Integer defaultBreakMinutes = 60;

    // Làm tròn giờ
    private Boolean enableRounding = false;
    private RoundingConfig checkInRounding;
    private RoundingConfig checkOutRounding;

    // Grace period (phút)
    private Integer lateGraceMinutes = 0;
    private Integer earlyLeaveGraceMinutes = 0;

    // Device & Location
    private Boolean requireDeviceRegistration = false;
    private Boolean requireGeoLocation = false;
    private Integer geoFenceRadiusMeters = 100;

    // Cho phép chấm công
    private Boolean allowMobileCheckIn = true;
    private Boolean allowWebCheckIn = true;
}

@Data
@Builder
public class RoundingConfig {
    private RoundingInterval interval = RoundingInterval.MINUTES_15;
    private RoundingDirection direction = RoundingDirection.NEAREST;
}

@Data
@Builder
public class PayrollConfig {
    private SalaryType defaultSalaryType = SalaryType.MONTHLY;
    private Integer payDay = 25;
    private Integer cutoffDay = 20;
    private RoundingDirection salaryRounding = RoundingDirection.NEAREST;
    private Integer standardWorkingDaysPerMonth = 22;
    private Integer standardWorkingHoursPerDay = 8;
}

@Data
@Builder
public class OvertimeConfig {
    private Boolean enableOvertime = true;
    private Boolean requireApproval = false;

    // Multiplier rates
    private BigDecimal regularOvertimeRate = new BigDecimal("1.25");
    private BigDecimal nightOvertimeRate = new BigDecimal("1.50");
    private BigDecimal holidayOvertimeRate = new BigDecimal("1.35");
    private BigDecimal weekendOvertimeRate = new BigDecimal("1.35");

    // Night hours
    private LocalTime nightStartTime = LocalTime.of(22, 0);
    private LocalTime nightEndTime = LocalTime.of(5, 0);

    // Limits
    private Integer maxOvertimeHoursPerDay = 4;
    private Integer maxOvertimeHoursPerMonth = 45;
}

@Data
@Builder
public class AllowanceConfig {
    private List<AllowanceRule> allowances = new ArrayList<>();
}

@Data
@Builder
public class AllowanceRule {
    private String code;
    private String name;
    private AllowanceType type;  // FIXED, CONDITIONAL, ONE_TIME
    private BigDecimal amount;
    private Boolean taxable = true;

    // Điều kiện (cho CONDITIONAL type)
    private AllowanceCondition condition;
}

@Data
@Builder
public class DeductionConfig {
    private List<DeductionRule> deductions = new ArrayList<>();

    // Phạt đi muộn/về sớm
    private Boolean enableLatePenalty = false;
    private BigDecimal latePenaltyPerMinute = BigDecimal.ZERO;
    private Boolean enableEarlyLeavePenalty = false;
    private BigDecimal earlyLeavePenaltyPerMinute = BigDecimal.ZERO;

    // Khấu trừ vắng mặt
    private Boolean enableAbsenceDeduction = true;
}

@Data
@Builder
public class DeductionRule {
    private String code;
    private String name;
    private DeductionType type;  // FIXED, PERCENTAGE
    private BigDecimal amount;
    private BigDecimal percentage;
    private Integer order;  // Thứ tự áp dụng
}
```

### Enums

```java
public enum ScheduleType {
    FIXED,      // Giờ cố định mỗi ngày
    FLEXIBLE,   // Giờ khác nhau theo ngày
    SHIFT       // Làm theo ca
}

public enum RoundingInterval {
    MINUTES_5(5),
    MINUTES_10(10),
    MINUTES_15(15),
    MINUTES_30(30),
    MINUTES_60(60);

    private final int minutes;
}

public enum RoundingDirection {
    UP,      // Làm tròn lên
    DOWN,    // Làm tròn xuống
    NEAREST  // Làm tròn gần nhất
}

public enum SalaryType {
    MONTHLY,  // Lương tháng
    DAILY,    // Lương ngày
    HOURLY    // Lương giờ
}

public enum AttendanceStatus {
    PRESENT,   // Có mặt
    ABSENT,    // Vắng mặt
    LEAVE,     // Nghỉ phép
    HOLIDAY    // Ngày lễ
}

public enum PayrollStatus {
    DRAFT,     // Nháp (có thể sửa)
    FINALIZED, // Đã chốt (không thể sửa)
    PAID       // Đã trả lương
}

public enum PaymentStatus {
    PENDING,   // Chờ thanh toán
    PAID,      // Đã thanh toán
    FAILED     // Thanh toán thất bại
}

public enum SelectionStatus {
    PENDING,   // Chờ duyệt
    APPROVED,  // Đã duyệt
    REJECTED   // Từ chối
}

public enum FeatureCode {
    ATTENDANCE,           // Chấm công cơ bản
    PAYROLL,              // Tính lương
    OVERTIME,             // Tăng ca
    LEAVE_MANAGEMENT,     // Quản lý nghỉ phép
    GEO_LOCATION,         // Chấm công theo vị trí
    DEVICE_REGISTRATION,  // Đăng ký thiết bị
    REPORTS,              // Báo cáo
    FLEXIBLE_SCHEDULE     // Lịch làm việc linh hoạt
}

public enum LeaveType {
    ANNUAL,    // Nghỉ phép năm
    SICK,      // Nghỉ ốm
    PERSONAL,  // Nghỉ việc riêng
    UNPAID     // Nghỉ không lương
}

public enum LeaveStatus {
    PENDING,   // Chờ duyệt
    APPROVED,  // Đã duyệt
    REJECTED   // Từ chối
}

public enum AdjustmentStatus {
    PENDING,   // Chờ duyệt
    APPROVED,  // Đã duyệt
    REJECTED   // Từ chối
}

public enum HolidayType {
    NATIONAL,  // Ngày lễ quốc gia
    COMPANY    // Ngày nghỉ công ty
}

public enum AllowanceType {
    FIXED,       // Cố định mỗi kỳ
    CONDITIONAL, // Theo điều kiện
    ONE_TIME     // Một lần
}

public enum DeductionType {
    FIXED,      // Số tiền cố định
    PERCENTAGE  // Phần trăm
}
```

## Correctness Properties

_A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees._

### Property 1: Configuration Round-Trip Consistency

_For any_ valid company settings configuration (attendance, payroll, overtime, allowance, deduction), serializing to JSON then deserializing back SHALL produce an equivalent configuration object.

**Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**

### Property 2: Default Configuration Initialization

_For any_ newly created company, the Company_Settings_Service SHALL create settings with all required fields having non-null default values.

**Validates: Requirements 1.6, 1.8**

### Property 3: Work Schedule Time Validity

_For any_ valid work schedule, the start time SHALL always be before the end time for each working period.

**Validates: Requirements 2.4**

### Property 4: Employee Schedule Resolution

_For any_ employee without an assigned schedule, the system SHALL return the company's default schedule.

**Validates: Requirements 2.7**

### Property 5: Time Rounding Determinism

_For any_ time value and rounding configuration, applying the rounding function multiple times SHALL produce the same result (idempotent).

**Validates: Requirements 4.5**

### Property 6: Original Time Preservation

_For any_ attendance record with rounding applied, both the original time and rounded time SHALL be stored.

**Validates: Requirements 4.4**

### Property 7: Working Hours Calculation

_For any_ attendance record with check-in and check-out times, working hours SHALL equal (checkout - checkin - breaks), and SHALL be non-negative.

**Validates: Requirements 3.4**

### Property 8: Late Detection Accuracy

_For any_ check-in time after (schedule_start + grace_period), the attendance record SHALL be marked as late with correct late minutes.

**Validates: Requirements 3.5**

### Property 9: Early Departure Detection Accuracy

_For any_ check-out time before (schedule_end - grace_period), the attendance record SHALL be marked as early departure with correct early minutes.

**Validates: Requirements 3.6**

### Property 10: Overtime Types Sum Invariant

_For any_ overtime calculation, the sum of (regular + night + holiday + weekend) overtime hours SHALL equal total overtime hours.

**Validates: Requirements 5.8**

### Property 11: Overtime Cap Enforcement

_For any_ overtime calculation with a configured maximum, the calculated overtime SHALL NOT exceed the maximum limit.

**Validates: Requirements 5.6**

### Property 12: Daily Salary Calculation

_For any_ employee with daily salary type, the base salary SHALL equal (daily_rate × working_days).

**Validates: Requirements 6.2**

### Property 13: Hourly Salary Calculation

_For any_ employee with hourly salary type, the base salary SHALL equal (hourly_rate × working_hours).

**Validates: Requirements 6.3**

### Property 14: Gross Salary Invariant

_For any_ payroll calculation, gross salary SHALL equal (base_salary + total_overtime_pay + total_allowances).

**Validates: Requirements 6.6**

### Property 15: Allowances Sum Invariant

_For any_ allowance calculation, the sum of individual allowance amounts SHALL equal total allowances.

**Validates: Requirements 7.8**

### Property 16: Deductions Sum Invariant

_For any_ deduction calculation, the sum of individual deduction amounts SHALL equal total deductions.

**Validates: Requirements 8.9**

### Property 17: Net Salary Formula

_For any_ payroll calculation, net salary SHALL equal (gross_salary - total_deductions).

**Validates: Requirements 9.6**

### Property 18: Payroll Record Round-Trip

_For any_ payroll record, serializing then deserializing SHALL produce an equivalent record.

**Validates: Requirements 9.10**

### Property 19: Finalized Payroll Immutability

_For any_ finalized payroll record, modification attempts SHALL be rejected.

**Validates: Requirements 9.9**

### Property 20: Audit Log Creation

_For any_ modification to attendance or payroll records, an audit log entry SHALL be created with timestamp, user, and change details.

**Validates: Requirements 13.1, 13.2, 13.3, 13.4**

### Property 21: Adjustment Request Workflow

_For any_ approved adjustment request, the attendance record SHALL be updated with the requested times and all derived values (working hours, overtime, late/early status) SHALL be recalculated.

**Validates: Requirements 10.3, 10.7**

### Property 22: Adjustment Request History

_For any_ adjustment request (approved or rejected), the original times, requested times, and decision details SHALL be preserved for audit.

**Validates: Requirements 11.1, 11.5**

### Property 23: Salary Notification Content

_For any_ salary notification sent to an employee, the notification SHALL contain net salary, earnings breakdown, deductions breakdown, and payment date.

**Validates: Requirements 10.4**

### Property 24: Plan Feature Access Control

_For any_ feature access request, the system SHALL allow access only if the company's plan includes that feature.

**Validates: Requirements 13.1, 13.2, 13.3, 13.4**

### Property 25: Schedule Suggestion Relevance

_For any_ schedule suggestion for an employee, the suggested schedules SHALL include schedules from the employee's past selections or company-recommended schedules.

**Validates: Requirements 12.2, 12.3**

### Property 26: Email Template Language Selection

_For any_ email notification sent to an employee, the system SHALL use the template matching the employee's locale setting, or fallback to English if not available.

**Validates: Requirements 17.7, 17.8**

## Email Templates

### Template Structure

```
src/main/resources/templates/email/
├── vi/
│   ├── salary-notification.html
│   ├── adjustment-approved.html
│   ├── adjustment-rejected.html
│   ├── leave-approved.html
│   └── leave-rejected.html
├── en/
│   ├── salary-notification.html
│   ├── adjustment-approved.html
│   ├── adjustment-rejected.html
│   ├── leave-approved.html
│   └── leave-rejected.html
└── ja/
    ├── salary-notification.html
    ├── adjustment-approved.html
    ├── adjustment-rejected.html
    ├── leave-approved.html
    └── leave-rejected.html
```

### Template Placeholders

```java
// Salary Notification
{employeeName}      // Tên nhân viên
{period}            // Kỳ lương (e.g., "2025年01月" / "01/2025")
{netSalary}         // Lương thực nhận (formatted)
{baseSalary}        // Lương cơ bản
{totalOvertime}     // Tổng tăng ca
{totalAllowances}   // Tổng phụ cấp
{totalDeductions}   // Tổng khấu trừ
{paymentDate}       // Ngày thanh toán
{companyName}       // Tên công ty

// Adjustment Approved
{employeeName}      // Tên nhân viên
{workDate}          // Ngày làm việc
{originalCheckIn}   // Giờ vào gốc
{originalCheckOut}  // Giờ ra gốc
{approvedCheckIn}   // Giờ vào được duyệt
{approvedCheckOut}  // Giờ ra được duyệt
{approverName}      // Người duyệt
{approvedAt}        // Thời gian duyệt

// Adjustment Rejected
{employeeName}      // Tên nhân viên
{workDate}          // Ngày làm việc
{originalCheckIn}   // Giờ vào gốc
{originalCheckOut}  // Giờ ra gốc
{requestedCheckIn}  // Giờ vào yêu cầu
{requestedCheckOut} // Giờ ra yêu cầu
{rejectionReason}   // Lý do từ chối
{approverName}      // Người từ chối

// Leave Approved
{employeeName}      // Tên nhân viên
{leaveType}         // Loại nghỉ phép
{startDate}         // Ngày bắt đầu
{endDate}           // Ngày kết thúc
{totalDays}         // Tổng số ngày
{approverName}      // Người duyệt

// Leave Rejected
{employeeName}      // Tên nhân viên
{leaveType}         // Loại nghỉ phép
{startDate}         // Ngày bắt đầu
{endDate}           // Ngày kết thúc
{rejectionReason}   // Lý do từ chối
{approverName}      // Người từ chối
```

### Email Service Interface

```java
public interface INotificationEmailService {

    // Salary notification
    void sendSalaryNotification(Long employeeId, PayrollRecord payroll);
    void sendBulkSalaryNotifications(Long companyId, YearMonth period);

    // Adjustment notifications
    void sendAdjustmentApprovedNotification(Long employeeId, AdjustmentRequest request);
    void sendAdjustmentRejectedNotification(Long employeeId, AdjustmentRequest request);

    // Leave notifications
    void sendLeaveApprovedNotification(Long employeeId, LeaveRequest request);
    void sendLeaveRejectedNotification(Long employeeId, LeaveRequest request);
}
```

### Template Selection Logic

```java
// Lấy locale từ user settings
String userLocale = userEntity.getLocale(); // "Asia/Ho_Chi_Minh" -> "vi"

// Map timezone to locale code
String localeCode = LocaleUtil.timezoneToLocale(userLocale);

// Load template với fallback
String templatePath = String.format("email/%s/%s.html", localeCode, templateName);
if (!templateExists(templatePath)) {
    templatePath = String.format("email/en/%s.html", templateName); // Fallback to English
}
```

### Email Template Guidelines

- Inline CSS only (email client compatibility)
- Max-width: 600px
- Brand color: #00b1ce
- Mobile-responsive design
- Plain text fallback for accessibility

## Error Handling

### Error Codes

```java
// Company Settings errors
SETTINGS_NOT_FOUND("SETTINGS_001", "Company settings not found"),
INVALID_ATTENDANCE_CONFIG("SETTINGS_002", "Invalid attendance configuration"),
INVALID_PAYROLL_CONFIG("SETTINGS_003", "Invalid payroll configuration"),
INVALID_OVERTIME_CONFIG("SETTINGS_004", "Invalid overtime configuration"),

// Work Schedule errors
SCHEDULE_NOT_FOUND("SCHEDULE_001", "Work schedule not found"),
INVALID_SCHEDULE_TIME("SCHEDULE_002", "Start time must be before end time"),
SCHEDULE_OVERLAP("SCHEDULE_003", "Schedule assignment overlaps with existing"),

// Attendance errors
ALREADY_CHECKED_IN("ATTENDANCE_001", "Already checked in today"),
NOT_CHECKED_IN("ATTENDANCE_002", "Must check in before check out"),
INVALID_DEVICE("ATTENDANCE_003", "Device not registered"),
OUTSIDE_GEOFENCE("ATTENDANCE_004", "Location outside allowed area"),
RECORD_NOT_FOUND("ATTENDANCE_005", "Attendance record not found"),

// Adjustment Request errors
ADJUSTMENT_NOT_FOUND("ADJUSTMENT_001", "Adjustment request not found"),
ADJUSTMENT_ALREADY_PROCESSED("ADJUSTMENT_002", "Adjustment request already processed"),
INVALID_ADJUSTMENT_TIME("ADJUSTMENT_003", "Invalid adjustment time"),

// Payroll errors
PAYROLL_NOT_FOUND("PAYROLL_001", "Payroll record not found"),
PAYROLL_ALREADY_FINALIZED("PAYROLL_002", "Payroll already finalized"),
INVALID_PAYROLL_PERIOD("PAYROLL_003", "Invalid payroll period"),
ATTENDANCE_NOT_COMPLETE("PAYROLL_004", "Attendance records incomplete for period"),

// Leave errors
LEAVE_NOT_FOUND("LEAVE_001", "Leave request not found"),
INSUFFICIENT_LEAVE_BALANCE("LEAVE_002", "Insufficient leave balance"),
LEAVE_OVERLAP("LEAVE_003", "Leave dates overlap with existing request"),

// Schedule Selection errors
SELECTION_NOT_FOUND("SELECTION_001", "Schedule selection not found"),
SELECTION_ALREADY_PROCESSED("SELECTION_002", "Schedule selection already processed"),
SCHEDULE_NOT_AVAILABLE("SELECTION_003", "Schedule not available for selected dates"),

// Plan Feature errors
FEATURE_NOT_AVAILABLE("PLAN_001", "Feature not available in current plan"),
PLAN_EXPIRED("PLAN_002", "Company plan has expired"),

// Payment errors
PAYMENT_FAILED("PAYMENT_001", "Payment processing failed"),
PAYROLL_NOT_FINALIZED("PAYMENT_002", "Payroll must be finalized before payment")
```

### Exception Handling Strategy

```java
// Sử dụng custom exceptions từ project hiện tại
throw NotFoundException.of(ErrorCode.SETTINGS_NOT_FOUND);
throw BadRequestException.of(ErrorCode.INVALID_SCHEDULE_TIME);
throw ConflictException.of(ErrorCode.ALREADY_CHECKED_IN);
```

## Testing Strategy

### Unit Tests

- Test từng Calculator module độc lập
- Test validation logic cho configs
- Test edge cases: midnight crossing, DST changes

### Property-Based Tests

Sử dụng **jqwik** library cho property-based testing:

```java
@Property
void configRoundTrip(@ForAll @ValidAttendanceConfig AttendanceConfig config) {
    String json = objectMapper.writeValueAsString(config);
    AttendanceConfig restored = objectMapper.readValue(json, AttendanceConfig.class);
    assertThat(restored).isEqualTo(config);
}

@Property
void overtimeSumInvariant(@ForAll @ValidOvertimeResult OvertimeResult result) {
    BigDecimal sum = result.getRegularHours()
        .add(result.getNightHours())
        .add(result.getHolidayHours())
        .add(result.getWeekendHours());
    assertThat(sum).isEqualTo(result.getTotalHours());
}

@Property
void netSalaryFormula(@ForAll @ValidPayrollResult PayrollResult result) {
    BigDecimal expected = result.getGrossSalary().subtract(result.getTotalDeductions());
    assertThat(result.getNetSalary()).isEqualTo(expected);
}
```

### Integration Tests

- Test full payroll calculation flow
- Test attendance → payroll integration
- Test config changes affect calculations

### Test Configuration

- Minimum 100 iterations per property test
- Use custom generators for valid business objects
- Tag format: **Feature: attendance-payroll-backend, Property {number}: {property_text}**
