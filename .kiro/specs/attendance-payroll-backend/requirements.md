# Requirements Document

## Introduction

Hệ thống chấm công và tính lương linh hoạt cho nền tảng HR SaaS multi-tenant. Mỗi doanh nghiệp có thể tuỳ chỉnh hoàn toàn các quy tắc chấm công, tính lương, tăng ca, phụ cấp và khấu trừ theo nhu cầu riêng. Hệ thống được thiết kế theo kiến trúc Configuration-driven để dễ dàng mở rộng và bảo trì.

## Glossary

- **Company_Settings_Service**: Service quản lý cấu hình của từng công ty
- **Attendance_Service**: Service xử lý chấm công (check-in/check-out)
- **Payroll_Service**: Service tính lương
- **Overtime_Calculator**: Module tính toán giờ tăng ca
- **Allowance_Calculator**: Module tính toán phụ cấp và thưởng
- **Deduction_Calculator**: Module tính toán khấu trừ
- **Work_Schedule**: Lịch làm việc của nhân viên
- **Attendance_Record**: Bản ghi chấm công
- **Payroll_Period**: Kỳ tính lương (tháng)
- **Rounding_Rule**: Quy tắc làm tròn giờ

## Requirements

### Requirement 1: Company Settings Management

**User Story:** As a company admin, I want to configure attendance and payroll rules for my company, so that the system calculates correctly according to our policies.

#### Acceptance Criteria

1. THE Company_Settings_Service SHALL store attendance configuration as JSON for each company
2. THE Company_Settings_Service SHALL store payroll configuration as JSON for each company
3. THE Company_Settings_Service SHALL store overtime configuration as JSON for each company
4. THE Company_Settings_Service SHALL store allowance configuration as JSON for each company
5. THE Company_Settings_Service SHALL store deduction configuration as JSON for each company
6. WHEN a company is created, THE Company_Settings_Service SHALL initialize default configurations
7. WHEN a company admin updates settings, THE Company_Settings_Service SHALL validate and persist the changes
8. THE Company_Settings_Service SHALL provide default values for all configuration fields

### Requirement 2: Work Schedule Management

**User Story:** As a company admin, I want to define work schedules for employees, so that the system knows when they should work.

#### Acceptance Criteria

1. THE Work_Schedule SHALL support fixed schedule (same hours every day)
2. THE Work_Schedule SHALL support flexible schedule (different hours per day of week)
3. THE Work_Schedule SHALL support shift-based schedule (multiple shifts per day)
4. WHEN creating a work schedule, THE System SHALL validate that start time is before end time
5. THE Work_Schedule SHALL support break time configuration
6. THE Work_Schedule SHALL be assignable to individual employees or groups
7. WHEN no schedule is assigned to an employee, THE System SHALL use the company default schedule

### Requirement 3: Attendance Recording

**User Story:** As an employee, I want to record my attendance, so that my working hours are tracked accurately.

#### Acceptance Criteria

1. WHEN an employee checks in, THE Attendance_Service SHALL record the check-in time
2. WHEN an employee checks out, THE Attendance_Service SHALL record the check-out time
3. IF the company enables time rounding, THEN THE Attendance_Service SHALL apply rounding rules to check-in/check-out times
4. THE Attendance_Service SHALL calculate actual working hours from check-in and check-out times
5. THE Attendance_Service SHALL detect late arrivals based on work schedule and grace period
6. THE Attendance_Service SHALL detect early departures based on work schedule and grace period
7. IF the company requires device registration, THEN THE Attendance_Service SHALL validate the device before recording
8. IF the company requires geo-location, THEN THE Attendance_Service SHALL validate location within geo-fence radius
9. WHEN an attendance record is modified, THE Attendance_Service SHALL log the change with reason

### Requirement 4: Time Rounding Rules

**User Story:** As a company admin, I want to configure time rounding rules, so that attendance times are rounded according to our policy.

#### Acceptance Criteria

1. THE Rounding_Rule SHALL support rounding intervals: 5, 10, 15, 30, 60 minutes
2. THE Rounding_Rule SHALL support rounding directions: UP, DOWN, NEAREST
3. THE Rounding_Rule SHALL allow different rules for check-in and check-out
4. WHEN rounding is applied, THE System SHALL preserve the original time for audit purposes
5. FOR ALL attendance records, applying rounding then calculating hours SHALL produce consistent results

### Requirement 5: Overtime Calculation

**User Story:** As a company admin, I want to configure overtime rules, so that overtime hours and pay are calculated correctly.

#### Acceptance Criteria

1. THE Overtime_Calculator SHALL calculate regular overtime (beyond standard hours)
2. THE Overtime_Calculator SHALL calculate night overtime (work during night hours)
3. THE Overtime_Calculator SHALL calculate holiday overtime (work on holidays)
4. THE Overtime_Calculator SHALL calculate weekend overtime (work on weekends)
5. THE Overtime_Calculator SHALL apply different multiplier rates for each overtime type
6. IF the company sets maximum overtime hours, THEN THE Overtime_Calculator SHALL cap calculations at the limit
7. IF overtime requires approval, THEN THE System SHALL only count approved overtime hours
8. FOR ALL overtime calculations, the sum of overtime types SHALL equal total overtime hours

### Requirement 6: Salary Type Support

**User Story:** As a company admin, I want to support different salary types, so that employees can be paid according to their contract.

#### Acceptance Criteria

1. THE Payroll_Service SHALL support monthly salary (fixed amount per month)
2. THE Payroll_Service SHALL support daily salary (rate × working days)
3. THE Payroll_Service SHALL support hourly salary (rate × working hours)
4. WHEN calculating salary, THE Payroll_Service SHALL use the employee's configured salary type
5. THE Payroll_Service SHALL support salary rounding rules (round to nearest yen)
6. FOR ALL salary calculations, base salary plus adjustments SHALL equal gross salary

### Requirement 7: Allowance and Bonus Management

**User Story:** As a company admin, I want to configure allowances and bonuses, so that employees receive additional compensation.

#### Acceptance Criteria

1. THE Allowance_Calculator SHALL support fixed allowances (same amount every period)
2. THE Allowance_Calculator SHALL support conditional allowances (based on attendance, performance)
3. THE Allowance_Calculator SHALL support one-time bonuses
4. THE Allowance_Calculator SHALL support transportation allowance
5. THE Allowance_Calculator SHALL support meal allowance
6. THE Allowance_Calculator SHALL support housing allowance
7. WHEN calculating allowances, THE System SHALL apply eligibility rules per allowance type
8. FOR ALL allowance calculations, the sum of individual allowances SHALL equal total allowances

### Requirement 8: Deduction Management

**User Story:** As a company admin, I want to configure deductions, so that required amounts are subtracted from salary.

#### Acceptance Criteria

1. THE Deduction_Calculator SHALL support fixed deductions (same amount every period)
2. THE Deduction_Calculator SHALL support percentage-based deductions
3. THE Deduction_Calculator SHALL support late arrival penalties
4. THE Deduction_Calculator SHALL support early departure penalties
5. THE Deduction_Calculator SHALL support absence deductions
6. THE Deduction_Calculator SHALL support tax deductions
7. THE Deduction_Calculator SHALL support insurance deductions
8. WHEN calculating deductions, THE System SHALL apply rules in configured order
9. FOR ALL deduction calculations, the sum of individual deductions SHALL equal total deductions

### Requirement 9: Payroll Calculation

**User Story:** As a company admin, I want to calculate payroll for a period, so that employees are paid correctly.

#### Acceptance Criteria

1. WHEN calculating payroll, THE Payroll_Service SHALL aggregate attendance records for the period
2. WHEN calculating payroll, THE Payroll_Service SHALL calculate base salary according to salary type
3. WHEN calculating payroll, THE Payroll_Service SHALL calculate overtime pay
4. WHEN calculating payroll, THE Payroll_Service SHALL calculate total allowances
5. WHEN calculating payroll, THE Payroll_Service SHALL calculate total deductions
6. THE Payroll_Service SHALL calculate net salary as: base + overtime + allowances - deductions
7. THE Payroll_Service SHALL generate payroll records for each employee
8. THE Payroll_Service SHALL support payroll preview before finalization
9. WHEN payroll is finalized, THE Payroll_Service SHALL lock the records from modification
10. FOR ALL payroll calculations, serializing then deserializing a payroll record SHALL produce an equivalent record

### Requirement 10: Payroll Finalization and Payment

**User Story:** As a company admin, I want to finalize payroll and process payments, so that employees receive their salaries on time.

#### Acceptance Criteria

1. WHEN payroll is finalized, THE System SHALL generate payslips for each employee
2. THE System SHALL support marking payroll as PAID after payment processing
3. WHEN payroll status changes to PAID, THE System SHALL send salary notification to each employee
4. THE Salary_Notification SHALL include: net salary, breakdown of earnings and deductions, payment date
5. THE System SHALL support bulk payment processing for all employees in a period
6. THE System SHALL track payment status per employee (PENDING, PAID, FAILED)
7. WHEN payment fails, THE System SHALL allow retry for individual employees
8. THE Salary_Notification email SHALL be sent in the employee's preferred language (vi, en, ja)

### Requirement 11: Attendance Adjustment Request

**User Story:** As an employee, I want to request attendance time corrections, so that my manager can approve changes to my check-in/check-out times.

#### Acceptance Criteria

1. WHEN an employee submits an adjustment request, THE System SHALL record the original time, requested time, and reason
2. THE Adjustment_Request SHALL require manager approval before applying changes
3. WHEN a manager approves an adjustment request, THE Attendance_Service SHALL update the attendance record with the new times
4. WHEN a manager rejects an adjustment request, THE System SHALL notify the employee with the rejection reason
5. THE System SHALL maintain history of all adjustment requests for audit purposes
6. THE Adjustment_Request SHALL support adjusting check-in time, check-out time, or both
7. WHEN an adjustment is applied, THE System SHALL recalculate working hours, overtime, and late/early status
8. THE Adjustment approval/rejection notification email SHALL be sent in the employee's preferred language (vi, en, ja)

### Requirement 12: Work Schedule Selection by Employee

**User Story:** As an employee, I want to select my preferred work schedule from available options, so that I can work at times that suit me.

#### Acceptance Criteria

1. WHEN a company enables flexible scheduling, THE System SHALL allow employees to select from available schedules
2. THE System SHALL suggest schedules based on employee's past selections
3. THE System SHALL suggest schedules recommended by the company for the employee's role/department
4. WHEN an employee selects a schedule, THE System SHALL require manager approval if configured
5. THE System SHALL display schedule availability and conflicts before selection
6. THE System SHALL support schedule selection for specific date ranges
7. WHEN schedule selection is approved, THE System SHALL update the employee's effective schedule

### Requirement 13: Plan-Based Feature Access

**User Story:** As a system, I want to restrict features based on company's subscription plan, so that companies only access features they paid for.

#### Acceptance Criteria

1. THE System SHALL check company's plan before allowing access to attendance features
2. THE System SHALL check company's plan before allowing access to payroll features
3. THE System SHALL check company's plan before allowing access to advanced features (geo-location, device registration)
4. WHEN a company attempts to use a feature not in their plan, THE System SHALL return appropriate error
5. THE System SHALL support feature flags per plan (attendance, payroll, overtime, leave management, reports)
6. WHEN company's plan changes, THE System SHALL immediately update feature access
7. THE System SHALL log all plan-based access denials for analytics

### Requirement 14: Holiday and Leave Management

**User Story:** As a company admin, I want to manage holidays and employee leaves, so that attendance and payroll are calculated correctly.

#### Acceptance Criteria

1. THE System SHALL support company-defined holidays
2. THE System SHALL support national holidays by locale
3. THE System SHALL support paid leave types (annual, sick, personal)
4. THE System SHALL support unpaid leave types
5. WHEN an employee is on leave, THE Attendance_Service SHALL not require check-in/check-out
6. WHEN calculating payroll, THE Payroll_Service SHALL apply leave rules to salary calculation
7. THE System SHALL track leave balances per employee
8. THE Leave approval/rejection notification email SHALL be sent in the employee's preferred language (vi, en, ja)

### Requirement 15: Reporting and Export

**User Story:** As a company admin, I want to generate attendance and payroll reports, so that I can review and export data.

#### Acceptance Criteria

1. THE System SHALL generate daily attendance reports
2. THE System SHALL generate monthly attendance summary reports
3. THE System SHALL generate payroll reports per period
4. THE System SHALL export reports in CSV format
5. THE System SHALL export reports in PDF format
6. WHEN generating reports, THE System SHALL filter by date range, department, or employee

### Requirement 16: Audit Trail

**User Story:** As a company admin, I want to track all changes to attendance and payroll data, so that I can audit modifications.

#### Acceptance Criteria

1. WHEN attendance records are created or modified, THE System SHALL log the action with timestamp and user
2. WHEN payroll records are created or modified, THE System SHALL log the action with timestamp and user
3. WHEN company settings are modified, THE System SHALL log the action with timestamp and user
4. THE System SHALL store before and after values for all modifications
5. THE System SHALL provide audit log query API with filtering

### Requirement 17: Email Notification Templates

**User Story:** As a system, I want to send email notifications in the user's preferred language, so that users can understand the content.

#### Acceptance Criteria

1. THE System SHALL support email templates in Vietnamese (vi), English (en), and Japanese (ja)
2. THE Salary_Notification_Template SHALL include: employee name, period, net salary, earnings breakdown, deductions breakdown, payment date
3. THE Adjustment_Approved_Template SHALL include: employee name, work date, original times, approved times, approver name
4. THE Adjustment_Rejected_Template SHALL include: employee name, work date, original times, requested times, rejection reason, approver name
5. THE Leave_Approved_Template SHALL include: employee name, leave type, date range, approver name
6. THE Leave_Rejected_Template SHALL include: employee name, leave type, date range, rejection reason, approver name
7. WHEN sending notification, THE System SHALL use the employee's locale setting to select the appropriate template
8. THE System SHALL fallback to English template if the user's preferred language template is not available
9. THE Email templates SHALL use inline CSS and be mobile-responsive (max-width 600px)
10. THE Email templates SHALL use the brand color (#00b1ce) for consistency
