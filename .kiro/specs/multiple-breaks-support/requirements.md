# Requirements Document

## Introduction

Mở rộng hệ thống Break Time Management để hỗ trợ nhiều lần nghỉ giải lao trong 1 ngày làm việc. Hiện tại hệ thống chỉ hỗ trợ 1 break record per attendance record, cần nâng cấp để hỗ trợ nhiều breaks (ví dụ: nghỉ trưa, nghỉ giữa chiều, nghỉ ngắn).

## Glossary

- **Break_Record**: Bản ghi giờ giải lao của nhân viên, bao gồm thời gian bắt đầu và kết thúc
- **Attendance_Record**: Bản ghi chấm công của nhân viên trong 1 ngày
- **Break_Session**: Một phiên nghỉ giải lao cụ thể trong ngày (có thể có nhiều sessions)
- **Adjustment_Request**: Yêu cầu điều chỉnh thời gian chấm công/giải lao
- **Total_Break_Duration**: Tổng thời gian nghỉ giải lao trong ngày (tính từ tất cả break sessions)

## Requirements

### Requirement 1: Hỗ trợ nhiều Break Sessions trong ngày

**User Story:** As an employee, I want to record multiple break sessions per day, so that I can accurately track all my break times (lunch break, afternoon break, short breaks).

#### Acceptance Criteria

1. WHEN an employee starts a new break WHILE already having completed breaks for the day, THE Break_Service SHALL create a new Break_Record for that session
2. WHEN an employee has multiple break records in a day, THE System SHALL calculate Total_Break_Duration as the sum of all break durations
3. THE Break_Record SHALL include a `breakNumber` field to identify the sequence of breaks (1st, 2nd, 3rd, etc.)
4. WHEN displaying break information, THE System SHALL show all break sessions ordered by breakNumber

### Requirement 2: Điều chỉnh Break Record cụ thể

**User Story:** As an employee, I want to request adjustment for a specific break session, so that I can correct errors in individual break records.

#### Acceptance Criteria

1. WHEN creating an adjustment request for breaks, THE CreateAdjustmentRequest SHALL include an optional `breakRecordId` field to specify which break to adjust
2. IF `breakRecordId` is provided, THE Adjustment_Service SHALL only adjust that specific break record
3. IF `breakRecordId` is null AND break fields are provided, THE Adjustment_Service SHALL reject the request with error "Must specify breakRecordId when adjusting break times"
4. WHEN an adjustment request is approved, THE System SHALL update only the specified break record

### Requirement 3: Validation cho Multiple Breaks

**User Story:** As a system administrator, I want the system to validate break times, so that breaks don't overlap and are within working hours.

#### Acceptance Criteria

1. WHEN an employee starts a new break, THE System SHALL verify no other break is currently active (breakEnd is null)
2. WHEN creating a new break, THE System SHALL verify the break start time doesn't overlap with existing completed breaks
3. IF break times overlap, THE System SHALL reject with error "Break times cannot overlap with existing breaks"
4. THE System SHALL allow configurable maximum number of breaks per day via Company_Settings

### Requirement 4: Báo cáo tổng hợp Multiple Breaks

**User Story:** As a manager, I want to see aggregated break reports, so that I can monitor total break time across all sessions.

#### Acceptance Criteria

1. WHEN generating daily break report, THE Report_Service SHALL aggregate all break sessions for each employee
2. THE DailyBreakReportResponse SHALL include `breakSessions` list containing all individual breaks
3. THE DailyBreakReportResponse SHALL include `totalBreakMinutes` calculated from all sessions
4. WHEN calculating payroll, THE Payroll_Calculator SHALL use Total_Break_Duration from all break sessions

### Requirement 5: API Response cho Multiple Breaks

**User Story:** As a frontend developer, I want clear API responses for multiple breaks, so that I can display break information correctly.

#### Acceptance Criteria

1. THE AttendanceRecordResponse SHALL include a `breaks` list instead of single break fields
2. WHEN fetching attendance record, THE System SHALL return all associated break records ordered by breakNumber
3. THE AdjustmentRequestResponse SHALL include `breakRecordId` to identify which break was adjusted
4. WHEN listing adjustment requests, THE System SHALL show the break session number being adjusted
