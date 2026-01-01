# Implementation Plan: Attendance & Payroll Backend System

## Overview

- Triển khai hệ thống chấm công và tính lương linh hoạt cho nền tảng HR SaaS multi-tenant. Sử dụng kiến trúc Configuration-driven với JSON configs cho mỗi công ty.
- Khi KIRO thực hiện task thì hãy phản hồi tôi bằng tiếng Việt.

## Tasks

- [x] 1. Database Schema & Migrations

  - [x] 1.1 Tạo Flyway migration cho company_settings table với JSONB columns

    - Tạo file `V{n}__create_company_settings_table.sql`
    - Columns: companyId, attendanceConfig, payrollConfig, overtimeConfig, allowanceConfig, deductionConfig
    - Index trên companyId
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 Tạo Flyway migration cho work_schedules và work_schedule_assignments tables

    - Tạo file `V{n}__create_work_schedule_tables.sql`
    - work_schedules: companyId, name, type, isDefault, scheduleData (JSONB)
    - work_schedule_assignments: employeeId, scheduleId, effectiveFrom, effectiveTo
    - _Requirements: 2.1, 2.2, 2.3, 2.6_

  - [x] 1.3 Tạo Flyway migration cho attendance_records table

    - Tạo file `V{n}__create_attendance_records_table.sql`
    - Columns cho original/rounded times, working/overtime minutes, device/location info
    - Indexes trên employeeId, companyId, workDate
    - _Requirements: 3.1, 3.2, 4.4_

  - [x] 1.4 Tạo Flyway migration cho payroll_records table

    - Tạo file `V{n}__create_payroll_records_table.sql`
    - Columns cho salary breakdown, allowances/deductions (JSONB), payment tracking
    - Indexes trên employeeId, companyId, year, month
    - _Requirements: 9.7, 10.1, 10.6_

  - [x] 1.5 Tạo Flyway migration cho attendance_adjustment_requests table

    - Tạo file `V{n}__create_adjustment_requests_table.sql`
    - Columns cho original/requested times, status, approval info
    - _Requirements: 11.1, 11.5_

  - [x] 1.6 Tạo Flyway migration cho schedule_selections table

    - Tạo file `V{n}__create_schedule_selections_table.sql`
    - Columns cho employeeId, scheduleId, effectiveFrom, effectiveTo, status
    - _Requirements: 12.1, 12.4_

  - [x] 1.7 Tạo Flyway migration cho holidays và leave tables
    - Tạo file `V{n}__create_holiday_leave_tables.sql`
    - holidays: companyId, date, name, type, isPaid
    - leave_requests: employeeId, leaveType, startDate, endDate, status
    - leave_balances: employeeId, year, leaveType, totalDays, usedDays
    - _Requirements: 14.1, 14.2, 14.3, 14.7_

- [x] 2. Enums & Configuration DTOs

  - [x] 2.1 Tạo các Enums mới

    - ScheduleType, RoundingInterval, RoundingDirection, SalaryType
    - AttendanceStatus, PayrollStatus, PaymentStatus
    - LeaveType, LeaveStatus, HolidayType
    - AdjustmentStatus, SelectionStatus, FeatureCode
    - AllowanceType, DeductionType
    - _Requirements: 2.1, 4.1, 4.2, 6.1_

  - [x] 2.2 Tạo Configuration DTOs với default values

    - AttendanceConfig, RoundingConfig
    - PayrollConfig, OvertimeConfig
    - AllowanceConfig, AllowanceRule
    - DeductionConfig, DeductionRule
    - _Requirements: 1.8_

  - [x] 2.3 Write property test cho Configuration round-trip
    - **Property 1: Configuration Round-Trip Consistency**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**

- [x] 3. Entity Classes

  - [x] 3.1 Tạo CompanySettingsEntity

    - Extends BaseEntity
    - JSONB columns cho các configs
    - _Requirements: 1.1-1.5_

  - [x] 3.2 Tạo WorkScheduleEntity và WorkScheduleAssignmentEntity

    - WorkScheduleEntity với scheduleData JSONB
    - WorkScheduleAssignmentEntity với effectiveFrom/To
    - _Requirements: 2.1-2.6_

  - [x] 3.3 Tạo AttendanceRecordEntity

    - Original và rounded times
    - Working/overtime/late/early minutes
    - Device và location info
    - _Requirements: 3.1-3.8_

  - [x] 3.4 Tạo PayrollRecordEntity

    - Salary breakdown fields
    - Allowances/deductions JSONB
    - Payment tracking fields
    - _Requirements: 9.7, 10.1-10.6_

  - [x] 3.5 Tạo AttendanceAdjustmentRequestEntity

    - Original/requested times
    - Status và approval info
    - _Requirements: 11.1-11.6_

  - [x] 3.6 Tạo ScheduleSelectionEntity

    - Employee schedule selection với approval workflow
    - _Requirements: 12.1-12.6_

  - [x] 3.7 Tạo HolidayEntity, LeaveRequestEntity, LeaveBalanceEntity
    - Holiday management
    - Leave request workflow
    - Leave balance tracking
    - _Requirements: 14.1-14.7_

- [x] 4. Repository Layer

  - [x] 4.1 Tạo CompanySettingsRepository

    - findByCompanyIdAndDeletedFalse
    - _Requirements: 1.1-1.5_

  - [x] 4.2 Tạo WorkScheduleRepository và WorkScheduleAssignmentRepository

    - findByCompanyIdAndDeletedFalse
    - findDefaultByCompanyId
    - findByEmployeeIdAndEffectiveDateRange
    - findAssignmentsByScheduleId
    - _Requirements: 2.1-2.7_

  - [x] 4.3 Tạo AttendanceRecordRepository

    - findByIdAndDeletedFalse
    - findByEmployeeIdAndWorkDate
    - findByCompanyIdAndWorkDateBetween
    - findByEmployeeIdAndWorkDateBetween
    - _Requirements: 3.1-3.9_

  - [x] 4.4 Tạo PayrollRecordRepository

    - findByIdAndDeletedFalse
    - findByCompanyIdAndYearAndMonth
    - findByEmployeeIdAndYearAndMonth
    - findByEmployeeIdOrderByYearDescMonthDesc
    - _Requirements: 9.1-9.10_

  - [x] 4.5 Tạo AttendanceAdjustmentRequestRepository

    - findByIdAndDeletedFalse
    - findPendingByCompanyId
    - findByEmployeeId
    - findByAttendanceRecordId
    - _Requirements: 11.1-11.7_

  - [x] 4.6 Tạo ScheduleSelectionRepository

    - findByIdAndDeletedFalse
    - findPendingByCompanyId
    - findByEmployeeIdOrderByCreatedAtDesc
    - _Requirements: 12.1-12.7_

  - [x] 4.7 Tạo HolidayRepository, LeaveRequestRepository, LeaveBalanceRepository
    - findByIdAndDeletedFalse (Holiday, LeaveRequest)
    - Holiday queries by date range
    - Leave request queries by status
    - Leave balance queries by employee and year
    - _Requirements: 14.1-14.7_

- [x] 5. Calculator Modules

  - [x] 5.1 Implement TimeRoundingCalculator

    - roundTime method với RoundingConfig
    - Support 5/10/15/30/60 minute intervals
    - Support UP/DOWN/NEAREST directions
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 5.2 Write property test cho Time Rounding

    - **Property 5: Time Rounding Determinism**
    - **Validates: Requirements 4.5**

  - [x] 5.3 Implement OvertimeCalculator

    - calculateOvertime method
    - Regular, night, holiday, weekend overtime
    - Apply multiplier rates
    - Cap at maximum limits
    - _Requirements: 5.1-5.7_

  - [x] 5.4 Write property test cho Overtime calculation

    - **Property 10: Overtime Types Sum Invariant**
    - **Property 11: Overtime Cap Enforcement**
    - **Validates: Requirements 5.6, 5.8**

  - [x] 5.5 Implement AllowanceCalculator

    - calculateAllowances method
    - Fixed, conditional, one-time allowances
    - Apply eligibility rules
    - _Requirements: 7.1-7.7_

  - [x] 5.6 Write property test cho Allowance calculation

    - **Property 15: Allowances Sum Invariant**
    - **Validates: Requirements 7.8**

  - [x] 5.7 Implement DeductionCalculator

    - calculateDeductions method
    - Fixed, percentage deductions
    - Late/early penalties
    - Apply in configured order
    - _Requirements: 8.1-8.8_

  - [x] 5.8 Write property test cho Deduction calculation

    - **Property 16: Deductions Sum Invariant**
    - **Validates: Requirements 8.9**

  - [x] 5.9 Implement PayrollCalculator

    - calculatePayroll method
    - Aggregate all calculations
    - Calculate gross and net salary
    - _Requirements: 9.1-9.6_

  - [x] 5.10 Write property tests cho Payroll calculation
    - **Property 14: Gross Salary Invariant**
    - **Property 17: Net Salary Formula**
    - **Validates: Requirements 6.6, 9.6**

- [x] 6. Checkpoint - Calculator Tests

  - Ensure all calculator tests pass, ask the user if questions arise.

- [x] 7. Service Layer - Company Settings

  - [x] 7.1 Tạo ICompanySettingsService interface

    - getSettings, updateAttendanceConfig, updatePayrollConfig
    - updateOvertimeConfig, updateAllowanceConfig, updateDeductionConfig
    - initializeDefaultSettings
    - _Requirements: 1.1-1.8_

  - [x] 7.2 Implement CompanySettingsServiceImpl

    - JSON serialization/deserialization
    - Validation logic
    - Default values initialization
    - _Requirements: 1.6, 1.7, 1.8_

  - [x] 7.3 Write property test cho Default Configuration
    - **Property 2: Default Configuration Initialization**
    - **Validates: Requirements 1.6, 1.8**

- [x] 8. Service Layer - Work Schedule

  - [x] 8.1 Tạo IWorkScheduleService interface

    - CRUD operations
    - getEffectiveSchedule
    - assignScheduleToEmployee
    - _Requirements: 2.1-2.7_

  - [x] 8.2 Implement WorkScheduleServiceImpl

    - Schedule validation (start < end)
    - Default schedule fallback
    - Assignment management
    - _Requirements: 2.4, 2.7_

  - [x] 8.3 Write property tests cho Work Schedule
    - **Property 3: Work Schedule Time Validity**
    - **Property 4: Employee Schedule Resolution**
    - **Validates: Requirements 2.4, 2.7**

- [x] 9. Service Layer - Attendance

  - [x] 9.1 Tạo IAttendanceService interface

    - checkIn, checkOut
    - adjustAttendance
    - getAttendanceRecordById, getAttendanceByEmployeeAndDate
    - getAttendanceRecords, getEmployeeAttendanceRecords, getAttendanceSummary
    - validateDevice, validateLocation
    - _Requirements: 3.1-3.9_

  - [x] 9.2 Implement AttendanceServiceImpl

    - Apply rounding rules
    - Calculate working hours
    - Detect late/early
    - Device/location validation
    - _Requirements: 3.1-3.9_

  - [x] 9.3 Write property tests cho Attendance
    - **Property 6: Original Time Preservation**
    - **Property 7: Working Hours Calculation**
    - **Property 8: Late Detection Accuracy**
    - **Property 9: Early Departure Detection Accuracy**
    - **Validates: Requirements 3.4, 3.5, 3.6, 4.4**

- [x] 10. Service Layer - Attendance Adjustment

  - [x] 10.1 Tạo IAttendanceAdjustmentService interface

    - createAdjustmentRequest
    - approveAdjustment, rejectAdjustment
    - getRequestById, getPendingRequests, getEmployeeRequests
    - getAdjustmentHistoryByAttendanceRecord
    - _Requirements: 11.1-11.7_

  - [x] 10.2 Implement AttendanceAdjustmentServiceImpl

    - Request creation with original/requested times
    - Approval workflow
    - Recalculate attendance on approval
    - _Requirements: 11.1-11.7_

  - [x] 10.3 Write property tests cho Adjustment
    - **Property 21: Adjustment Request Workflow**
    - **Property 22: Adjustment Request History**
    - **Validates: Requirements 11.3, 11.7, 11.1, 11.5**

- [x] 11. Checkpoint - Attendance Tests

  - Ensure all attendance tests pass, ask the user if questions arise.

- [x] 12. Service Layer - Payroll

  - [x] 12.1 Tạo IPayrollService interface

    - previewPayroll, finalizePayroll
    - markAsPaid, retryPayment
    - sendSalaryNotifications
    - getPayrollRecordById, getPayrollPeriodSummary
    - getPayrollRecords, getEmployeePayroll, getEmployeePayrollHistory
    - exportPayrollCsv, exportPayrollPdf, generatePayslip
    - _Requirements: 9.1-9.10, 10.1-10.7_

  - [x] 12.2 Implement PayrollServiceImpl

    - Aggregate attendance records
    - Calculate using all calculators
    - Generate payroll records
    - Finalization logic (lock records)
    - _Requirements: 9.1-9.9_

  - [x] 12.3 Implement Payment processing

    - markAsPaid, retryPayment
    - Payment status tracking
    - _Requirements: 10.2, 10.5, 10.6, 10.7_

  - [x] 12.4 Implement Salary notification

    - sendSalaryNotifications
    - Email template với salary breakdown
    - _Requirements: 10.3, 10.4_

  - [x] 12.5 Write property tests cho Payroll
    - **Property 18: Payroll Record Round-Trip**
    - **Property 19: Finalized Payroll Immutability**
    - **Property 23: Salary Notification Content**
    - **Validates: Requirements 9.10, 9.9, 10.4**

- [x] 13. Service Layer - Schedule Selection

  - [x] 13.1 Tạo IScheduleSelectionService interface

    - selectSchedule
    - approveSelection, rejectSelection
    - getSelectionById, getSuggestedSchedules, getAvailableSchedules
    - getPendingSelections, getEmployeeSelectionHistory
    - _Requirements: 12.1-12.7_

  - [x] 13.2 Implement ScheduleSelectionServiceImpl

    - Employee schedule selection
    - Suggestion algorithm (past selections, company recommendations)
    - Approval workflow
    - _Requirements: 12.1-12.7_

  - [x] 13.3 Write property test cho Schedule Suggestion
    - **Property 25: Schedule Suggestion Relevance**
    - **Validates: Requirements 12.2, 12.3**

- [x] 14. Service Layer - Plan Features

  - [x] 14.1 Tạo IPlanFeatureService interface

    - hasFeatureAccess, validateFeatureAccess
    - getCompanyFeatures, getPlanFeatures
    - _Requirements: 13.1-13.7_

  - [x] 14.2 Implement PlanFeatureServiceImpl

    - Feature access check based on plan
    - Feature flags per plan
    - Access denial logging
    - _Requirements: 13.1-13.7_

  - [x] 14.3 Write property test cho Plan Feature Access
    - **Property 24: Plan Feature Access Control**
    - **Validates: Requirements 13.1, 13.2, 13.3, 13.4**

- [x] 15. Service Layer - Holiday & Leave

  - [x] 15.1 Tạo IHolidayService interface

    - CRUD for holidays (create, update, delete)
    - getHolidayById, getHolidays, getHolidaysByDateRange
    - getNationalHolidays
    - _Requirements: 14.1, 14.2_

  - [x] 15.2 Tạo ILeaveService interface

    - createLeaveRequest, cancelLeaveRequest
    - approveLeave, rejectLeave
    - getLeaveRequestById, getPendingLeaveRequests, getEmployeeLeaveRequests
    - getLeaveBalance, updateLeaveBalance
    - _Requirements: 14.3-14.7_

  - [x] 15.3 Implement HolidayServiceImpl và LeaveServiceImpl
    - Holiday management
    - Leave request workflow
    - Balance tracking
    - _Requirements: 14.1-14.7_

- [x] 16. Checkpoint - Service Tests

  - Ensure all service tests pass, ask the user if questions arise.

- [x] 17. Mapper Layer

  - [x] 17.1 Tạo CompanySettingsMapper

    - toEntity, toResponse, updateEntity
    - _Requirements: 1.1-1.8_

  - [x] 17.2 Tạo WorkScheduleMapper

    - toEntity, toResponse, updateEntity
    - _Requirements: 2.1-2.7_

  - [x] 17.3 Tạo AttendanceMapper

    - toEntity, toResponse, toSummaryResponse
    - _Requirements: 3.1-3.9_

  - [x] 17.4 Tạo PayrollMapper

    - toEntity, toResponse, toPreviewResponse
    - _Requirements: 9.1-9.10_

  - [x] 17.5 Tạo các Mapper còn lại
    - AdjustmentRequestMapper
    - ScheduleSelectionMapper
    - HolidayMapper, LeaveMapper
    - _Requirements: 11.1-11.7, 12.1-12.7, 14.1-14.7_

- [x] 18. Controller Layer - Company Settings

  - [x] 18.1 Tạo CompanySettingsController
    - GET /api/company/settings
    - PUT /api/company/settings/attendance
    - PUT /api/company/settings/payroll
    - PUT /api/company/settings/overtime
    - PUT /api/company/settings/allowance
    - PUT /api/company/settings/deduction
    - @PreAuthorize for ADMIN_COMPANY
    - _Requirements: 1.1-1.8_

- [x] 19. Controller Layer - Work Schedule

  - [x] 19.1 Tạo WorkScheduleController

    - GET /api/company/schedules (list)
    - GET /api/company/schedules/{id} (detail)
    - POST /api/company/schedules (create)
    - PUT /api/company/schedules/{id} (update)
    - DELETE /api/company/schedules/{id} (delete)
    - POST /api/company/schedules/{id}/assign (assign to employees)
    - GET /api/company/schedules/{id}/assignments (list assignments)
    - @PreAuthorize for ADMIN_COMPANY, MANAGER_COMPANY
    - _Requirements: 2.1-2.7_

  - [x] 19.2 Tạo ScheduleSelectionController
    - POST /api/employee/schedule-selection (employee select)
    - GET /api/employee/schedule-selection/history (employee history)
    - GET /api/employee/schedule-suggestions (employee suggestions)
    - PUT /api/company/schedule-selections/{id}/approve (manager)
    - PUT /api/company/schedule-selections/{id}/reject (manager)
    - GET /api/company/schedule-selections/pending (manager list)
    - GET /api/company/schedule-selections/{id} (manager detail)
    - _Requirements: 12.1-12.7_

- [x] 20. Controller Layer - Attendance

  - [x] 20.1 Tạo AttendanceController

    - POST /api/employee/attendance/check-in
    - POST /api/employee/attendance/check-out
    - GET /api/company/attendance (admin - list)
    - GET /api/company/attendance/{id} (admin - detail)
    - GET /api/employee/attendance (employee - list)
    - GET /api/employee/attendance/{date} (employee - detail by date)
    - _Requirements: 3.1-3.9_

  - [x] 20.2 Tạo AttendanceAdjustmentController
    - POST /api/employee/attendance-adjustments (employee create)
    - GET /api/employee/attendance-adjustments (employee list)
    - GET /api/employee/attendance-adjustments/{id} (employee detail)
    - PUT /api/company/attendance-adjustments/{id}/approve (manager)
    - PUT /api/company/attendance-adjustments/{id}/reject (manager)
    - GET /api/company/attendance-adjustments/pending (manager list)
    - GET /api/company/attendance-adjustments/{id} (manager detail)
    - GET /api/company/attendance/{recordId}/adjustment-history (adjustment history for record)
    - _Requirements: 11.1-11.7_

- [x] 21. Controller Layer - Payroll

  - [x] 21.1 Tạo PayrollController
    - GET /api/company/payroll/preview (preview for period)
    - POST /api/company/payroll/finalize
    - POST /api/company/payroll/pay
    - POST /api/company/payroll/notify
    - GET /api/company/payroll (list by period)
    - GET /api/company/payroll/{period}/summary (period summary - YYYY-MM)
    - GET /api/company/payroll/records/{id} (record detail)
    - GET /api/company/payroll/export/csv
    - GET /api/company/payroll/export/pdf
    - GET /api/employee/payroll (employee list)
    - GET /api/employee/payroll/{period} (employee payslip detail - YYYY-MM)
    - GET /api/employee/payroll/{recordId}/download (download payslip PDF)
    - @PreAuthorize for ADMIN_COMPANY
    - _Requirements: 9.1-9.10, 10.1-10.7_

- [x] 22. Controller Layer - Holiday & Leave

  - [x] 22.1 Tạo HolidayController

    - GET /api/company/holidays (list)
    - GET /api/company/holidays/{id} (detail)
    - POST /api/company/holidays (create)
    - PUT /api/company/holidays/{id} (update)
    - DELETE /api/company/holidays/{id} (delete)
    - GET /api/company/holidays/range (by date range)
    - @PreAuthorize for ADMIN_COMPANY
    - _Requirements: 14.1, 14.2_

  - [x] 22.2 Tạo LeaveController
    - POST /api/employee/leave-requests (employee create)
    - GET /api/employee/leave-requests (employee list)
    - GET /api/employee/leave-requests/{id} (employee detail)
    - DELETE /api/employee/leave-requests/{id} (employee cancel pending)
    - GET /api/employee/leave-balance (employee balance)
    - PUT /api/company/leave-requests/{id}/approve (manager)
    - PUT /api/company/leave-requests/{id}/reject (manager)
    - GET /api/company/leave-requests/pending (manager list)
    - GET /api/company/leave-requests/{id} (manager detail)
    - _Requirements: 14.3-14.7_

- [x] 23. Audit Trail

  - [x] 23.1 Implement AuditLogService

    - Log attendance changes
    - Log payroll changes
    - Log settings changes
    - Store before/after values
    - _Requirements: 16.1-16.5_

  - [x] 23.2 Write property test cho Audit Log
    - **Property 20: Audit Log Creation**
    - **Validates: Requirements 16.1, 16.2, 16.3, 16.4**

- [x] 24. Error Codes

  - [x] 24.1 Thêm Error Codes vào ErrorCode enum
    - SETTINGS*\*, SCHEDULE*\_, ATTENDANCE\_\_
    - PAYROLL*\*, LEAVE*\_, ADJUSTMENT\_\_
    - SELECTION*\*, PLAN*\_, PAYMENT\_\_
    - _Requirements: All_

- [x] 25. Email Templates

  - [x] 25.1 Tạo Salary Notification templates

    - Tạo file `templates/email/vi/salary-notification.html`
    - Tạo file `templates/email/en/salary-notification.html`
    - Tạo file `templates/email/ja/salary-notification.html`
    - Placeholders: employeeName, period, netSalary, baseSalary, totalOvertime, totalAllowances, totalDeductions, paymentDate
    - _Requirements: 17.2_

  - [x] 25.2 Tạo Adjustment Approved templates

    - Tạo file `templates/email/vi/adjustment-approved.html`
    - Tạo file `templates/email/en/adjustment-approved.html`
    - Tạo file `templates/email/ja/adjustment-approved.html`
    - Placeholders: employeeName, workDate, originalCheckIn/Out, approvedCheckIn/Out, approverName
    - _Requirements: 17.3_

  - [x] 25.3 Tạo Adjustment Rejected templates

    - Tạo file `templates/email/vi/adjustment-rejected.html`
    - Tạo file `templates/email/en/adjustment-rejected.html`
    - Tạo file `templates/email/ja/adjustment-rejected.html`
    - Placeholders: employeeName, workDate, originalCheckIn/Out, requestedCheckIn/Out, rejectionReason, approverName
    - _Requirements: 17.4_

  - [x] 25.4 Tạo Leave Approved templates

    - Tạo file `templates/email/vi/leave-approved.html`
    - Tạo file `templates/email/en/leave-approved.html`
    - Tạo file `templates/email/ja/leave-approved.html`
    - Placeholders: employeeName, leaveType, startDate, endDate, approverName
    - _Requirements: 17.5_

  - [x] 25.5 Tạo Leave Rejected templates

    - Tạo file `templates/email/vi/leave-rejected.html`
    - Tạo file `templates/email/en/leave-rejected.html`
    - Tạo file `templates/email/ja/leave-rejected.html`
    - Placeholders: employeeName, leaveType, startDate, endDate, rejectionReason, approverName
    - _Requirements: 17.6_

  - [x] 25.6 Implement INotificationEmailService

    - sendSalaryNotification, sendBulkSalaryNotifications
    - sendAdjustmentApprovedNotification, sendAdjustmentRejectedNotification
    - sendLeaveApprovedNotification, sendLeaveRejectedNotification
    - Template selection với locale fallback
    - _Requirements: 17.7, 17.8_

  - [x] 25.7 Write property test cho Email Template Language Selection
    - **Property 26: Email Template Language Selection**
    - **Validates: Requirements 17.7, 17.8**

- [x] 26. Final Checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- All tasks are required for comprehensive implementation
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Sử dụng jqwik library cho property-based testing
- Tất cả APIs phải check plan features trước khi xử lý
