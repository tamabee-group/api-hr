# Backend Architecture (Java/Spring Boot)

## Project Structure

```
src/main/java/com/tamabee/api_hr/
├── controller/
│   ├── admin/          # Tamabee admin APIs (/api/admin/**)
│   ├── company/        # Company APIs (/api/company/**)
│   └── core/           # Auth, public APIs (/api/auth/**, /api/users/**)
├── service/
│   ├── admin/          # Admin business logic
│   ├── company/        # Company business logic
│   └── core/           # Shared services (Auth, Email, File)
├── mapper/
│   ├── admin/          # Admin mappers
│   ├── company/        # Company mappers
│   └── core/           # Core mappers
├── entity/
│   ├── attendance/     # Shift, Schedule, AttendanceRecord
│   ├── audit/          # AuditLog, WorkModeChangeLog
│   ├── company/        # Company, CompanyProfile, CompanySetting
│   ├── contract/       # EmploymentContract
│   ├── core/           # EmailVerification, MailHistory
│   ├── leave/          # Holiday, LeaveBalance, LeaveRequest
│   ├── payroll/        # Salary, Allowance, Deduction, PayrollRecord
│   ├── user/           # User, UserProfile
│   └── wallet/         # Wallet, Transaction, Deposit, Plan, Commission
├── repository/         # JPA repositories
├── dto/request/        # Request DTOs
├── dto/response/       # Response DTOs
├── enums/              # All enums
├── exception/          # Custom exceptions
├── config/             # Configuration classes
├── filter/             # Request filters
├── scheduler/          # Scheduled tasks
└── util/               # Utility classes
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
