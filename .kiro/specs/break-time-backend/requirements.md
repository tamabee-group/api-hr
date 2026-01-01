# Requirements Document: Break Time Management

## Introduction

Hệ thống quản lý giờ giải lao (Break Time) cho nền tảng HR SaaS multi-tenant. Mỗi công ty có thể cấu hình chính sách giải lao riêng: có/không có giờ giải lao, giải lao có lương/không lương, thời gian giải lao tối thiểu theo quy định pháp luật hoặc theo chính sách công ty.

## Glossary

- **Break_Config**: Cấu hình giờ giải lao của công ty
- **Break_Policy**: Chính sách giải lao (có lương/không lương)
- **Break_Record**: Bản ghi giờ giải lao của nhân viên
- **Minimum_Break**: Thời gian giải lao tối thiểu bắt buộc
- **Legal_Break_Requirement**: Yêu cầu giải lao theo quy định pháp luật
- **Paid_Break**: Giờ giải lao được tính lương
- **Unpaid_Break**: Giờ giải lao không tính lương
- **Break_Deduction**: Khấu trừ thời gian giải lao khỏi giờ làm việc
- **Overtime_Config**: Cấu hình hệ số tăng ca của công ty
- **Night_Hours**: Giờ làm đêm (mặc định 22:00-05:00)
- **Overtime_Multiplier**: Hệ số nhân lương tăng ca

## Requirements

### Requirement 1: Break Configuration Management

**User Story:** As a company admin, I want to configure break time policies for my company, so that the system handles break time correctly according to our policies.

#### Acceptance Criteria

1. THE Break_Config SHALL support enabling/disabling break time for the company
2. THE Break_Config SHALL support configuring break as paid or unpaid
3. THE Break_Config SHALL support configuring minimum break duration in minutes
4. THE Break_Config SHALL support configuring maximum break duration in minutes
5. THE Break_Config SHALL support configuring legal minimum break requirement based on locale
6. THE Break_Config SHALL support configuring custom company break policy that overrides legal minimum
7. WHEN break is disabled, THE System SHALL NOT deduct break time from working hours
8. WHEN break is enabled and unpaid, THE System SHALL deduct break time from working hours
9. WHEN break is enabled and paid, THE System SHALL NOT deduct break time from working hours
10. THE Break_Config SHALL be stored as part of CompanySettings JSON
11. THE Break_Config SHALL support configuring fixed break mode (auto-apply break without tracking)
12. THE Break_Config SHALL support configuring number of break periods per attendance (1, 2, or more)
13. WHEN fixed break mode is enabled, THE System SHALL automatically apply configured break duration without requiring employee to record break
14. THE Break_Config SHALL support configuring break periods with specific start/end times for fixed schedules

### Requirement 2: Legal Break Requirements

**User Story:** As a system, I want to enforce legal break requirements based on locale, so that companies comply with labor laws.

#### Acceptance Criteria

1. THE System SHALL support Japanese labor law break requirements:
   - Working 6-8 hours: minimum 45 minutes break
   - Working over 8 hours: minimum 60 minutes break
2. THE System SHALL support Vietnamese labor law break requirements:
   - Working over 6 hours: minimum 30 minutes break
   - Night shift: minimum 45 minutes break
3. THE System SHALL support configurable break requirements for other locales
4. WHEN company does not configure custom break policy, THE System SHALL use legal minimum based on company locale
5. WHEN company configures custom break policy, THE System SHALL use the greater of (custom policy, legal minimum)
6. THE System SHALL validate that configured break duration meets legal minimum

### Requirement 3: Break Time in Work Schedule

**User Story:** As a company admin, I want to configure break time in work schedules, so that employees know when they should take breaks.

#### Acceptance Criteria

1. THE Work_Schedule SHALL support configuring break start time and end time
2. THE Work_Schedule SHALL support configuring multiple break periods per day (e.g., morning break, lunch break, afternoon break)
3. THE Work_Schedule SHALL support flexible break (employee chooses when to take break within working hours)
4. THE Work_Schedule SHALL support fixed break (specific time slot for break)
5. WHEN break is fixed, THE System SHALL validate that break time falls within working hours
6. THE Work_Schedule SHALL calculate total break duration from configured break periods
7. THE Work_Schedule SHALL validate that total break duration meets minimum requirement
8. THE Work_Schedule SHALL support configuring break periods per schedule type (FIXED schedule has fixed breaks, FLEXIBLE schedule has flexible breaks)
9. FOR FIXED schedule type, THE System SHALL auto-apply configured break periods without requiring employee tracking

### Requirement 9: Night Shift Break Handling

**User Story:** As a company admin, I want to configure break time for night shifts that span across midnight, so that employees working overnight have proper breaks.

#### Acceptance Criteria

1. THE System SHALL support work schedules that span across midnight (e.g., 17:00 to 07:00 next day)
2. WHEN work schedule spans midnight, THE System SHALL correctly calculate total working hours
3. WHEN work schedule spans midnight, THE System SHALL correctly place break periods within the shift
4. THE Break_Config SHALL support configuring night shift break rules separately from day shift
5. THE System SHALL detect if a shift is a night shift based on configured night hours (e.g., 22:00-05:00)
6. WHEN shift is detected as night shift, THE System SHALL apply night shift break requirements
7. THE System SHALL support break periods that span across midnight (e.g., break from 23:30 to 00:30)
8. WHEN calculating break compliance for night shift, THE System SHALL use night shift minimum requirements
9. THE Break_Record SHALL correctly associate with the work date (start date of the shift)
10. THE System SHALL handle timezone correctly for night shift break calculations

### Requirement 4: Break Time Recording

**User Story:** As an employee, I want to record my break time, so that my actual break duration is tracked.

#### Acceptance Criteria

1. WHEN break tracking is enabled, THE System SHALL allow employees to record break start time
2. WHEN break tracking is enabled, THE System SHALL allow employees to record break end time
3. THE System SHALL calculate actual break duration from break start and end times
4. THE System SHALL support multiple break records per day (based on company config)
5. IF actual break is less than minimum required, THE System SHALL flag the attendance record
6. IF actual break exceeds maximum allowed, THE System SHALL cap break duration at maximum
7. THE Break_Record SHALL store original and capped break duration for audit
8. WHEN fixed break mode is enabled for the company/schedule, THE System SHALL NOT require employees to record breaks
9. WHEN fixed break mode is enabled, THE System SHALL auto-create break records based on configured break periods

### Requirement 5: Break Time Impact on Working Hours

**User Story:** As a system, I want to correctly calculate working hours considering break time, so that payroll is accurate.

#### Acceptance Criteria

1. WHEN break is unpaid, THE Working_Hours SHALL equal (checkout - checkin - break_duration)
2. WHEN break is paid, THE Working_Hours SHALL equal (checkout - checkin)
3. WHEN break tracking is disabled, THE System SHALL use configured default break duration
4. WHEN break tracking is enabled, THE System SHALL use actual recorded break duration
5. THE System SHALL NOT count break time as overtime
6. FOR ALL working hours calculations, the formula SHALL be consistent with break policy

### Requirement 6: Break Time Impact on Payroll

**User Story:** As a system, I want to correctly calculate payroll considering break time, so that employees are paid correctly.

#### Acceptance Criteria

1. WHEN break is unpaid, THE Payroll_Service SHALL NOT include break time in paid hours
2. WHEN break is paid, THE Payroll_Service SHALL include break time in paid hours
3. WHEN employee takes less break than minimum required, THE System SHALL still deduct minimum break (for unpaid breaks)
4. WHEN employee takes more break than maximum allowed, THE System SHALL deduct actual break up to maximum (for unpaid breaks)
5. THE Payroll_Record SHALL store break duration and break policy for audit
6. FOR ALL payroll calculations, break handling SHALL be consistent with company break policy

### Requirement 10: Break Time and Overtime Integration

**User Story:** As a system, I want to correctly calculate overtime considering break time, so that employees are paid correctly for overtime work.

#### Acceptance Criteria

1. THE System SHALL calculate net working hours (after break deduction) before determining overtime
2. WHEN net working hours exceed standard hours (e.g., 8 hours), THE System SHALL calculate regular overtime
3. THE System SHALL NOT include break time in overtime hours
4. WHEN calculating night overtime, THE System SHALL exclude break periods that fall within night hours
5. THE Overtime_Calculator SHALL receive net working hours (after break) as input
6. FOR ALL overtime calculations, break time SHALL be deducted first before overtime determination
7. THE System SHALL correctly calculate overtime for overnight shifts (e.g., 17:00 to 07:00)
8. WHEN overnight shift has break, THE System SHALL correctly split working hours before/after midnight for overtime calculation

### Requirement 11: Night Shift Overtime Calculation

**User Story:** As a system, I want to correctly calculate night overtime for shifts that span across midnight, so that employees are paid correctly.

#### Acceptance Criteria

1. THE System SHALL identify night hours based on company configuration (default: 22:00-05:00)
2. WHEN employee works during night hours, THE System SHALL calculate night overtime separately
3. THE Night_Overtime_Rate SHALL be applied to hours worked during night hours (excluding breaks)
4. FOR overnight shifts (e.g., 17:00 to 07:00), THE System SHALL split hours into:
   - Regular hours: 17:00-22:00 (5 hours)
   - Night hours: 22:00-05:00 (7 hours)
   - Morning hours: 05:00-07:00 (2 hours)
5. WHEN break falls within night hours, THE System SHALL deduct break from night hours
6. THE System SHALL use default overtime multipliers based on Japanese labor law:
   - Regular overtime: 1.25x (beyond 8 hours during day)
   - Night work: 1.25x (work during night hours 22:00-05:00)
   - Night overtime: 1.25x + 0.25x = 1.50x (overtime during night hours)
7. THE Payroll_Record SHALL store breakdown of regular hours, night hours, and overtime hours

### Requirement 12: Configurable Overtime Multipliers

**User Story:** As a company admin, I want to configure custom overtime multipliers for my company, so that we can offer better rates than legal minimum.

#### Acceptance Criteria

1. THE Overtime_Config SHALL support configuring regular overtime multiplier (default: 1.25)
2. THE Overtime_Config SHALL support configuring night work multiplier (default: 1.25)
3. THE Overtime_Config SHALL support configuring night overtime multiplier (default: 1.50)
4. THE Overtime_Config SHALL support configuring holiday overtime multiplier (default: 1.35)
5. THE Overtime_Config SHALL support configuring holiday night overtime multiplier (default: 1.60)
6. THE System SHALL validate that custom multipliers are NOT below legal minimum
7. WHEN company does not configure custom multipliers, THE System SHALL use legal minimum based on locale
8. THE Overtime_Config SHALL be stored as part of CompanySettings JSON
9. THE System SHALL support different legal minimums for different locales:
   - Japan: Regular OT 1.25x, Night 1.25x, Night OT 1.50x, Holiday 1.35x
   - Vietnam: Regular OT 1.50x, Night 1.30x, Night OT 1.95x, Holiday 2.00x
10. WHEN calculating payroll, THE System SHALL use the configured multipliers from Overtime_Config

### Requirement 7: Break Time Validation

**User Story:** As a system, I want to validate break time configurations and records, so that data integrity is maintained.

#### Acceptance Criteria

1. THE System SHALL validate that break start time is before break end time
2. THE System SHALL validate that break duration is non-negative
3. THE System SHALL validate that minimum break does not exceed maximum break
4. THE System SHALL validate that configured break meets legal minimum
5. THE System SHALL validate that break time falls within working hours
6. WHEN validation fails, THE System SHALL return appropriate error message

### Requirement 8: Break Time Reporting

**User Story:** As a company admin, I want to view break time reports, so that I can monitor employee break patterns.

#### Acceptance Criteria

1. THE System SHALL generate daily break time reports per employee
2. THE System SHALL generate monthly break time summary reports
3. THE Report SHALL include: total break duration, average break duration, break compliance rate
4. THE Report SHALL flag employees who consistently take less than minimum break
5. THE Report SHALL flag employees who consistently exceed maximum break
