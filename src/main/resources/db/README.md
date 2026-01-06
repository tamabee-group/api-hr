# Database Migration Structure

## Multi-Tenant Architecture

Hệ thống sử dụng kiến trúc multi-tenant với database riêng cho mỗi tenant.

### Database Structure

```
PostgreSQL Server
├── tamabee_hr (Master DB)
│   ├── companies
│   ├── users
│   ├── plans
│   ├── plan_features
│   ├── plan_feature_codes
│   ├── wallets
│   ├── wallet_transactions
│   ├── deposit_requests
│   ├── employee_commissions
│   ├── tamabee_settings
│   └── mail_history
│
├── tamabee_tamabee (Tamabee Tenant DB)
│   ├── user_profiles
│   ├── company_settings
│   ├── attendance_records
│   ├── payroll_records
│   └── ... (HR data)
│
└── tamabee_{tenantDomain} (Customer Tenant DBs)
    ├── user_profiles
    ├── company_settings
    ├── attendance_records
    ├── payroll_records
    └── ... (HR data)
```

### Folder Structure

```
db/
├── master/                    # Master DB migrations
│   ├── V1__init.sql          # Schema
│   └── V2__init_settings.sql # Config data
│
├── tenant/                    # Tenant DB template
│   └── V1__init.sql          # Schema template
│
├── scripts/                   # Setup scripts
│   └── create_tamabee_database.sql
│
└── migration/                 # Legacy (single DB) - sẽ bị xóa
    ├── V1__init_schema.sql
    ├── V2__init_settings.sql
    └── V3__init_test_data.sql
```

### Setup Instructions

#### 1. Tạo Master Database

```bash
# Tạo database master
createdb -U postgres tamabee_hr

# Hoặc dùng psql
psql -U postgres -c "CREATE DATABASE tamabee_hr;"
```

#### 2. Tạo Tamabee Tenant Database

```bash
# Chạy script tạo database
psql -U postgres -f src/main/resources/db/scripts/create_tamabee_database.sql
```

#### 3. Chạy Migrations

Flyway sẽ tự động chạy migrations khi application khởi động:

- Master DB: `db/master/V1__init.sql`, `V2__init_settings.sql`
- Tenant DB: `db/tenant/V1__init.sql`

### Soft Delete Strategy

| Entity Type                          | Has Soft Delete | Reason                      |
| ------------------------------------ | --------------- | --------------------------- |
| User, Company, Plan                  | ✅ Yes          | Ít data, cần khôi phục      |
| ShiftTemplate, WorkSchedule          | ✅ Yes          | Template có thể tái sử dụng |
| EmployeeSalary, Allowance, Deduction | ✅ Yes          | Cấu hình quan trọng         |
| AttendanceRecord, BreakRecord        | ❌ No           | Data lớn, tăng liên tục     |
| PayrollRecord, PayrollItem           | ❌ No           | Data lớn mỗi kỳ lương       |
| WalletTransaction, AuditLog          | ❌ No           | Log data, có thể archive    |

### Tamabee Special Tenant

- Company ID: 0
- Tenant Domain: "tamabee"
- Database: tamabee_tamabee
- Plan ID: null (all features enabled)
