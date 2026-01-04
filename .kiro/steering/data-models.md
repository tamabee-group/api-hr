# Data Models

## Entity Domains

### User & Company

- `UserEntity`: employeeCode (6-char), email, role, companyId (0 = Tamabee)
- `UserProfileEntity`: personal info, avatar
- `CompanyEntity`: planId, referredByEmployeeCode
- `CompanyProfileEntity`: address, contact info
- `CompanySettingEntity`: attendance, payroll settings

### Wallet & Billing

- `WalletEntity`: balance, lastBillingDate, nextBillingDate
- `WalletTransactionEntity`: type (DEPOSIT/BILLING/REFUND/COMMISSION), amount, balanceBefore/After
- `DepositRequestEntity`: status, transferProofUrl, approvedBy
- `EmployeeCommissionEntity`: referral commissions
- `PlanEntity`, `PlanFeatureEntity`: subscription plans

### Attendance

- `ShiftTemplateEntity`: shift definitions
- `ShiftAssignmentEntity`: employee shift assignments
- `WorkScheduleEntity`: work schedules
- `AttendanceRecordEntity`: clock in/out records
- `BreakRecordEntity`: break times
- `AttendanceAdjustmentRequestEntity`: correction requests

### Leave

- `LeaveRequestEntity`: leave applications
- `LeaveBalanceEntity`: remaining leave days
- `HolidayEntity`: company holidays

### Payroll

- `EmployeeSalaryEntity`: base salary config
- `EmployeeAllowanceEntity`: allowances
- `EmployeeDeductionEntity`: deductions
- `PayrollPeriodEntity`: payroll periods
- `PayrollRecordEntity`: calculated payroll
- `PayrollItemEntity`: payroll line items

### Contract

- `EmploymentContractEntity`: employment contracts

### Audit

- `AuditLogEntity`: system audit logs
- `WorkModeChangeLogEntity`: work mode changes

## BaseEntity Fields

All entities extend `BaseEntity`:

- `createdAt`, `createdBy`
- `updatedAt`, `updatedBy`
- `deleted` (soft delete)

## Key Enums

```java
UserRole: ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE,
          ADMIN_COMPANY, MANAGER_COMPANY, EMPLOYEE_COMPANY
UserStatus: ACTIVE, INACTIVE, PENDING
DepositStatus: PENDING, APPROVED, REJECTED
TransactionType: DEPOSIT, BILLING, REFUND, COMMISSION
AttendanceStatus: PRESENT, ABSENT, LATE, EARLY_LEAVE
LeaveStatus: PENDING, APPROVED, REJECTED, CANCELLED
PayrollStatus: DRAFT, CONFIRMED, PAID
ContractType: FULL_TIME, PART_TIME, CONTRACT
```

## Index Strategy

- Index ALL foreign keys
- Index `deleted` field (CRITICAL)
- Composite indexes for common query patterns
