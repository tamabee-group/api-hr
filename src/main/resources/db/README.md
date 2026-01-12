# Database Migration Structure

## Multi-Tenant Architecture

Hệ thống sử dụng kiến trúc multi-tenant với database riêng cho mỗi tenant.

### Database Structure

```
PostgreSQL Server
├── tamabee_hr (Master DB)
│   ├── companies
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
│   ├── users
│   ├── user_profiles
│   ├── company_settings
│   ├── attendance_records
│   ├── payroll_records
│   └── ... (HR data)
│
└── tamabee_{tenantDomain} (Customer Tenant DBs)
    ├── users
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
│   └── V2__init_settings.sql # Config data (plans, features)
│
├── tenant/                    # Tenant DB migrations (dùng chung cho tất cả tenants)
│   └── V1__init.sql          # Schema cho tenant (bao gồm Tamabee)
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

#### 2. Khởi động Application

Khi application khởi động:

1. Flyway chạy migrations cho Master DB (`db/master/`)
2. `TenantDataSourceLoader` tự động tạo database `tamabee_tamabee` nếu chưa có
3. Flyway chạy migrations cho Tamabee tenant (`db/tenant/`)
4. `DataInitializer` tạo Tamabee company (id=0) và admin user

**Không cần chạy script thủ công!**

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
- Plan ID: FREE_PLAN_ID (all features enabled)
- **Dùng chung Flyway migrations với các tenant khác** (`db/tenant/`)
