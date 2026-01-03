# Implementation Plan: Flexible Workforce Management - Backend API

## Overview

Triển khai hệ thống quản lý nhân sự linh hoạt cho backend API, bao gồm:

- Unified Attendance API
- Shift Management
- Employee Salary Configuration
- Individual Allowances/Deductions
- Payroll Period Workflow
- Employment Contract Management
- Reporting

Khi Kiro thực hiện task hãy phản hồi tôi bằng tiếng việt.

## Tasks

- [x] 1. Database Schema & Entities

  - [x] 1.1 Create new enums (ShiftAssignmentStatus, SwapRequestStatus, PayrollPeriodStatus, PayrollItemStatus, ContractType, ContractStatus)
    - Tạo các enum files trong package `enums/`
    - _Requirements: 2.1-2.7, 5.1-5.8, 9.1-9.7_
  - [x] 1.2 Create ShiftTemplateEntity and ShiftAssignmentEntity
    - Tạo entities trong package `entity/attendance/`
    - Bao gồm indexes cho companyId, employeeId, workDate
    - _Requirements: 2.1, 2.2_
  - [x] 1.3 Create ShiftSwapRequestEntity
    - Tạo entity cho yêu cầu đổi ca
    - _Requirements: 2.3-2.5_
  - [x] 1.4 Create EmployeeAllowanceEntity and EmployeeDeductionEntity
    - Tạo entities trong package `entity/payroll/`
    - _Requirements: 4.1, 4.2_
  - [x] 1.5 Create PayrollPeriodEntity and PayrollItemEntity
    - Tạo entities cho kỳ lương và chi tiết lương
    - _Requirements: 5.1-5.8_
  - [x] 1.6 Create EmploymentContractEntity
    - Tạo entity cho hợp đồng lao động
    - _Requirements: 9.1-9.7_
  - [x] 1.7 Update EmployeeSalaryEntity to add shiftRate field
    - Thêm field shiftRate cho lương theo ca
    - _Requirements: 3.1-3.7_
  - [x] 1.8 Create Flyway migration for new tables
    - Tạo migration script V4\_\_flexible_workforce.sql
    - Bao gồm tables, indexes, constraints
    - _Requirements: 1.1-10.8_

- [x] 2. Repository Layer

  - [x] 2.1 Create ShiftTemplateRepository and ShiftAssignmentRepository
    - Implement findByCompanyId, findByEmployeeIdAndWorkDate
    - _Requirements: 2.1-2.7_
  - [x] 2.2 Create ShiftSwapRequestRepository
    - Implement findByCompanyIdAndStatus, findByRequesterId
    - _Requirements: 2.3-2.5_
  - [x] 2.3 Create EmployeeAllowanceRepository and EmployeeDeductionRepository
    - Implement findActiveByEmployeeId, findByEffectiveDateRange
    - _Requirements: 4.1-4.6_
  - [x] 2.4 Create PayrollPeriodRepository and PayrollItemRepository
    - Implement findByCompanyIdAndYearAndMonth, findByPeriodId
    - _Requirements: 5.1-5.8_
  - [x] 2.5 Create EmploymentContractRepository
    - Implement findActiveByEmployeeId, findExpiringContracts
    - _Requirements: 9.1-9.7_

- [x] 3. Unified Attendance Service

  - [x] 3.1 Create UnifiedAttendanceResponse DTO with embedded break records
    - Tạo DTO trong package `dto/response/`
    - Bao gồm BreakRecordResponse, AppliedSettingsSnapshot, ShiftInfo
    - _Requirements: 1.1, 1.2, 8.1-8.5_
  - [x] 3.2 Implement IUnifiedAttendanceService interface
    - Định nghĩa methods: getTodayAttendance, getAttendanceByDate, checkIn, checkOut, startBreak, endBreak
    - _Requirements: 1.1-1.8_
  - [x] 3.3 Implement UnifiedAttendanceServiceImpl
    - Implement getTodayAttendance với embedded break records
    - Implement getAttendanceByDate với embedded break records
    - _Requirements: 1.1, 1.2_
  - [x] 3.4 Implement checkIn with company settings validation
    - Apply rounding, grace period, device, location validation
    - _Requirements: 1.3, 1.7, 1.8, 7.1_
  - [x] 3.5 Implement checkOut with calculations
    - Calculate working minutes, overtime, late/early leave
    - _Requirements: 1.4, 7.2_
  - [x] 3.6 Implement startBreak with validation
    - Validate max breaks per day, break periods
    - _Requirements: 1.5, 7.3_
  - [x] 3.7 Implement endBreak with effective minutes calculation
    - Apply min/max break settings
    - _Requirements: 1.6_
  - [ ]\* 3.8 Write property test for unified attendance response
    - **Property 1: Unified Attendance Response Contains All Break Records**
    - **Validates: Requirements 1.1, 1.2**
  - [ ]\* 3.9 Write property test for check-in rounding
    - **Property 2: Check-in Rounding Applies Company Settings Correctly**
    - **Validates: Requirements 1.3, 7.1**
  - [ ]\* 3.10 Write property test for working time calculation
    - **Property 3: Working Time Calculation Accuracy**
    - **Validates: Requirements 1.4, 7.2**
  - [ ]\* 3.11 Write property test for break count validation
    - **Property 4: Break Count Validation**
    - **Validates: Requirements 1.5, 7.3**
  - [ ]\* 3.12 Write property test for effective break minutes
    - **Property 5: Effective Break Minutes Clamping**
    - **Validates: Requirements 1.6**

- [x] 4. Shift Management Service

  - [x] 4.1 Create Shift DTOs (ShiftTemplateRequest, ShiftTemplateResponse, ShiftAssignmentRequest, etc.)
    - Tạo DTOs trong package `dto/request/` và `dto/response/`
    - _Requirements: 2.1-2.7_
  - [x] 4.2 Implement IShiftService interface
    - Định nghĩa methods cho CRUD shift template, assignment, swap
    - _Requirements: 2.1-2.7_
  - [x] 4.3 Implement ShiftServiceImpl - Shift Template CRUD
    - createShiftTemplate, updateShiftTemplate, deleteShiftTemplate, getShiftTemplates
    - _Requirements: 2.1_
  - [x] 4.4 Implement ShiftServiceImpl - Shift Assignment
    - assignShift với overlap validation, unassignShift, getShiftAssignments
    - _Requirements: 2.2, 2.6, 2.7_
  - [x] 4.5 Implement ShiftServiceImpl - Shift Swap
    - requestSwap, approveSwap, rejectSwap, getSwapRequests
    - _Requirements: 2.3-2.5_
  - [ ]\* 4.6 Write property test for shift assignment no overlap
    - **Property 8: Shift Assignment No Overlap Invariant**
    - **Validates: Requirements 2.7**
  - [ ]\* 4.7 Write property test for shift swap updates
    - **Property 9: Shift Swap Updates Both Assignments**
    - **Validates: Requirements 2.4**

- [x] 5. Employee Salary Configuration Service

  - [x] 5.1 Create Salary Config DTOs
    - SalaryConfigRequest, EmployeeSalaryConfigResponse
    - _Requirements: 3.1-3.7_
  - [x] 5.2 Implement IEmployeeSalaryConfigService interface
    - createSalaryConfig, updateSalaryConfig, getCurrentSalaryConfig, getSalaryConfigHistory
    - _Requirements: 3.1-3.7_
  - [x] 5.3 Implement EmployeeSalaryConfigServiceImpl
    - Support MONTHLY, DAILY, HOURLY, SHIFT_BASED salary types
    - Implement versioning logic (close previous config when creating new)
    - _Requirements: 3.1-3.7_
  - [ ]\* 5.4 Write property test for salary config versioning
    - **Property 10: Salary Config Versioning**
    - **Validates: Requirements 3.6**
  - [ ]\* 5.5 Write property test for active salary config selection
    - **Property 11: Active Salary Config Selection**
    - **Validates: Requirements 3.7**

- [x] 6. Individual Allowance/Deduction Service

  - [x] 6.1 Create Allowance/Deduction DTOs
    - AllowanceAssignmentRequest, EmployeeAllowanceResponse, DeductionAssignmentRequest, EmployeeDeductionResponse
    - _Requirements: 4.1-4.6_
  - [x] 6.2 Implement IEmployeeAllowanceService and IEmployeeDeductionService interfaces
    - assignAllowance, updateAllowance, deactivateAllowance, getEmployeeAllowances
    - assignDeduction, updateDeduction, deactivateDeduction, getEmployeeDeductions
    - _Requirements: 4.1-4.6_
  - [x] 6.3 Implement EmployeeAllowanceServiceImpl
    - Support override company defaults
    - Implement soft deactivation
    - _Requirements: 4.1, 4.4, 4.5, 4.6_
  - [x] 6.4 Implement EmployeeDeductionServiceImpl
    - Support override company defaults
    - Implement soft deactivation
    - _Requirements: 4.2, 4.4, 4.5, 4.6_

- [x] 7. Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Payroll Period Service

  - [x] 8.1 Create Payroll DTOs
    - PayrollPeriodRequest, PayrollPeriodResponse, PayrollPeriodDetailResponse
    - PayrollItemResponse, PayrollAdjustmentRequest, PaymentRequest
    - _Requirements: 5.1-5.8_
  - [x] 8.2 Implement IPayrollPeriodService interface
    - createPayrollPeriod, calculatePayroll, getPayrollPeriodDetail
    - adjustPayrollItem, submitForReview, approvePayroll, markAsPaid
    - _Requirements: 5.1-5.8_
  - [x] 8.3 Implement PayrollPeriodServiceImpl - Period Management
    - createPayrollPeriod với status DRAFT
    - getPayrollPeriodDetail với all items
    - _Requirements: 5.1, 5.3_
  - [x] 8.4 Implement PayrollPeriodServiceImpl - Calculation
    - calculatePayroll: generate payroll items for all active employees
    - Apply salary config, overtime, allowances, deductions
    - _Requirements: 5.2, 5.8_
  - [x] 8.5 Implement PayrollPeriodServiceImpl - Adjustment
    - adjustPayrollItem: store adjustment amount and reason
    - Validate period status before allowing adjustment
    - _Requirements: 5.4, 5.7_
  - [x] 8.6 Implement PayrollPeriodServiceImpl - Workflow
    - submitForReview, approvePayroll, markAsPaid
    - Validate status transitions
    - _Requirements: 5.5, 5.6, 5.7_
  - [ ]\* 8.7 Write property test for payroll applies active allowances/deductions
    - **Property 13: Payroll Applies Only Active Allowances/Deductions**
    - **Validates: Requirements 4.3, 6.8**
  - [ ]\* 8.8 Write property test for payroll period status transition
    - **Property 14: Payroll Period Status Transition Invariant**
    - **Validates: Requirements 5.7**
  - [ ]\* 8.9 Write property test for gross salary calculation
    - **Property 15: Gross Salary Calculation Formula**
    - **Validates: Requirements 5.8**

- [x] 9. Payroll Calculation Engine

  - [x] 9.1 Implement IPayrollCalculator interface
    - calculateBaseSalary, calculateOvertime, calculateBreakDeduction
    - calculateAllowances, calculateDeductions, calculateGrossSalary, calculateNetSalary
    - _Requirements: 6.1-6.8_
  - [x] 9.2 Implement PayrollCalculatorImpl - Base Salary
    - MONTHLY: prorate based on working days
    - DAILY: daily rate \* working days
    - HOURLY: hourly rate \* working hours
    - SHIFT_BASED: shift rate \* number of shifts
    - _Requirements: 6.1-6.4_
  - [x] 9.3 Implement PayrollCalculatorImpl - Overtime
    - Apply company overtime rates (regular, night, holiday, weekend)
    - _Requirements: 6.5_
  - [x] 9.4 Implement PayrollCalculatorImpl - Break Deduction
    - Deduct break time for UNPAID break type
    - _Requirements: 6.6_
  - [x] 9.5 Implement PayrollCalculatorImpl - Penalties
    - Apply late/early leave penalties from company config
    - _Requirements: 6.7_
  - [ ]\* 9.6 Write property test for salary calculation by type
    - **Property 12: Salary Calculation by Type**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4**

- [x] 10. Employment Contract Service

  - [x] 10.1 Create Contract DTOs
    - ContractRequest, ContractResponse
    - _Requirements: 9.1-9.7_
  - [x] 10.2 Implement IEmploymentContractService interface
    - createContract, updateContract, terminateContract
    - getCurrentContract, getContractHistory, getExpiringContracts
    - _Requirements: 9.1-9.7_
  - [x] 10.3 Implement EmploymentContractServiceImpl
    - Create contract with overlap validation
    - Link to salary config
    - Implement expiry detection
    - _Requirements: 9.1-9.6_
  - [x] 10.4 Implement contract expiry scheduler
    - Auto-update employee status when contract expires
    - _Requirements: 9.5_
  - [ ]\* 10.5 Write property test for contract no overlap
    - **Property 16: Contract No Overlap Invariant**
    - **Validates: Requirements 9.6**
  - [ ]\* 10.6 Write property test for expiring contract detection
    - **Property 17: Expiring Contract Detection**
    - **Validates: Requirements 9.3**

- [x] 11. Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Report Service

  - [x] 12.1 Create Report DTOs
    - ReportQuery, AttendanceSummaryReport, OvertimeReport, BreakComplianceReport
    - PayrollSummaryReport, CostAnalysisReport, ShiftUtilizationReport
    - _Requirements: 10.1-10.8_
  - [x] 12.2 Implement IReportService interface
    - generateAttendanceSummary, generateOvertimeReport, generateBreakComplianceReport
    - generatePayrollSummary, generateCostAnalysis, generateShiftUtilization
    - exportReport
    - _Requirements: 10.1-10.8_
  - [x] 12.3 Implement ReportServiceImpl - Attendance Reports
    - Attendance summary, overtime breakdown, break compliance
    - _Requirements: 10.1-10.3_
  - [x] 12.4 Implement ReportServiceImpl - Payroll Reports
    - Payroll summary, cost analysis
    - _Requirements: 10.4, 10.5_
  - [x] 12.5 Implement ReportServiceImpl - Shift Reports
    - Shift utilization, swap statistics
    - _Requirements: 10.8_
  - [x] 12.6 Implement Report Export (CSV, PDF)
    - Export reports in CSV and PDF formats
    - _Requirements: 10.6_
  - [ ]\* 12.7 Write property test for report date range filtering
    - **Property 19: Report Date Range Filtering**
    - **Validates: Requirements 10.7**

- [x] 13. Controller Layer

  - [x] 13.1 Create UnifiedAttendanceController
    - GET /api/employee/attendance/today (unified response)
    - GET /api/employee/attendance/{date} (unified response)
    - POST /api/employee/attendance/check-in
    - POST /api/employee/attendance/check-out
    - POST /api/employee/attendance/break/start
    - POST /api/employee/attendance/break/{id}/end
    - _Requirements: 1.1-1.8_
  - [x] 13.2 Create ShiftController
    - CRUD endpoints for shift templates
    - Shift assignment endpoints
    - Shift swap endpoints
    - _Requirements: 2.1-2.7_
  - [x] 13.3 Create EmployeeSalaryConfigController
    - CRUD endpoints for salary config
    - _Requirements: 3.1-3.7_
  - [x] 13.4 Create EmployeeAllowanceController and EmployeeDeductionController
    - CRUD endpoints for individual allowances/deductions
    - _Requirements: 4.1-4.6_
  - [x] 13.5 Create PayrollPeriodController
    - CRUD endpoints for payroll periods
    - Workflow endpoints (submit, approve, pay)
    - _Requirements: 5.1-5.8_
  - [x] 13.6 Create EmploymentContractController
    - CRUD endpoints for contracts
    - Expiring contracts endpoint
    - _Requirements: 9.1-9.7_
  - [x] 13.7 Create ReportController
    - Report generation endpoints
    - Export endpoints
    - _Requirements: 10.1-10.8_

- [x] 14. Settings Validation & Caching

  - [x] 14.1 Implement CompanySettingsCache
    - Cache company settings per request
    - _Requirements: 7.7_
  - [x] 14.2 Implement settings validation with defaults
    - Use default values when config is missing
    - Log warnings for missing configs
    - _Requirements: 7.6_
  - [x] 14.3 Write property test for settings fallback
    - **Property 18: Settings Fallback to Defaults**
    - **Validates: Requirements 7.6**
  - [x] 14.4 Write property test for attendance filtering
    - **Property 20: Attendance Filtering Accuracy**
    - **Validates: Requirements 8.6**

- [x] 15. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
