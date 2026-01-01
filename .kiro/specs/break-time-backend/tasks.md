# Implementation Plan: Break Time Management Backend

## Overview

- Triển khai hệ thống quản lý giờ giải lao tích hợp với hệ thống Attendance & Payroll hiện có.
- Khi KIRO thực hiện task thì hãy phản hồi tôi bằng tiếng Việt.

## Tasks

- [x] 1. Database Schema & Migrations

  - [x] 1.1 Tạo Flyway migration cho break_records table

    - Tạo file `V{n}__create_break_records_table.sql`
    - Columns: attendanceRecordId, employeeId, companyId, workDate, breakStart, breakEnd, actualBreakMinutes, effectiveBreakMinutes, notes
    - Indexes trên attendanceRecordId, employeeId, workDate
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 1.2 Tạo Flyway migration để thêm break fields vào attendance_records

    - Tạo file `V{n}__add_break_fields_to_attendance.sql`
    - Columns: totalBreakMinutes, effectiveBreakMinutes, breakType, breakCompliant
    - _Requirements: 5.1, 5.2_

  - [x] 1.3 Tạo Flyway migration để thêm break fields vào payroll_records
    - Tạo file `V{n}__add_break_fields_to_payroll.sql`
    - Columns: totalBreakMinutes, breakType, breakDeductionAmount
    - _Requirements: 6.5_

- [x] 2. Enums & Configuration DTOs

  - [x] 2.1 Tạo BreakType enum

    - PAID, UNPAID
    - _Requirements: 1.2_

  - [x] 2.2 Tạo BreakConfig DTO

    - breakEnabled, breakType, defaultBreakMinutes
    - minimumBreakMinutes, maximumBreakMinutes
    - useLegalMinimum, breakTrackingEnabled, locale
    - fixedBreakMode, breakPeriodsPerAttendance, fixedBreakPeriods
    - _Requirements: 1.1-1.14_

  - [x] 2.3 Tạo BreakPeriod DTO

    - name, startTime, endTime, durationMinutes, isFlexible, order
    - _Requirements: 3.1-3.9_

  - [x] 2.4 Cập nhật WorkScheduleData để thêm breakPeriods

    - Thêm List<BreakPeriod> breakPeriods
    - Thêm totalBreakMinutes
    - _Requirements: 3.6_

  - [x] 2.5 Tạo OvertimeConfig DTO

    - overtimeEnabled, standardWorkingHours
    - nightStartTime, nightEndTime
    - regularOvertimeRate, nightWorkRate, nightOvertimeRate
    - holidayOvertimeRate, holidayNightOvertimeRate
    - useLegalMinimum, locale
    - _Requirements: 12.1-12.10_

  - [x] 2.6 Tạo OvertimeResult và OvertimeMultipliers DTOs

    - OvertimeResult: regularMinutes, nightMinutes, overtimeMinutes, amounts
    - OvertimeMultipliers: các hệ số nhân
    - _Requirements: 11.7, 12.1-12.5_

  - [x] 2.7 Write property test cho BreakConfig validation
    - **Property 3: Minimum Break Enforcement**
    - **Validates: Requirements 7.3**

- [x] 3. Entity Classes

  - [x] 3.1 Tạo BreakRecordEntity

    - Extends BaseEntity
    - Fields: attendanceRecordId, employeeId, companyId, workDate, breakStart, breakEnd, actualBreakMinutes, effectiveBreakMinutes, notes
    - _Requirements: 4.1-4.7_

  - [x] 3.2 Cập nhật AttendanceRecordEntity

    - Thêm: totalBreakMinutes, effectiveBreakMinutes, breakType, breakCompliant
    - _Requirements: 5.1-5.6_

  - [x] 3.3 Cập nhật PayrollRecordEntity
    - Thêm: totalBreakMinutes, breakType, breakDeductionAmount
    - _Requirements: 6.1-6.6_

- [x] 4. Repository Layer

  - [x] 4.1 Tạo BreakRecordRepository
    - findByIdAndDeletedFalse
    - findByAttendanceRecordIdAndDeletedFalse
    - findByEmployeeIdAndWorkDateAndDeletedFalse
    - findByCompanyIdAndWorkDateBetweenAndDeletedFalse
    - _Requirements: 4.1-4.7_

- [x] 5. Calculator Modules

  - [x] 5.1 Implement LegalBreakRequirements

    - getMinimumBreak cho Japanese labor law
    - getMinimumBreak cho Vietnamese labor law
    - getMinimumBreak cho default/other locales
    - _Requirements: 2.1-2.3_

  - [x] 5.2 Write property test cho Legal Break Requirements

    - **Property 4: Legal Minimum Compliance**
    - **Validates: Requirements 2.5, 7.4**

  - [x] 5.3 Implement BreakCalculator

    - calculateTotalBreakMinutes
    - calculateEffectiveBreakMinutes (với min/max capping)
    - calculateNetWorkingMinutes
    - getLegalMinimumBreak
    - isNightShift detection
    - calculateWorkingMinutesForOvernightShift
    - _Requirements: 5.1-5.6, 9.1-9.10_

  - [x] 5.4 Write property tests cho Break Calculator

    - **Property 1: Break Duration Non-Negative**
    - **Property 5: Working Hours Calculation with Unpaid Break**
    - **Property 6: Working Hours Calculation with Paid Break**
    - **Property 8: Effective Break Capping**
    - **Property 10: Total Break Minutes Invariant**
    - **Property 11: Night Shift Detection**
    - **Property 12: Overnight Working Hours Calculation**
    - **Property 13: Night Shift Break Requirements**
    - **Property 14: Break Period Across Midnight**
    - **Validates: Requirements 5.1, 5.2, 4.6, 7.2, 9.1-9.8**

  - [x] 5.5 Cập nhật WorkingHoursCalculator

    - Tích hợp BreakCalculator
    - Tính working hours có tính đến break policy
    - Xử lý overnight shift (qua đêm)
    - isOvernightShift detection
    - _Requirements: 5.1-5.6, 9.1-9.10_

  - [x] 5.6 Implement LegalOvertimeRequirements

    - getMinimumMultipliers cho Japanese labor law
    - getMinimumMultipliers cho Vietnamese labor law
    - getMinimumMultipliers cho default/other locales
    - _Requirements: 12.9_

  - [x] 5.7 Implement OvertimeCalculator

    - calculateOvertime với break và night shift
    - calculateNightMinutes (sau khi trừ break)
    - getLegalMinimumMultipliers
    - validateMultipliers (không thấp hơn legal minimum)
    - Split hours cho overnight shift (regular, night, morning)
    - _Requirements: 10.1-10.8, 11.1-11.7, 12.1-12.10_

  - [x] 5.8 Write property tests cho Overtime Calculator
    - **Property 15: Overtime Multiplier Validation**
    - **Property 16: Night Minutes Calculation**
    - **Property 17: Overtime Amount Calculation**
    - **Property 18: Overnight Shift Hour Split**
    - **Validates: Requirements 11.4, 11.5, 12.6, 12.10**

- [x] 6. Checkpoint - Calculator Tests

  - Ensure all calculator tests pass, ask the user if questions arise.

- [x] 7. Service Layer - Break Service

  - [x] 7.1 Tạo IBreakService interface

    - startBreak, endBreak
    - getBreakRecordsByAttendance, getBreakSummary
    - validateBreakDuration, getLegalMinimumBreak, getEffectiveMinimumBreak
    - _Requirements: 4.1-4.7, 7.1-7.6_

  - [x] 7.2 Implement BreakServiceImpl

    - Break recording logic
    - Validation logic
    - Legal minimum enforcement
    - Fixed break mode: auto-create break records
    - _Requirements: 4.1-4.9, 7.1-7.6_

  - [x] 7.3 Write property tests cho Break Service
    - **Property 2: Break Time Within Working Hours**
    - **Property 9: Break Record Audit Trail**
    - **Validates: Requirements 3.5, 4.7**

- [x] 8. Service Layer - Update Existing Services

  - [x] 8.1 Cập nhật CompanySettingsService

    - Thêm updateBreakConfig method
    - Thêm updateOvertimeConfig method
    - Validation cho BreakConfig và OvertimeConfig
    - _Requirements: 1.1-1.10, 12.1-12.10_

  - [x] 8.2 Cập nhật AttendanceService

    - Tích hợp break calculation vào working hours
    - Cập nhật checkIn/checkOut để xử lý break
    - _Requirements: 5.1-5.6_

  - [x] 8.3 Cập nhật PayrollService

    - Tích hợp break deduction vào payroll calculation
    - Tích hợp OvertimeCalculator để tính overtime với multipliers
    - Lưu break info và overtime breakdown vào PayrollRecord
    - _Requirements: 6.1-6.6, 10.1-10.8, 11.1-11.7_

  - [x] 8.4 Write property test cho Payroll with Break and Overtime
    - **Property 7: Break Deduction Consistency**
    - **Property 17: Overtime Amount Calculation**
    - **Validates: Requirements 6.1, 6.2, 11.3, 12.10**

- [x] 9. Checkpoint - Service Tests

  - Ensure all service tests pass, ask the user if questions arise.

- [x] 10. Mapper Layer

  - [x] 10.1 Tạo BreakRecordMapper

    - toEntity, toResponse
    - _Requirements: 4.1-4.7_

  - [x] 10.2 Tạo BreakConfigMapper

    - toConfig, toResponse
    - _Requirements: 1.1-1.10_

  - [x] 10.3 Tạo OvertimeConfigMapper

    - toConfig, toResponse
    - _Requirements: 12.1-12.10_

  - [x] 10.4 Cập nhật AttendanceMapper
    - Thêm break fields vào response
    - _Requirements: 5.1-5.6_

- [x] 11. Controller Layer

  - [x] 11.1 Cập nhật CompanySettingsController

    - PUT /api/company/settings/break
    - PUT /api/company/settings/overtime
    - GET /api/company/settings/overtime
    - @PreAuthorize for ADMIN_COMPANY
    - _Requirements: 1.1-1.10, 12.1-12.10_

  - [x] 11.2 Tạo BreakController

    - POST /api/employee/attendance/break/start
    - POST /api/employee/attendance/break/{breakRecordId}/end
    - GET /api/employee/attendance/{date}/breaks
    - @PreAuthorize for authenticated employees
    - _Requirements: 4.1-4.7_

  - [x] 11.3 Cập nhật WorkScheduleController
    - Thêm break periods vào schedule creation/update
    - _Requirements: 3.1-3.7_

- [x] 12. Report Layer

  - [x] 12.1 Tạo BreakReportService

    - generateDailyBreakReport
    - generateMonthlyBreakReport
    - _Requirements: 8.1-8.5_

  - [x] 12.2 Tạo BreakReportController
    - GET /api/company/reports/break/daily
    - GET /api/company/reports/break/monthly
    - @PreAuthorize for ADMIN_COMPANY, MANAGER_COMPANY
    - _Requirements: 8.1-8.5_

- [x] 13. Error Codes

  - [x] 13.1 Thêm Break Error Codes vào ErrorCode enum

    - INVALID_BREAK_CONFIG, BREAK_MINIMUM_EXCEEDS_MAXIMUM, BREAK_BELOW_LEGAL_MINIMUM
    - BREAK_ALREADY_STARTED, NO_ACTIVE_BREAK, BREAK_OUTSIDE_WORKING_HOURS
    - BREAK_RECORD_NOT_FOUND, INVALID_BREAK_DURATION, BREAK_START_AFTER_END
    - _Requirements: 7.1-7.6_

  - [x] 13.2 Thêm Overtime Error Codes vào ErrorCode enum
    - INVALID_OVERTIME_CONFIG, OVERTIME_RATE_BELOW_LEGAL_MINIMUM, INVALID_NIGHT_HOURS_CONFIG
    - _Requirements: 12.6_

- [x] 14. Final Checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tích hợp với hệ thống Attendance & Payroll hiện có
- Sử dụng jqwik library cho property-based testing
- Tuân thủ quy định pháp luật về giờ giải lao (Nhật Bản, Việt Nam)
- Break config được lưu trong CompanySettings JSON
