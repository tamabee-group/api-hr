# Design Document: Flexible Workforce Management - Backend API

## Overview

Thiết kế hệ thống backend API cho quản lý nhân sự linh hoạt, hỗ trợ các doanh nghiệp tự do như quán ăn, nhà hàng, cafe với các đặc điểm:

- Unified Attendance API (gộp attendance + break)
- Shift Management với swap functionality
- Individual salary configuration
- Personalized allowances/deductions
- Payroll workflow với review/approval
- Employment contract management
- Comprehensive reporting

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Controller Layer                          │
├─────────────────────────────────────────────────────────────────┤
│  AttendanceController  │  ShiftController  │  PayrollController │
│  ContractController    │  ReportController │  SettingsController│
└─────────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────────┐
│                         Service Layer                            │
├─────────────────────────────────────────────────────────────────┤
│  IAttendanceService    │  IShiftService    │  IPayrollService   │
│  IContractService      │  IReportService   │  ISettingsService  │
│  IPayrollCalculator    │  IBreakCalculator │  IOvertimeCalculator│
└─────────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────────┐
│                         Mapper Layer                             │
├─────────────────────────────────────────────────────────────────┤
│  AttendanceMapper  │  ShiftMapper  │  PayrollMapper  │  etc.    │
└─────────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────────┐
│                       Repository Layer                           │
├─────────────────────────────────────────────────────────────────┤
│  AttendanceRecordRepository  │  ShiftTemplateRepository         │
│  ShiftAssignmentRepository   │  ShiftSwapRequestRepository      │
│  EmployeeSalaryRepository    │  EmployeeAllowanceRepository     │
│  EmployeeDeductionRepository │  PayrollPeriodRepository         │
│  PayrollItemRepository       │  EmploymentContractRepository    │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Unified Attendance Service

```java
public interface IUnifiedAttendanceService {
    // Lấy trạng thái chấm công hôm nay (bao gồm break records)
    UnifiedAttendanceResponse getTodayAttendance(Long employeeId);

    // Lấy chấm công theo ngày (bao gồm break records)
    UnifiedAttendanceResponse getAttendanceByDate(Long employeeId, LocalDate date);

    // Check-in với validation company settings
    UnifiedAttendanceResponse checkIn(Long employeeId, CheckInRequest request);

    // Check-out với calculation
    UnifiedAttendanceResponse checkOut(Long employeeId, CheckOutRequest request);

    // Start break
    UnifiedAttendanceResponse startBreak(Long employeeId, StartBreakRequest request);

    // End break
    UnifiedAttendanceResponse endBreak(Long employeeId, Long breakRecordId);
}
```

### 2. Shift Management Service

```java
public interface IShiftService {
    // Shift Template CRUD
    ShiftTemplateResponse createShiftTemplate(Long companyId, ShiftTemplateRequest request);
    ShiftTemplateResponse updateShiftTemplate(Long id, ShiftTemplateRequest request);
    void deleteShiftTemplate(Long id);
    Page<ShiftTemplateResponse> getShiftTemplates(Long companyId, Pageable pageable);

    // Shift Assignment
    ShiftAssignmentResponse assignShift(ShiftAssignmentRequest request);
    void unassignShift(Long assignmentId);
    Page<ShiftAssignmentResponse> getShiftAssignments(Long companyId, ShiftAssignmentQuery query, Pageable pageable);

    // Shift Swap
    ShiftSwapRequestResponse requestSwap(Long employeeId, ShiftSwapRequest request);
    ShiftSwapRequestResponse approveSwap(Long requestId, Long approverId);
    ShiftSwapRequestResponse rejectSwap(Long requestId, Long approverId, String reason);
    Page<ShiftSwapRequestResponse> getSwapRequests(Long companyId, SwapRequestQuery query, Pageable pageable);
}
```

### 3. Employee Salary Configuration Service

```java
public interface IEmployeeSalaryConfigService {
    // Tạo/cập nhật cấu hình lương
    EmployeeSalaryConfigResponse createSalaryConfig(Long employeeId, SalaryConfigRequest request);
    EmployeeSalaryConfigResponse updateSalaryConfig(Long configId, SalaryConfigRequest request);

    // Lấy cấu hình lương hiện tại
    EmployeeSalaryConfigResponse getCurrentSalaryConfig(Long employeeId);

    // Lấy lịch sử cấu hình lương
    List<EmployeeSalaryConfigResponse> getSalaryConfigHistory(Long employeeId);
}
```

### 4. Individual Allowance/Deduction Service

```java
public interface IEmployeeAllowanceService {
    EmployeeAllowanceResponse assignAllowance(Long employeeId, AllowanceAssignmentRequest request);
    EmployeeAllowanceResponse updateAllowance(Long assignmentId, AllowanceAssignmentRequest request);
    void deactivateAllowance(Long assignmentId);
    List<EmployeeAllowanceResponse> getEmployeeAllowances(Long employeeId, boolean includeInactive);
}

public interface IEmployeeDeductionService {
    EmployeeDeductionResponse assignDeduction(Long employeeId, DeductionAssignmentRequest request);
    EmployeeDeductionResponse updateDeduction(Long assignmentId, DeductionAssignmentRequest request);
    void deactivateDeduction(Long assignmentId);
    List<EmployeeDeductionResponse> getEmployeeDeductions(Long employeeId, boolean includeInactive);
}
```

### 5. Payroll Period Service

```java
public interface IPayrollPeriodService {
    // Tạo kỳ lương mới
    PayrollPeriodResponse createPayrollPeriod(Long companyId, PayrollPeriodRequest request);

    // Tính lương cho kỳ
    PayrollPeriodResponse calculatePayroll(Long periodId);

    // Lấy chi tiết kỳ lương
    PayrollPeriodDetailResponse getPayrollPeriodDetail(Long periodId);

    // Điều chỉnh payroll item
    PayrollItemResponse adjustPayrollItem(Long itemId, PayrollAdjustmentRequest request);

    // Workflow actions
    PayrollPeriodResponse submitForReview(Long periodId);
    PayrollPeriodResponse approvePayroll(Long periodId, Long approverId);
    PayrollPeriodResponse markAsPaid(Long periodId, PaymentRequest request);
}
```

### 6. Employment Contract Service

```java
public interface IEmploymentContractService {
    ContractResponse createContract(Long employeeId, ContractRequest request);
    ContractResponse updateContract(Long contractId, ContractRequest request);
    ContractResponse terminateContract(Long contractId, String reason);
    ContractResponse getCurrentContract(Long employeeId);
    List<ContractResponse> getContractHistory(Long employeeId);
    Page<ContractResponse> getExpiringContracts(Long companyId, int daysUntilExpiry, Pageable pageable);
}
```

### 7. Report Service

```java
public interface IReportService {
    AttendanceSummaryReport generateAttendanceSummary(Long companyId, ReportQuery query);
    OvertimeReport generateOvertimeReport(Long companyId, ReportQuery query);
    BreakComplianceReport generateBreakComplianceReport(Long companyId, ReportQuery query);
    PayrollSummaryReport generatePayrollSummary(Long companyId, ReportQuery query);
    CostAnalysisReport generateCostAnalysis(Long companyId, ReportQuery query);
    ShiftUtilizationReport generateShiftUtilization(Long companyId, ReportQuery query);
    byte[] exportReport(ReportType type, Long companyId, ReportQuery query, ExportFormat format);
}
```

## Data Models

### New Entities

```java
// Shift Template - Mẫu ca làm việc
@Entity
@Table(name = "shift_templates")
public class ShiftTemplateEntity extends BaseEntity {
    private Long companyId;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakMinutes;
    private BigDecimal multiplier; // Hệ số lương (1.0, 1.5 cho ca đêm...)
    private String description;
    private Boolean isActive;
}

// Shift Assignment - Phân ca cho nhân viên
@Entity
@Table(name = "shift_assignments")
public class ShiftAssignmentEntity extends BaseEntity {
    private Long employeeId;
    private Long shiftTemplateId;
    private LocalDate workDate;
    @Enumerated(EnumType.STRING)
    private ShiftAssignmentStatus status; // SCHEDULED, COMPLETED, SWAPPED, CANCELLED
    private Long swappedWithEmployeeId;
    private Long swappedFromAssignmentId;
}

// Shift Swap Request - Yêu cầu đổi ca
@Entity
@Table(name = "shift_swap_requests")
public class ShiftSwapRequestEntity extends BaseEntity {
    private Long requesterId;
    private Long targetEmployeeId;
    private Long requesterAssignmentId;
    private Long targetAssignmentId;
    @Enumerated(EnumType.STRING)
    private SwapRequestStatus status; // PENDING, APPROVED, REJECTED
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
}

// Employee Allowance Assignment - Phụ cấp cá nhân
@Entity
@Table(name = "employee_allowances")
public class EmployeeAllowanceEntity extends BaseEntity {
    private Long employeeId;
    private Long companyId;
    private String allowanceCode;
    private String allowanceName;
    @Enumerated(EnumType.STRING)
    private AllowanceType allowanceType;
    private BigDecimal amount;
    private Boolean taxable;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
}

// Employee Deduction Assignment - Khấu trừ cá nhân
@Entity
@Table(name = "employee_deductions")
public class EmployeeDeductionEntity extends BaseEntity {
    private Long employeeId;
    private Long companyId;
    private String deductionCode;
    private String deductionName;
    @Enumerated(EnumType.STRING)
    private DeductionType deductionType;
    private BigDecimal amount;
    private BigDecimal percentage;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
}

// Payroll Period - Kỳ lương
@Entity
@Table(name = "payroll_periods")
public class PayrollPeriodEntity extends BaseEntity {
    private Long companyId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer year;
    private Integer month;
    @Enumerated(EnumType.STRING)
    private PayrollPeriodStatus status; // DRAFT, REVIEWING, APPROVED, PAID
    private Long createdBy;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
    private String paymentReference;
    // Summary fields
    private BigDecimal totalGrossSalary;
    private BigDecimal totalNetSalary;
    private Integer totalEmployees;
}

// Payroll Item - Chi tiết lương từng nhân viên
@Entity
@Table(name = "payroll_items")
public class PayrollItemEntity extends BaseEntity {
    private Long payrollPeriodId;
    private Long employeeId;
    private Long companyId;
    // Salary info
    @Enumerated(EnumType.STRING)
    private SalaryType salaryType;
    private BigDecimal baseSalary;
    private BigDecimal calculatedBaseSalary;
    // Working time
    private Integer workingDays;
    private Integer workingHours;
    private Integer workingMinutes;
    // Overtime
    private Integer regularOvertimeMinutes;
    private Integer nightOvertimeMinutes;
    private Integer holidayOvertimeMinutes;
    private Integer weekendOvertimeMinutes;
    private BigDecimal totalOvertimePay;
    // Break
    private Integer totalBreakMinutes;
    @Enumerated(EnumType.STRING)
    private BreakType breakType;
    private BigDecimal breakDeductionAmount;
    // Allowances & Deductions (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    private String allowanceDetails;
    private BigDecimal totalAllowances;
    @JdbcTypeCode(SqlTypes.JSON)
    private String deductionDetails;
    private BigDecimal totalDeductions;
    // Totals
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    // Adjustment
    private BigDecimal adjustmentAmount;
    private String adjustmentReason;
    private Long adjustedBy;
    private LocalDateTime adjustedAt;
    // Status
    @Enumerated(EnumType.STRING)
    private PayrollItemStatus status; // CALCULATED, ADJUSTED, CONFIRMED
}

// Employment Contract - Hợp đồng lao động
@Entity
@Table(name = "employment_contracts")
public class EmploymentContractEntity extends BaseEntity {
    private Long employeeId;
    private Long companyId;
    @Enumerated(EnumType.STRING)
    private ContractType contractType; // FULL_TIME, PART_TIME, SEASONAL, CONTRACT
    private String contractNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long salaryConfigId; // Link to EmployeeSalaryEntity
    @Enumerated(EnumType.STRING)
    private ContractStatus status; // ACTIVE, EXPIRED, TERMINATED
    private String terminationReason;
    private LocalDate terminatedAt;
    private String notes;
}
```

### New Enums

```java
public enum ShiftAssignmentStatus {
    SCHEDULED, COMPLETED, SWAPPED, CANCELLED
}

public enum SwapRequestStatus {
    PENDING, APPROVED, REJECTED
}

public enum PayrollPeriodStatus {
    DRAFT, REVIEWING, APPROVED, PAID
}

public enum PayrollItemStatus {
    CALCULATED, ADJUSTED, CONFIRMED
}

public enum ContractType {
    FULL_TIME, PART_TIME, SEASONAL, CONTRACT
}

public enum ContractStatus {
    ACTIVE, EXPIRED, TERMINATED
}

public enum ReportType {
    ATTENDANCE_SUMMARY, OVERTIME, BREAK_COMPLIANCE,
    PAYROLL_SUMMARY, COST_ANALYSIS, SHIFT_UTILIZATION
}

public enum ExportFormat {
    CSV, PDF
}
```

### Updated EmployeeSalaryEntity

```java
@Entity
@Table(name = "employee_salaries")
public class EmployeeSalaryEntity extends BaseEntity {
    private Long employeeId;
    private Long companyId;
    @Enumerated(EnumType.STRING)
    private SalaryType salaryType; // MONTHLY, DAILY, HOURLY, SHIFT_BASED
    private BigDecimal monthlySalary;
    private BigDecimal dailyRate;
    private BigDecimal hourlyRate;
    private BigDecimal shiftRate; // NEW: Lương theo ca
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String note;
}
```

### Unified Attendance Response DTO

```java
@Data
public class UnifiedAttendanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate workDate;

    // Check-in/out times
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;
    private LocalDateTime roundedCheckIn;
    private LocalDateTime roundedCheckOut;

    // Calculated values
    private Integer workingMinutes;
    private Integer overtimeMinutes;
    private Integer nightMinutes;
    private Integer nightOvertimeMinutes;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private Integer netWorkingMinutes;

    // Break info
    private Integer totalBreakMinutes;
    private Integer effectiveBreakMinutes;
    private BreakType breakType;
    private Boolean breakCompliant;
    private List<BreakRecordResponse> breakRecords; // Embedded break records

    // Status
    private AttendanceStatus status;

    // Applied settings snapshot
    private AppliedSettingsSnapshot appliedSettings;

    // Shift info (if applicable)
    private ShiftInfo shiftInfo;
}

@Data
public class BreakRecordResponse {
    private Long id;
    private Integer breakNumber;
    private LocalDateTime breakStart;
    private LocalDateTime breakEnd;
    private Integer actualBreakMinutes;
    private Integer effectiveBreakMinutes;
    private String notes;
    private Boolean isActive; // true if break is ongoing
}

@Data
public class AppliedSettingsSnapshot {
    private RoundingConfig checkInRounding;
    private RoundingConfig checkOutRounding;
    private Integer lateGraceMinutes;
    private Integer earlyLeaveGraceMinutes;
    private BreakConfig breakConfig;
}

@Data
public class ShiftInfo {
    private Long shiftTemplateId;
    private String shiftName;
    private LocalTime scheduledStart;
    private LocalTime scheduledEnd;
    private BigDecimal multiplier;
}
```

## Error Handling

```java
// New error codes
public enum ErrorCode {
    // Shift errors
    SHIFT_TEMPLATE_NOT_FOUND,
    SHIFT_ASSIGNMENT_NOT_FOUND,
    SHIFT_OVERLAP_EXISTS,
    SHIFT_SWAP_NOT_ALLOWED,
    SHIFT_SWAP_REQUEST_NOT_FOUND,

    // Salary config errors
    SALARY_CONFIG_NOT_FOUND,
    INVALID_SALARY_TYPE,
    SALARY_CONFIG_OVERLAP,

    // Payroll errors
    PAYROLL_PERIOD_NOT_FOUND,
    PAYROLL_ALREADY_APPROVED,
    PAYROLL_ALREADY_PAID,
    PAYROLL_CALCULATION_FAILED,
    PAYROLL_ITEM_NOT_FOUND,

    // Contract errors
    CONTRACT_NOT_FOUND,
    CONTRACT_OVERLAP_EXISTS,
    CONTRACT_ALREADY_TERMINATED,

    // Allowance/Deduction errors
    ALLOWANCE_NOT_FOUND,
    DEDUCTION_NOT_FOUND,
    INVALID_EFFECTIVE_DATE,

    // Break errors
    MAX_BREAKS_EXCEEDED,
    BREAK_NOT_ALLOWED,
    BREAK_ALREADY_ACTIVE,

    // Settings errors
    COMPANY_SETTINGS_NOT_FOUND,
    INVALID_SETTINGS_CONFIG
}
```

## Correctness Properties

_A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees._

### Property 1: Unified Attendance Response Contains All Break Records

_For any_ attendance record with associated break records, when fetching attendance by date or today, the response SHALL contain the attendance record with all break records embedded in a single response object.

**Validates: Requirements 1.1, 1.2**

### Property 2: Check-in Rounding Applies Company Settings Correctly

_For any_ check-in time and company rounding settings (interval, direction), the rounded check-in time SHALL equal the expected rounded value based on the rounding algorithm.

**Validates: Requirements 1.3, 7.1**

### Property 3: Working Time Calculation Accuracy

_For any_ check-in time, check-out time, and break records, the calculated working minutes SHALL equal (check-out - check-in - total break minutes), and overtime SHALL be calculated based on standard working hours.

**Validates: Requirements 1.4, 7.2**

### Property 4: Break Count Validation

_For any_ employee with existing breaks on a date, starting a new break SHALL be rejected if the break count would exceed the company's maxBreaksPerDay setting.

**Validates: Requirements 1.5, 7.3**

### Property 5: Effective Break Minutes Clamping

_For any_ break duration and company break config (min/max), the effective break minutes SHALL be clamped between minimumBreakMinutes and maximumBreakMinutes.

**Validates: Requirements 1.6**

### Property 6: Device Registration Validation

_For any_ company with requireDeviceRegistration = true, check-in/out requests without a valid deviceId SHALL be rejected.

**Validates: Requirements 1.7**

### Property 7: Geo-fence Validation

_For any_ company with requireGeoLocation = true and geoFenceRadiusMeters = R, check-in/out requests with location distance > R from company location SHALL be rejected.

**Validates: Requirements 1.8**

### Property 8: Shift Assignment No Overlap Invariant

_For any_ employee and date, there SHALL NOT exist two shift assignments with overlapping time ranges on the same date.

**Validates: Requirements 2.7**

### Property 9: Shift Swap Updates Both Assignments

_For any_ approved shift swap request, both the requester's and target's shift assignments SHALL be updated with swapped status and cross-references.

**Validates: Requirements 2.4**

### Property 10: Salary Config Versioning

_For any_ salary config update, the previous active config SHALL have its effectiveTo set to the day before the new config's effectiveFrom.

**Validates: Requirements 3.6**

### Property 11: Active Salary Config Selection

_For any_ employee with multiple salary configs, getCurrentSalaryConfig SHALL return the config where effectiveFrom <= currentDate AND (effectiveTo IS NULL OR effectiveTo >= currentDate).

**Validates: Requirements 3.7**

### Property 12: Salary Calculation by Type

_For any_ employee salary config:

- MONTHLY: calculatedSalary = monthlySalary \* (actualWorkingDays / standardWorkingDays)
- DAILY: calculatedSalary = dailyRate \* actualWorkingDays
- HOURLY: calculatedSalary = hourlyRate \* actualWorkingHours
- SHIFT_BASED: calculatedSalary = shiftRate \* numberOfShifts

**Validates: Requirements 6.1, 6.2, 6.3, 6.4**

### Property 13: Payroll Applies Only Active Allowances/Deductions

_For any_ payroll calculation, only allowances/deductions where effectiveFrom <= periodEnd AND (effectiveTo IS NULL OR effectiveTo >= periodStart) SHALL be included.

**Validates: Requirements 4.3, 6.8**

### Property 14: Payroll Period Status Transition Invariant

_For any_ payroll period with status APPROVED or PAID, any modification attempt SHALL be rejected with appropriate error.

**Validates: Requirements 5.7**

### Property 15: Gross Salary Calculation Formula

_For any_ payroll item, grossSalary SHALL equal calculatedBaseSalary + totalOvertimePay + totalAllowances - totalDeductions - breakDeductionAmount.

**Validates: Requirements 5.8**

### Property 16: Contract No Overlap Invariant

_For any_ employee, there SHALL NOT exist two contracts with overlapping date ranges (startDate to endDate).

**Validates: Requirements 9.6**

### Property 17: Expiring Contract Detection

_For any_ contract with endDate within 30 days from current date and status = ACTIVE, the contract SHALL appear in the expiring contracts list.

**Validates: Requirements 9.3**

### Property 18: Settings Fallback to Defaults

_For any_ company without specific settings configured, the system SHALL use default values for all required settings without throwing errors.

**Validates: Requirements 7.6**

### Property 19: Report Date Range Filtering

_For any_ report query with startDate and endDate, all returned records SHALL have dates within the specified range (inclusive).

**Validates: Requirements 10.7**

### Property 20: Attendance Filtering Accuracy

_For any_ attendance query with filters (date range, employee, status), all returned records SHALL match ALL specified filter criteria.

**Validates: Requirements 8.6**

## Testing Strategy

### Unit Tests

- Test từng service method với mock dependencies
- Test validation logic cho các request DTOs
- Test calculation logic (overtime, break, salary)
- Test edge cases: empty inputs, boundary values, null handling

### Property-Based Tests (JQwik)

- Minimum 100 iterations per property test
- Use JQwik library for Java property-based testing
- Each property test must reference its design document property
- Tag format: **Feature: flexible-workforce-management, Property {number}: {property_text}**

### Integration Tests

- Test full workflow: check-in → break → check-out → payroll calculation
- Test payroll period workflow: DRAFT → REVIEWING → APPROVED → PAID
- Test shift swap workflow
- Test contract lifecycle: create → expire → renew
