---
name: Backend Data Models
description: Entity domains, BaseEntity, soft delete strategy, repository patterns, key enums, index strategy
---

# Data Models

## Entity Domains

### User & Company

- `UserEntity`: employeeCode (6-char), email, role, companyId (0 = Tamabee), language
- `UserProfileEntity`: personal info, avatar
- `CompanyEntity`: planId, region ("vi"/"ja"), referredByEmployeeCode
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

### Notification & Feedback

- `SystemNotificationEntity`: system announcements (master DB), titleVi/En/Ja, contentVi/En/Ja (Markdown), targetAudience
- `FeedbackEntity`: user feedback tickets (master DB), userId, tenantDomain, type, status, attachmentUrls
- `FeedbackReplyEntity`: Tamabee staff replies (master DB), feedbackId, repliedByUserId, content
- `NotificationEntity` (mở rộng): thêm `title`, `content` (Markdown), `systemNotificationId`

### Audit

- `AuditLogEntity`: system audit logs
- `WorkModeChangeLogEntity`: work mode changes

## BaseEntity Fields

All entities extend `BaseEntity`:

- `createdAt`, `createdBy`
- `updatedAt`, `updatedBy`

### RegionAwareAuditListener

`BaseEntity` sử dụng `@EntityListeners(RegionAwareAuditListener.class)` thay vì Spring `AuditingEntityListener`. Listener này inject timezone từ `RegionContext` vào `createdAt`/`updatedAt`:

- `@PrePersist`: set cả `createdAt` và `updatedAt` = `LocalDateTime.now(timezone)`
- `@PreUpdate`: set `updatedAt` = `LocalDateTime.now(timezone)`
- Fallback: UTC nếu không có region (scheduler, system operation)

### Region vs Language

| Concept    | Thuộc về    | Values           | Quyết định                                    |
| ---------- | ----------- | ---------------- | --------------------------------------------- |
| `region`   | Company     | "vi", "ja"       | Timezone, currency, salary templates, zipcode |
| `language` | User        | "vi", "en", "ja" | Ngôn ngữ giao diện cá nhân                    |
| `[locale]` | Next.js URL | "vi", "en", "ja" | i18n routing (KHÔNG liên quan region)         |

- `CompanyEntity.region` — lưu trên DB, chỉ ADMIN_TAMABEE được đổi
- `UserEntity` KHÔNG có field `region` — `UserResponse.region` được derive từ `company.region` qua `UserMapper`
- `UserEntity.language` — ngôn ngữ giao diện, mỗi user tự chọn
- JWT chứa cả `region` (từ company) và `language` (từ user)

## Soft Delete Strategy

**BaseEntity KHÔNG có `deleted`** - chỉ entities cần soft delete mới tự thêm field này.

### Entities CÓ soft delete (ít data, cần khôi phục)

```
User, UserProfile, Company, CompanyProfile, CompanySetting
Plan, PlanFeature, PlanFeatureCode
ShiftTemplate, WorkSchedule, Holiday
EmployeeSalary, EmployeeAllowance, EmployeeDeduction
EmploymentContract, DepositRequest, Wallet, TamabeeSetting
```

### Entities KHÔNG có soft delete (data lớn, xóa thẳng)

```
AttendanceRecord, BreakRecord, PayrollRecord, PayrollItem, PayrollPeriod
WalletTransaction, AuditLog, WorkModeChangeLog, MailHistory
LeaveRequest, LeaveBalance, AttendanceAdjustmentRequest, ShiftSwapRequest
ShiftAssignment, WorkScheduleAssignment, ScheduleSelection
EmployeeCommission, EmailVerification
SystemNotification, Feedback, FeedbackReply
```

### Repository Pattern

```java
// CÓ soft delete - luôn filter deleted = false
Optional<UserEntity> findByIdAndDeletedFalse(Long id);

// KHÔNG soft delete - query bình thường
List<AttendanceRecordEntity> findByUserId(Long userId);
```

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
TargetAudience: COMPANY_ADMINS, ALL_USERS
FeedbackType: BUG_REPORT, FEATURE_REQUEST, GENERAL_FEEDBACK, SUPPORT_REQUEST
FeedbackStatus: OPEN, IN_PROGRESS, RESOLVED, CLOSED
```

## Index Strategy

- Index ALL foreign keys
- Index `deleted` field CHỈ cho entities có soft delete
- Composite indexes for common query patterns
