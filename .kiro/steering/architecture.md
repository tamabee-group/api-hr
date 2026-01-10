# Backend Architecture (Java/Spring Boot)

## Project Structure

```
src/main/java/com/tamabee/api_hr/
├── config/                 # Configuration classes
├── constants/              # Constants (PlanConstants)
├── controller/
│   ├── admin/              # /api/admin/**
│   ├── company/            # /api/company/**
│   └── core/               # /api/auth/**, /api/users/**
├── datasource/             # Multi-tenant (TenantContext, TenantFilter, routing)
├── dto/
│   ├── auth/
│   │   ├── request/        # LoginRequest, RegisterRequest...
│   │   └── response/       # LoginResponse
│   ├── common/             # BaseResponse
│   ├── config/             # AllowanceConfig, PayrollConfig...
│   ├── result/             # Internal calculation results
│   ├── request/            # API request DTOs (theo domain)
│   └── response/           # API response DTOs (theo domain)
├── entity/
│   ├── attendance/         # Shift, Schedule, AttendanceRecord
│   ├── audit/              # AuditLog, WorkModeChangeLog
│   ├── company/            # Company, CompanyProfile, CompanySetting
│   ├── contract/           # EmploymentContract
│   ├── core/               # EmailVerification, MailHistory
│   ├── leave/              # Holiday, LeaveBalance, LeaveRequest
│   ├── payroll/            # Salary, Allowance, Deduction, PayrollRecord
│   ├── user/               # User, UserProfile
│   └── wallet/             # Wallet, Transaction, Deposit, Plan
├── enums/                  # All enums
├── exception/              # Custom exceptions
├── mapper/
│   ├── admin/              # Admin mappers
│   ├── company/            # Company mappers
│   └── core/               # Core mappers
├── repository/
│   ├── attendance/         # AttendanceRecord, BreakRecord, Shift...
│   ├── audit/              # AuditLog
│   ├── company/            # Company, CompanySettings
│   ├── contract/           # EmploymentContract
│   ├── core/               # EmailVerification
│   ├── leave/              # Holiday, LeaveBalance, LeaveRequest
│   ├── payroll/            # Salary, Allowance, Deduction, PayrollRecord
│   ├── user/               # User
│   └── wallet/             # Wallet, Transaction, Deposit, Plan
├── scheduler/              # Scheduled tasks
├── service/
│   ├── admin/
│   │   ├── I{Name}Service.java
│   │   └── impl/           # Tất cả ServiceImpl
│   ├── calculator/         # Business logic calculators
│   ├── company/
│   │   ├── I{Name}Service.java
│   │   ├── cache/          # Cached services
│   │   └── impl/           # Tất cả ServiceImpl
│   └── core/
│       ├── I{Name}Service.java
│       └── impl/           # Tất cả ServiceImpl
└── util/                   # Utility classes
```

## Layer Flow

```
Controller → Service (Interface + Impl) → Mapper → Repository → Entity
```

## Package Access Control

| Package | API Path          | Roles                              |
| ------- | ----------------- | ---------------------------------- |
| admin   | `/api/admin/**`   | `ADMIN_TAMABEE`, `MANAGER_TAMABEE` |
| company | `/api/company/**` | `ADMIN_COMPANY`, `MANAGER_COMPANY` |
| core    | `/api/auth/**`    | Public                             |
| core    | `/api/users/me`   | All authenticated                  |

## User Roles

| Role               | CompanyId | Description               |
| ------------------ | --------- | ------------------------- |
| `ADMIN_TAMABEE`    | 0         | Full system access        |
| `MANAGER_TAMABEE`  | 0         | Manage companies/deposits |
| `EMPLOYEE_TAMABEE` | 0         | Limited Tamabee access    |
| `ADMIN_COMPANY`    | ID        | Full company access       |
| `MANAGER_COMPANY`  | ID        | Manage employees          |
| `EMPLOYEE_COMPANY` | ID        | Basic employee access     |
