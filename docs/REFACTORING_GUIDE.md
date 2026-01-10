# Backend Refactoring Guide

## Mục tiêu

Tổ chức lại cấu trúc thư mục để:

- Dễ tìm kiếm file
- Nhất quán naming convention
- Tách biệt rõ ràng theo domain
- Dễ maintain và scale

---

## Cấu trúc hiện tại vs Đề xuất

### Vấn đề hiện tại

1. **Service layer không nhất quán**: Một số impl nằm trực tiếp trong package, một số trong `impl/`
2. **Repository flat**: Tất cả repository nằm chung 1 folder
3. **DTO chưa tổ chức theo domain**: `request/`, `response/` flat
4. **Model package thừa**: Trùng với `dto/`

---

## Cấu trúc đề xuất chi tiết

```
src/main/java/com/tamabee/api_hr/
├── ApiHrApplication.java
│
├── config/                          # ✅ Giữ nguyên
│   ├── CorsConfig.java
│   ├── DataInitializer.java
│   ├── DataSourceConfig.java
│   ├── FlywayMultiTenantConfig.java
│   ├── JpaConfig.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtConfig.java
│   ├── SecurityConfig.java
│   ├── SshTunnelInitializer.java
│   └── WebMvcConfig.java
│
├── constants/                       # ✅ Giữ nguyên
│   └── PlanConstants.java
│
├── controller/                      # ✅ Giữ nguyên cấu trúc
│   ├── admin/                       # /api/admin/**
│   ├── company/                     # /api/company/**
│   └── core/                        # /api/auth/**, /api/users/**
│
├── datasource/                      # ✅ Giữ nguyên (multi-tenant)
│   ├── TenantDatabaseInitializer.java
│   ├── TenantDataSourceLoader.java
│   ├── TenantDataSourceManager.java
│   ├── TenantFilter.java            # ← Di chuyển từ filter/
│   ├── TenantProvisioningService.java
│   └── TenantRoutingDataSource.java
│
├── dto/                             # 🔄 Tổ chức lại theo domain
│   ├── auth/                        # Auth DTOs
│   │   ├── LoginRequest.java        # ← từ model/request/
│   │   ├── LoginResponse.java       # ← từ model/response/
│   │   ├── RegisterRequest.java
│   │   ├── ForgotPasswordRequest.java
│   │   ├── ResetPasswordRequest.java
│   │   ├── SendVerificationRequest.java
│   │   └── VerifyEmailRequest.java
│   │
│   ├── common/                      # Shared DTOs
│   │   └── BaseResponse.java        # ← từ model/response/
│   │
│   ├── config/                      # ✅ Giữ nguyên
│   │   ├── AllowanceCondition.java
│   │   ├── AllowanceConfig.java
│   │   ├── AllowanceRule.java
│   │   ├── AttendanceConfig.java
│   │   ├── BreakConfig.java
│   │   ├── BreakPeriod.java
│   │   ├── DeductionConfig.java
│   │   ├── DeductionRule.java
│   │   ├── OvertimeConfig.java
│   │   ├── OvertimeMultipliers.java
│   │   ├── PayrollConfig.java
│   │   ├── RoundingConfig.java
│   │   ├── WorkModeConfig.java
│   │   └── WorkScheduleData.java
│   │
│   ├── result/                      # ✅ Giữ nguyên (internal calculation)
│   │   ├── AllowanceItem.java
│   │   ├── AllowanceResult.java
│   │   ├── AttendanceSummary.java
│   │   ├── DailyOvertimeDetail.java
│   │   ├── DeductionItem.java
│   │   ├── DeductionResult.java
│   │   ├── EmployeeSalaryInfo.java
│   │   ├── OvertimeResult.java
│   │   ├── PayrollResult.java
│   │   └── WorkingHoursResult.java
│   │
│   ├── request/                     # 🔄 Tổ chức theo domain
│   │   ├── attendance/
│   │   │   ├── AdjustAttendanceRequest.java
│   │   │   ├── AttendanceConfigRequest.java
│   │   │   ├── AttendanceQueryRequest.java
│   │   │   ├── BatchShiftAssignmentRequest.java
│   │   │   ├── BreakConfigRequest.java
│   │   │   ├── CheckInRequest.java
│   │   │   ├── CheckOutRequest.java
│   │   │   ├── CreateAdjustmentRequest.java
│   │   │   ├── EmployeeSwapRequest.java
│   │   │   ├── ShiftAssignmentQuery.java
│   │   │   ├── ShiftAssignmentRequest.java
│   │   │   ├── ShiftSwapRequest.java
│   │   │   ├── ShiftTemplateRequest.java
│   │   │   ├── StartBreakRequest.java
│   │   │   └── SwapRequestQuery.java
│   │   │
│   │   ├── company/
│   │   │   ├── UpdateCompanyProfileRequest.java
│   │   │   └── UpdateCompanyRequest.java
│   │   │
│   │   ├── leave/
│   │   │   ├── CreateHolidayRequest.java
│   │   │   ├── CreateLeaveRequest.java
│   │   │   └── UpdateHolidayRequest.java
│   │   │
│   │   ├── payroll/
│   │   │   ├── AllowanceAssignmentRequest.java
│   │   │   ├── AllowanceConfigRequest.java
│   │   │   ├── ContractQuery.java
│   │   │   ├── ContractRequest.java
│   │   │   ├── DeductionAssignmentRequest.java
│   │   │   ├── DeductionConfigRequest.java
│   │   │   ├── OvertimeConfigRequest.java
│   │   │   ├── PayrollAdjustmentRequest.java
│   │   │   ├── PayrollConfigRequest.java
│   │   │   ├── PayrollPeriodRequest.java
│   │   │   ├── ReportQuery.java
│   │   │   └── SalaryConfigRequest.java
│   │   │
│   │   ├── schedule/
│   │   │   ├── AssignScheduleRequest.java
│   │   │   ├── CreateWorkScheduleRequest.java
│   │   │   ├── SelectScheduleRequest.java
│   │   │   ├── UpdateWorkScheduleRequest.java
│   │   │   └── WorkModeConfigRequest.java
│   │   │
│   │   ├── user/
│   │   │   ├── CreateCompanyEmployeeRequest.java
│   │   │   ├── CreateTamabeeUserRequest.java
│   │   │   ├── CreateUserRequest.java
│   │   │   ├── UpdateUserProfileRequest.java
│   │   │   └── UpdateUserRequest.java
│   │   │
│   │   ├── wallet/
│   │   │   ├── CommissionFilterRequest.java
│   │   │   ├── DepositFilterRequest.java
│   │   │   ├── DepositRequestCreateRequest.java
│   │   │   ├── DirectWalletRequest.java
│   │   │   ├── PaymentRequest.java
│   │   │   ├── PlanCreateRequest.java
│   │   │   ├── PlanFeatureRequest.java
│   │   │   ├── PlanUpdateRequest.java
│   │   │   ├── RefundRequest.java
│   │   │   └── TransactionFilterRequest.java
│   │   │
│   │   ├── AuditLogQueryRequest.java
│   │   ├── RejectRequest.java
│   │   └── SettingUpdateRequest.java
│   │
│   └── response/                    # 🔄 Tổ chức theo domain
│       ├── attendance/
│       │   ├── AdjustmentRequestResponse.java
│       │   ├── AttendanceRecordResponse.java
│       │   ├── AttendanceSummaryResponse.java
│       │   ├── BatchAssignmentResult.java
│       │   ├── BreakConfigResponse.java
│       │   ├── BreakConfigSnapshot.java
│       │   ├── BreakRecordResponse.java
│       │   ├── BreakSummaryResponse.java
│       │   ├── DailyBreakReportResponse.java
│       │   ├── MonthlyBreakReportResponse.java
│       │   ├── ShiftAssignmentResponse.java
│       │   ├── ShiftInfoResponse.java
│       │   ├── ShiftSwapRequestResponse.java
│       │   └── ShiftTemplateResponse.java
│       │
│       ├── audit/
│       │   └── AuditLogResponse.java
│       │
│       ├── company/
│       │   ├── CompanyProfileResponse.java
│       │   ├── CompanyResponse.java
│       │   ├── CompanySettingsResponse.java
│       │   ├── DomainAvailabilityResponse.java
│       │   └── PublicSettingsResponse.java
│       │
│       ├── leave/
│       │   ├── HolidayResponse.java
│       │   ├── LeaveBalanceResponse.java
│       │   └── LeaveRequestResponse.java
│       │
│       ├── payroll/
│       │   ├── AppliedSettingsSnapshot.java
│       │   ├── ContractResponse.java
│       │   ├── EmployeeAllowanceResponse.java
│       │   ├── EmployeeDeductionResponse.java
│       │   ├── EmployeeSalaryConfigResponse.java
│       │   ├── OvertimeConfigResponse.java
│       │   ├── PayrollItemResponse.java
│       │   ├── PayrollPeriodDetailResponse.java
│       │   ├── PayrollPeriodResponse.java
│       │   ├── PayrollPeriodSummaryResponse.java
│       │   ├── PayrollPreviewResponse.java
│       │   ├── PayrollRecordResponse.java
│       │   ├── RoundingConfigSnapshot.java
│       │   └── SalaryConfigValidationResponse.java
│       │
│       ├── report/
│       │   ├── AttendanceSummaryReport.java
│       │   ├── BreakComplianceReport.java
│       │   ├── CostAnalysisReport.java
│       │   ├── CostByContractType.java
│       │   ├── CostBySalaryType.java
│       │   ├── EmployeeAttendanceSummary.java
│       │   ├── EmployeeBreakSummary.java
│       │   ├── EmployeeOvertimeSummary.java
│       │   ├── EmployeePayrollSummary.java
│       │   ├── OvertimeReport.java
│       │   ├── PayrollSummaryReport.java
│       │   ├── ShiftTemplateSummary.java
│       │   └── ShiftUtilizationReport.java
│       │
│       ├── schedule/
│       │   ├── EmployeeScheduleDataResponse.java
│       │   ├── ScheduleSelectionResponse.java
│       │   ├── WorkModeChangeLogResponse.java
│       │   ├── WorkModeConfigResponse.java
│       │   ├── WorkScheduleAssignmentResponse.java
│       │   └── WorkScheduleResponse.java
│       │
│       ├── user/
│       │   ├── ApproverResponse.java
│       │   ├── UserProfileResponse.java
│       │   └── UserResponse.java
│       │
│       ├── wallet/
│       │   ├── CommissionOverallSummaryResponse.java
│       │   ├── CommissionResponse.java
│       │   ├── CommissionSummaryResponse.java
│       │   ├── DepositRequestResponse.java
│       │   ├── PlanFeatureResponse.java
│       │   ├── PlanFeaturesResponse.java
│       │   ├── PlanResponse.java
│       │   ├── ReferredCompanyResponse.java
│       │   ├── WalletOverviewResponse.java
│       │   ├── WalletResponse.java
│       │   ├── WalletStatisticsResponse.java
│       │   └── WalletTransactionResponse.java
│       │
│       └── SettingResponse.java
```

```
│
├── entity/                          # ✅ Giữ nguyên cấu trúc
│   ├── BaseEntity.java
│   ├── attendance/
│   ├── audit/
│   ├── company/
│   ├── contract/
│   ├── core/
│   ├── leave/
│   ├── payroll/
│   ├── user/
│   └── wallet/
│
├── enums/                           # ✅ Giữ nguyên
│
├── exception/                       # ✅ Giữ nguyên
│
├── mapper/                          # ✅ Giữ nguyên cấu trúc
│   ├── admin/
│   ├── company/
│   └── core/
│
├── repository/                      # 🔄 Tổ chức theo domain
│   ├── attendance/
│   │   ├── AttendanceAdjustmentRequestRepository.java
│   │   ├── AttendanceRecordRepository.java
│   │   ├── BreakRecordRepository.java
│   │   ├── ScheduleSelectionRepository.java
│   │   ├── ShiftAssignmentRepository.java
│   │   ├── ShiftSwapRequestRepository.java
│   │   ├── ShiftTemplateRepository.java
│   │   ├── WorkModeChangeLogRepository.java
│   │   ├── WorkScheduleAssignmentRepository.java
│   │   └── WorkScheduleRepository.java
│   │
│   ├── audit/
│   │   └── AuditLogRepository.java
│   │
│   ├── company/
│   │   ├── CompanyRepository.java
│   │   └── CompanySettingsRepository.java
│   │
│   ├── contract/
│   │   └── EmploymentContractRepository.java
│   │
│   ├── core/
│   │   └── EmailVerificationRepository.java
│   │
│   ├── leave/
│   │   ├── HolidayRepository.java
│   │   ├── LeaveBalanceRepository.java
│   │   └── LeaveRequestRepository.java
│   │
│   ├── payroll/
│   │   ├── EmployeeAllowanceRepository.java
│   │   ├── EmployeeDeductionRepository.java
│   │   ├── EmployeeSalaryRepository.java
│   │   ├── PayrollItemRepository.java
│   │   ├── PayrollPeriodRepository.java
│   │   └── PayrollRecordRepository.java
│   │
│   ├── user/
│   │   └── UserRepository.java
│   │
│   └── wallet/
│       ├── DepositRequestRepository.java
│       ├── EmployeeCommissionRepository.java
│       ├── PlanFeatureCodeRepository.java
│       ├── PlanFeatureRepository.java
│       ├── PlanRepository.java
│       ├── TamabeeSettingRepository.java
│       ├── WalletRepository.java
│       └── WalletTransactionRepository.java
│
├── scheduler/                       # ✅ Giữ nguyên
│   ├── BillingScheduler.java
│   ├── ContractExpiryScheduler.java
│   └── TenantCleanupScheduler.java
```

```
│
├── service/                         # 🔄 Chuẩn hóa Interface + Impl
│   ├── admin/
│   │   ├── IBillingService.java
│   │   ├── ICommissionService.java
│   │   ├── ICompanyManagerService.java
│   │   ├── IDepositRequestService.java
│   │   ├── IEmployeeManagerService.java
│   │   ├── IEmployeeReferralService.java
│   │   ├── IPlanService.java
│   │   ├── ISettingService.java
│   │   ├── IWalletService.java
│   │   ├── IWalletTransactionService.java
│   │   └── impl/                    # ✅ Tất cả impl vào đây
│   │       ├── BillingServiceImpl.java
│   │       ├── CommissionServiceImpl.java
│   │       ├── CompanyManagerServiceImpl.java
│   │       ├── DepositRequestServiceImpl.java
│   │       ├── EmployeeManagerServiceImpl.java
│   │       ├── EmployeeReferralServiceImpl.java
│   │       ├── PlanServiceImpl.java
│   │       ├── SettingServiceImpl.java
│   │       ├── WalletServiceImpl.java
│   │       └── WalletTransactionServiceImpl.java
│   │
│   ├── calculator/                  # ✅ Giữ nguyên (business logic)
│   │   ├── IAllowanceCalculator.java
│   │   ├── IBreakCalculator.java
│   │   ├── IDeductionCalculator.java
│   │   ├── IOvertimeCalculator.java
│   │   ├── IPayrollCalculator.java
│   │   ├── ITimeRoundingCalculator.java
│   │   ├── IWorkingHoursCalculator.java
│   │   ├── AllowanceCalculator.java
│   │   ├── BreakCalculator.java
│   │   ├── DeductionCalculator.java
│   │   ├── OvertimeCalculator.java
│   │   ├── PayrollCalculator.java
│   │   ├── TimeRoundingCalculator.java
│   │   ├── WorkingHoursCalculator.java
│   │   ├── LegalBreakRequirements.java
│   │   └── LegalOvertimeRequirements.java
│   │
│   ├── company/
│   │   ├── IAttendanceAdjustmentService.java
│   │   ├── IAttendanceService.java
│   │   ├── IBreakReportService.java
│   │   ├── IBreakService.java
│   │   ├── ICachedCompanySettingsService.java
│   │   ├── ICompanyDepositService.java
│   │   ├── ICompanyEmployeeService.java
│   │   ├── ICompanyProfileService.java
│   │   ├── ICompanySettingsService.java
│   │   ├── ICompanyWalletService.java
│   │   ├── IEmployeeAllowanceService.java
│   │   ├── IEmployeeDeductionService.java
│   │   ├── IEmployeeSalaryConfigService.java
│   │   ├── IEmploymentContractService.java
│   │   ├── IHolidayService.java
│   │   ├── ILeaveService.java
│   │   ├── IPayrollPeriodService.java
│   │   ├── IPayrollService.java
│   │   ├── IPlanFeatureService.java
│   │   ├── IReportService.java
│   │   ├── IScheduleSelectionService.java
│   │   ├── IShiftService.java
│   │   ├── IUserManagerService.java
│   │   ├── IUserProfileService.java
│   │   ├── IWorkScheduleService.java
│   │   ├── cache/                   # ✅ Giữ nguyên
│   │   │   └── CachedCompanySettingsServiceImpl.java
│   │   └── impl/                    # 🔄 Di chuyển tất cả impl vào đây
│   │       ├── AttendanceAdjustmentServiceImpl.java
│   │       ├── AttendanceServiceImpl.java
│   │       ├── BreakReportServiceImpl.java
│   │       ├── BreakServiceImpl.java
│   │       ├── CompanyDepositServiceImpl.java
│   │       ├── CompanyEmployeeServiceImpl.java
│   │       ├── CompanyProfileServiceImpl.java
│   │       ├── CompanySettingsServiceImpl.java
│   │       ├── CompanyWalletServiceImpl.java
│   │       ├── EmployeeAllowanceServiceImpl.java   # ← di chuyển
│   │       ├── EmployeeDeductionServiceImpl.java   # ← di chuyển
│   │       ├── EmployeeSalaryConfigServiceImpl.java # ← di chuyển
│   │       ├── EmploymentContractServiceImpl.java  # ← di chuyển
│   │       ├── HolidayServiceImpl.java
│   │       ├── LeaveServiceImpl.java
│   │       ├── PayrollPeriodServiceImpl.java       # ← di chuyển
│   │       ├── PayrollServiceImpl.java
│   │       ├── PlanFeatureServiceImpl.java
│   │       ├── ReportExportService.java            # ← di chuyển
│   │       ├── ReportServiceImpl.java              # ← di chuyển
│   │       ├── ScheduleSelectionServiceImpl.java
│   │       ├── ShiftServiceImpl.java               # ← di chuyển
│   │       ├── UserManagerServiceImpl.java
│   │       ├── UserProfileServiceImpl.java
│   │       └── WorkScheduleServiceImpl.java
```

```
│   │
│   └── core/
│       ├── IAuditLogService.java
│       ├── IAuthService.java
│       ├── IEmailService.java
│       ├── IEmailVerificationService.java
│       ├── IEmployeeScheduleService.java
│       ├── INotificationEmailService.java
│       ├── IPlanFeaturesService.java
│       ├── IUploadService.java
│       └── impl/                    # 🔄 Di chuyển tất cả impl vào đây
│           ├── AuditLogServiceImpl.java
│           ├── AuthServiceImpl.java
│           ├── EmailServiceImpl.java
│           ├── EmailVerificationServiceImpl.java
│           ├── EmployeeScheduleServiceImpl.java    # ← di chuyển
│           ├── NotificationEmailServiceImpl.java
│           ├── PayslipPdfGenerator.java            # ← di chuyển
│           ├── PlanFeaturesServiceImpl.java
│           └── UploadServiceImpl.java
│
└── util/                            # ✅ Giữ nguyên
    ├── EmployeeCodeGenerator.java
    ├── JwtUtil.java
    ├── LocaleUtil.java
    ├── ReferralCodeGenerator.java
    ├── ReportLabels.java
    ├── SecurityUtil.java
    └── TenantDomainValidator.java
```

---

## Danh sách file cần di chuyển (với checkbox)

### 1. Xóa package `model/` (merge vào `dto/`)

- [ ] `model/request/LoginRequest.java` → `dto/auth/LoginRequest.java`
- [ ] `model/request/RegisterRequest.java` → `dto/auth/RegisterRequest.java`
- [ ] `model/request/ForgotPasswordRequest.java` → `dto/auth/ForgotPasswordRequest.java`
- [ ] `model/request/ResetPasswordRequest.java` → `dto/auth/ResetPasswordRequest.java`
- [ ] `model/request/SendVerificationRequest.java` → `dto/auth/SendVerificationRequest.java`
- [ ] `model/request/VerifyEmailRequest.java` → `dto/auth/VerifyEmailRequest.java`
- [ ] `model/response/BaseResponse.java` → `dto/common/BaseResponse.java`
- [ ] `model/response/LoginResponse.java` → `dto/auth/LoginResponse.java`
- [ ] Xóa folder `model/`

### 2. Di chuyển `filter/` vào `datasource/`

- [ ] `filter/TenantContext.java` → `datasource/TenantContext.java`
- [ ] `filter/TenantFilter.java` → `datasource/TenantFilter.java`
- [ ] Xóa folder `filter/`

**Lý do**: TenantContext và TenantFilter liên quan trực tiếp đến multi-tenant datasource.

### 3. Di chuyển Service Impl vào `impl/`

**Company Services:**

- [ ] `service/company/EmployeeAllowanceServiceImpl.java` → `service/company/impl/`
- [ ] `service/company/EmployeeDeductionServiceImpl.java` → `service/company/impl/`
- [ ] `service/company/EmployeeSalaryConfigServiceImpl.java` → `service/company/impl/`
- [ ] `service/company/EmploymentContractServiceImpl.java` → `service/company/impl/`
- [ ] `service/company/PayrollPeriodServiceImpl.java` → `service/company/impl/`
- [ ] `service/company/ReportExportService.java` → `service/company/impl/`
- [ ] `service/company/ReportServiceImpl.java` → `service/company/impl/`
- [ ] `service/company/ShiftServiceImpl.java` → `service/company/impl/`

**Core Services:**

- [ ] `service/core/EmployeeScheduleServiceImpl.java` → `service/core/impl/`
- [ ] `service/core/PayslipPdfGenerator.java` → `service/core/impl/`

### 4. Tổ chức Repository theo domain

**Attendance:**

- [ ] `repository/AttendanceAdjustmentRequestRepository.java` → `repository/attendance/`
- [ ] `repository/AttendanceRecordRepository.java` → `repository/attendance/`
- [ ] `repository/BreakRecordRepository.java` → `repository/attendance/`
- [ ] `repository/ScheduleSelectionRepository.java` → `repository/attendance/`
- [ ] `repository/ShiftAssignmentRepository.java` → `repository/attendance/`
- [ ] `repository/ShiftSwapRequestRepository.java` → `repository/attendance/`
- [ ] `repository/ShiftTemplateRepository.java` → `repository/attendance/`
- [ ] `repository/WorkModeChangeLogRepository.java` → `repository/attendance/`
- [ ] `repository/WorkScheduleAssignmentRepository.java` → `repository/attendance/`
- [ ] `repository/WorkScheduleRepository.java` → `repository/attendance/`

**Audit:**

- [ ] `repository/AuditLogRepository.java` → `repository/audit/`

**Company:**

- [ ] `repository/CompanyRepository.java` → `repository/company/`
- [ ] `repository/CompanySettingsRepository.java` → `repository/company/`

**Contract:**

- [ ] `repository/EmploymentContractRepository.java` → `repository/contract/`

**Core:**

- [ ] `repository/EmailVerificationRepository.java` → `repository/core/`

**Leave:**

- [ ] `repository/HolidayRepository.java` → `repository/leave/`
- [ ] `repository/LeaveBalanceRepository.java` → `repository/leave/`
- [ ] `repository/LeaveRequestRepository.java` → `repository/leave/`

**Payroll:**

- [ ] `repository/EmployeeAllowanceRepository.java` → `repository/payroll/`
- [ ] `repository/EmployeeDeductionRepository.java` → `repository/payroll/`
- [ ] `repository/EmployeeSalaryRepository.java` → `repository/payroll/`
- [ ] `repository/PayrollItemRepository.java` → `repository/payroll/`
- [ ] `repository/PayrollPeriodRepository.java` → `repository/payroll/`
- [ ] `repository/PayrollRecordRepository.java` → `repository/payroll/`

**User:**

- [ ] `repository/UserRepository.java` → `repository/user/`

**Wallet:**

- [ ] `repository/DepositRequestRepository.java` → `repository/wallet/`
- [ ] `repository/EmployeeCommissionRepository.java` → `repository/wallet/`
- [ ] `repository/PlanFeatureCodeRepository.java` → `repository/wallet/`
- [ ] `repository/PlanFeatureRepository.java` → `repository/wallet/`
- [ ] `repository/PlanRepository.java` → `repository/wallet/`
- [ ] `repository/TamabeeSettingRepository.java` → `repository/wallet/`
- [ ] `repository/WalletRepository.java` → `repository/wallet/`
- [ ] `repository/WalletTransactionRepository.java` → `repository/wallet/`

---

## Hướng dẫn thực hiện từng bước

### Bước 1: Tạo cấu trúc thư mục mới

```bash
# Repository subfolders
mkdir -p src/main/java/com/tamabee/api_hr/repository/{attendance,audit,company,contract,core,leave,payroll,user,wallet}

# DTO auth & common
mkdir -p src/main/java/com/tamabee/api_hr/dto/{auth,common}

# DTO request/response theo domain
mkdir -p src/main/java/com/tamabee/api_hr/dto/request/{attendance,company,leave,payroll,user,wallet}
mkdir -p src/main/java/com/tamabee/api_hr/dto/response/{attendance,company,leave,payroll,user,wallet}
```

### Bước 2: Di chuyển Repository (ưu tiên cao)

```bash
# Attendance
git mv repository/AttendanceAdjustmentRequestRepository.java repository/attendance/
git mv repository/AttendanceRecordRepository.java repository/attendance/
git mv repository/BreakRecordRepository.java repository/attendance/
git mv repository/ScheduleSelectionRepository.java repository/attendance/
git mv repository/ShiftAssignmentRepository.java repository/attendance/
git mv repository/ShiftSwapRequestRepository.java repository/attendance/
git mv repository/ShiftTemplateRepository.java repository/attendance/
git mv repository/WorkModeChangeLogRepository.java repository/attendance/
git mv repository/WorkScheduleAssignmentRepository.java repository/attendance/
git mv repository/WorkScheduleRepository.java repository/attendance/

# Audit
git mv repository/AuditLogRepository.java repository/audit/

# Company
git mv repository/CompanyRepository.java repository/company/
git mv repository/CompanySettingsRepository.java repository/company/

# Contract
git mv repository/EmploymentContractRepository.java repository/contract/

# Core
git mv repository/EmailVerificationRepository.java repository/core/

# Leave
git mv repository/HolidayRepository.java repository/leave/
git mv repository/LeaveBalanceRepository.java repository/leave/
git mv repository/LeaveRequestRepository.java repository/leave/

# Payroll
git mv repository/EmployeeAllowanceRepository.java repository/payroll/
git mv repository/EmployeeDeductionRepository.java repository/payroll/
git mv repository/EmployeeSalaryRepository.java repository/payroll/
git mv repository/PayrollItemRepository.java repository/payroll/
git mv repository/PayrollPeriodRepository.java repository/payroll/
git mv repository/PayrollRecordRepository.java repository/payroll/

# User
git mv repository/UserRepository.java repository/user/

# Wallet
git mv repository/DepositRequestRepository.java repository/wallet/
git mv repository/EmployeeCommissionRepository.java repository/wallet/
git mv repository/PlanFeatureCodeRepository.java repository/wallet/
git mv repository/PlanFeatureRepository.java repository/wallet/
git mv repository/PlanRepository.java repository/wallet/
git mv repository/TamabeeSettingRepository.java repository/wallet/
git mv repository/WalletRepository.java repository/wallet/
git mv repository/WalletTransactionRepository.java repository/wallet/
```

### Bước 3: Di chuyển Service Impl

```bash
# Company services
git mv service/company/EmployeeAllowanceServiceImpl.java service/company/impl/
git mv service/company/EmployeeDeductionServiceImpl.java service/company/impl/
git mv service/company/EmployeeSalaryConfigServiceImpl.java service/company/impl/
git mv service/company/EmploymentContractServiceImpl.java service/company/impl/
git mv service/company/PayrollPeriodServiceImpl.java service/company/impl/
git mv service/company/ReportExportService.java service/company/impl/
git mv service/company/ReportServiceImpl.java service/company/impl/
git mv service/company/ShiftServiceImpl.java service/company/impl/

# Core services
git mv service/core/EmployeeScheduleServiceImpl.java service/core/impl/
git mv service/core/PayslipPdfGenerator.java service/core/impl/
```

### Bước 4: Merge model/ vào dto/

```bash
# Auth Request DTOs
git mv model/request/LoginRequest.java dto/auth/request/
git mv model/request/RegisterRequest.java dto/auth/request/
git mv model/request/ForgotPasswordRequest.java dto/auth/request/
git mv model/request/ResetPasswordRequest.java dto/auth/request/
git mv model/request/SendVerificationRequest.java dto/auth/request/
git mv model/request/VerifyEmailRequest.java dto/auth/request/

# Auth Response DTOs
git mv model/response/LoginResponse.java dto/auth/response/

# Common
git mv model/response/BaseResponse.java dto/common/

# Xóa folder model
rm -rf model/
```

### Bước 5: Di chuyển filter/ vào datasource/

```bash
git mv filter/TenantContext.java datasource/
git mv filter/TenantFilter.java datasource/
rm -rf filter/
```

### Bước 6: Cập nhật import trong các file

Sau khi di chuyển, cần update import statements. IDE (IntelliJ) sẽ tự động suggest.

**Ví dụ thay đổi import:**

```java
// Trước

import com.tamabee.api_hr.model.request.LoginRequest;
import com.tamabee.api_hr.model.response.BaseResponse;
import com.tamabee.api_hr.model.response.LoginResponse;

// Sau
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.dto.auth.request.LoginRequest;
import com.tamabee.api_hr.dto.auth.response.LoginResponse;

```

---

## Naming Conventions

### Interfaces

| Type                 | Pattern             | Example               |
| -------------------- | ------------------- | --------------------- |
| Service Interface    | `I{Domain}Service`  | `IPayrollService`     |
| Calculator Interface | `I{Name}Calculator` | `IOvertimeCalculator` |

### Implementations

| Type            | Pattern               | Example              |
| --------------- | --------------------- | -------------------- |
| Service Impl    | `{Domain}ServiceImpl` | `PayrollServiceImpl` |
| Calculator Impl | `{Name}Calculator`    | `OvertimeCalculator` |

### DTOs

| Type     | Pattern                   | Example             |
| -------- | ------------------------- | ------------------- |
| Request  | `{Action}{Domain}Request` | `CreateUserRequest` |
| Response | `{Domain}Response`        | `UserResponse`      |
| Query    | `{Domain}Query`           | `ContractQuery`     |

### Repository

| Type       | Pattern              | Example          |
| ---------- | -------------------- | ---------------- |
| Repository | `{Entity}Repository` | `UserRepository` |

---

## Checklist sau refactor

### Build & Test

- [ ] Build thành công: `.\mvnw clean compile`
- [ ] Tests pass: `.\mvnw test`

### Packages đã xóa

- [ ] Không còn package `model/`
- [ ] Không còn package `filter/`

### Cấu trúc mới

- [ ] Tất cả ServiceImpl nằm trong `impl/`
- [ ] Repository được tổ chức theo domain
- [ ] Import statements đã được cập nhật
- [ ] Không còn file nào ở vị trí cũ

---

## Lưu ý quan trọng

1. **Backup trước khi refactor**: Commit tất cả changes hiện tại
2. **Refactor từng bước**: Không di chuyển tất cả cùng lúc
3. **Test sau mỗi bước**: Đảm bảo build và test pass
4. **Sử dụng IDE refactor**: IntelliJ có tính năng "Move" tự động update imports

---

## Ưu tiên thực hiện

1. **Cao**: Di chuyển Service Impl vào `impl/` (nhất quán)
2. **Cao**: Merge `model/` vào `dto/` (loại bỏ trùng lặp)
3. **Trung bình**: Tổ chức Repository theo domain
4. **Thấp**: Di chuyển `filter/` vào `datasource/`
