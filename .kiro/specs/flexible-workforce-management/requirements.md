# Requirements Document

## Introduction

Hệ thống quản lý nhân sự linh hoạt cho doanh nghiệp tự do (quán ăn, nhà hàng, cafe) với các đặc điểm:

- Mỗi nhân viên có loại hình lương riêng (chính thức, thời vụ, theo ca)
- Mức lương và quy định riêng cho từng người
- Hệ thống ca làm việc linh hoạt, có thể đổi ca
- Phụ cấp và khấu trừ cá nhân hóa
- Manager có thể xem và chỉnh sửa trước khi chốt lương
- API chấm công thống nhất (attendance + break trong cùng 1 luồng)

## Glossary

- **Attendance_System**: Hệ thống chấm công thống nhất bao gồm check-in/out và break time
- **Shift_Manager**: Module quản lý ca làm việc và đổi ca
- **Employee_Salary_Config**: Cấu hình lương riêng cho từng nhân viên
- **Payroll_Period**: Kỳ lương với workflow DRAFT → REVIEWING → APPROVED → PAID
- **Allowance_Assignment**: Phụ cấp được gán cho từng nhân viên cụ thể
- **Deduction_Assignment**: Khấu trừ được gán cho từng nhân viên cụ thể
- **Shift_Swap_Request**: Yêu cầu đổi ca giữa các nhân viên
- **Company_Settings**: Cấu hình chung của công ty (attendance, payroll, overtime, break, allowance, deduction)

## Requirements

### Requirement 1: Unified Attendance API

**User Story:** As a developer, I want a unified attendance API that returns both check-in/out and break data together, so that the frontend can display complete attendance information in a single request.

#### Acceptance Criteria

1. WHEN an employee calls GET /api/employee/attendance/today, THE Attendance_System SHALL return attendance record with embedded break records in a single response
2. WHEN an employee calls GET /api/employee/attendance/{date}, THE Attendance_System SHALL return attendance record with all break records for that date
3. WHEN an employee performs check-in, THE Attendance_System SHALL apply company settings (rounding, grace period, geo-location) correctly
4. WHEN an employee performs check-out, THE Attendance_System SHALL calculate working minutes, overtime, late/early leave based on company settings
5. WHEN an employee starts a break, THE Attendance_System SHALL validate against company break config (max breaks per day, break periods)
6. WHEN an employee ends a break, THE Attendance_System SHALL calculate effective break minutes based on min/max settings
7. IF company settings require device registration, THEN THE Attendance_System SHALL validate device ID before allowing check-in/out
8. IF company settings require geo-location, THEN THE Attendance_System SHALL validate location within geo-fence radius

### Requirement 2: Shift Management System

**User Story:** As a company admin, I want to create and manage work shifts, so that I can assign employees to different shifts and allow shift swapping.

#### Acceptance Criteria

1. WHEN a manager creates a shift template, THE Shift_Manager SHALL store shift name, start time, end time, break minutes, and multiplier
2. WHEN a manager assigns an employee to a shift, THE Shift_Manager SHALL create a shift assignment with date and status
3. WHEN an employee requests to swap shifts with another employee, THE Shift_Manager SHALL create a swap request with PENDING status
4. WHEN a manager approves a shift swap, THE Shift_Manager SHALL update both employees' shift assignments and mark request as APPROVED
5. WHEN a manager rejects a shift swap, THE Shift_Manager SHALL mark request as REJECTED with reason
6. WHEN viewing shift assignments, THE Shift_Manager SHALL return assignments with employee names and shift details
7. THE Shift_Manager SHALL prevent assigning overlapping shifts to the same employee on the same date

### Requirement 3: Employee Salary Configuration

**User Story:** As a company admin, I want to configure individual salary settings for each employee, so that I can handle different employment types (full-time, part-time, seasonal).

#### Acceptance Criteria

1. WHEN creating employee salary config, THE Employee_Salary_Config SHALL support MONTHLY, DAILY, HOURLY, and SHIFT_BASED salary types
2. WHEN setting salary for MONTHLY type, THE Employee_Salary_Config SHALL store monthly salary amount
3. WHEN setting salary for DAILY type, THE Employee_Salary_Config SHALL store daily rate
4. WHEN setting salary for HOURLY type, THE Employee_Salary_Config SHALL store hourly rate
5. WHEN setting salary for SHIFT_BASED type, THE Employee_Salary_Config SHALL store rate per shift
6. WHEN updating salary config, THE Employee_Salary_Config SHALL create new record with effectiveFrom date and close previous record
7. THE Employee_Salary_Config SHALL return the active salary config based on current date

### Requirement 4: Individual Allowance & Deduction Assignment

**User Story:** As a company admin, I want to assign specific allowances and deductions to individual employees, so that each employee can have personalized compensation.

#### Acceptance Criteria

1. WHEN assigning an allowance to an employee, THE Allowance_Assignment SHALL store employee ID, allowance type, amount, and effective dates
2. WHEN assigning a deduction to an employee, THE Deduction_Assignment SHALL store employee ID, deduction type, amount/percentage, and effective dates
3. WHEN calculating payroll, THE Payroll_Period SHALL apply only active allowances/deductions for each employee
4. WHEN an allowance/deduction is deactivated, THE system SHALL set effectiveTo date without deleting the record
5. THE system SHALL allow overriding company default allowance/deduction amounts for specific employees
6. WHEN listing employee allowances/deductions, THE system SHALL return both company defaults and individual overrides

### Requirement 5: Payroll Period Workflow

**User Story:** As a company admin, I want to manage payroll periods with a review workflow, so that I can verify and adjust salaries before finalizing.

#### Acceptance Criteria

1. WHEN a payroll period is created, THE Payroll_Period SHALL have status DRAFT
2. WHEN calculating payroll for a period, THE Payroll_Period SHALL generate payroll items for all active employees
3. WHEN a manager reviews payroll, THE Payroll_Period SHALL allow viewing all payroll items with details
4. WHEN a manager adjusts a payroll item, THE Payroll_Period SHALL store adjustment amount and reason
5. WHEN a manager approves payroll period, THE Payroll_Period SHALL change status to APPROVED and lock all items
6. WHEN payroll is paid, THE Payroll_Period SHALL change status to PAID with payment timestamp
7. IF payroll period is APPROVED or PAID, THEN THE Payroll_Period SHALL prevent any modifications
8. THE Payroll_Period SHALL calculate gross salary = base salary + overtime + allowances - deductions

### Requirement 6: Payroll Calculation Engine

**User Story:** As a system, I want to calculate payroll accurately based on employee salary type and company settings, so that employees receive correct compensation.

#### Acceptance Criteria

1. WHEN calculating MONTHLY salary, THE system SHALL prorate based on actual working days vs standard working days
2. WHEN calculating DAILY salary, THE system SHALL multiply daily rate by actual working days
3. WHEN calculating HOURLY salary, THE system SHALL multiply hourly rate by actual working hours
4. WHEN calculating SHIFT_BASED salary, THE system SHALL multiply shift rate by number of shifts worked
5. WHEN calculating overtime, THE system SHALL apply company overtime rates (regular, night, holiday, weekend)
6. WHEN calculating break deductions for UNPAID break type, THE system SHALL deduct break time from working hours
7. WHEN calculating late/early leave penalties, THE system SHALL apply company deduction config
8. THE system SHALL apply all active individual allowances and deductions for each employee

### Requirement 7: Company Settings Validation

**User Story:** As a system, I want to validate and apply company settings consistently across all attendance and payroll operations, so that no settings are missed.

#### Acceptance Criteria

1. WHEN processing check-in, THE system SHALL load and apply attendance config (rounding, grace period, device, location)
2. WHEN processing check-out, THE system SHALL load and apply attendance config for time calculations
3. WHEN processing break start/end, THE system SHALL load and apply break config (type, min/max, legal minimum)
4. WHEN calculating overtime, THE system SHALL load and apply overtime config (rates, night hours, limits)
5. WHEN calculating payroll, THE system SHALL load and apply payroll config (pay day, cutoff, rounding)
6. IF any required config is missing, THEN THE system SHALL use default values and log warning
7. THE system SHALL cache company settings to avoid repeated database queries within same request

### Requirement 8: Attendance Record Detail API

**User Story:** As a manager, I want to view detailed attendance records with all related data, so that I can make informed decisions about adjustments and payroll.

#### Acceptance Criteria

1. WHEN viewing attendance record detail, THE system SHALL return check-in/out times (original and rounded)
2. WHEN viewing attendance record detail, THE system SHALL return all break records with start/end times
3. WHEN viewing attendance record detail, THE system SHALL return calculated values (working minutes, overtime, late, early leave)
4. WHEN viewing attendance record detail, THE system SHALL return applied settings snapshot (rounding config, break config)
5. WHEN viewing attendance record detail, THE system SHALL return adjustment history if any
6. THE system SHALL support filtering attendance records by date range, employee, and status

### Requirement 9: Employment Contract Management

**User Story:** As a company admin, I want to manage employment contracts for each employee, so that I can track contract terms, renewal dates, and employment type.

#### Acceptance Criteria

1. WHEN creating an employment contract, THE system SHALL store employee ID, contract type (FULL_TIME, PART_TIME, SEASONAL, CONTRACT), start date, and end date
2. WHEN a contract is created, THE system SHALL link it to the employee's salary configuration
3. WHEN a contract is about to expire (within 30 days), THE system SHALL flag it for renewal notification
4. WHEN viewing employee details, THE system SHALL return current active contract information
5. WHEN a contract ends, THE system SHALL automatically update employee status to INACTIVE if no new contract exists
6. THE system SHALL prevent creating overlapping contracts for the same employee
7. WHEN listing contracts, THE system SHALL support filtering by status (ACTIVE, EXPIRED, PENDING), contract type, and date range

### Requirement 10: Attendance & Payroll Reports

**User Story:** As a company admin, I want to generate comprehensive reports on attendance and payroll, so that I can analyze workforce performance and costs.

#### Acceptance Criteria

1. WHEN generating attendance summary report, THE system SHALL return total working days, present days, absent days, late count, early leave count per employee
2. WHEN generating overtime report, THE system SHALL return overtime hours breakdown (regular, night, holiday, weekend) per employee
3. WHEN generating break compliance report, THE system SHALL return break time statistics and compliance rate per employee
4. WHEN generating payroll summary report, THE system SHALL return total gross salary, net salary, allowances, deductions per period
5. WHEN generating cost analysis report, THE system SHALL return labor cost breakdown by department, contract type, and salary type
6. THE system SHALL support exporting reports in CSV and PDF formats
7. THE system SHALL support date range filtering for all reports
8. WHEN generating shift utilization report, THE system SHALL return shift coverage, swap statistics, and unfilled shifts
