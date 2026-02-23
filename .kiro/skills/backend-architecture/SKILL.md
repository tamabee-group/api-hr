---
name: Backend Architecture
description: Spring Boot architecture, project structure, multi-tenant database patterns, cross-database query patterns (5 patterns), TenantFilter routing
---

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
├── datasource/             # Multi-tenant (TenantContext, TenantFilter, RegionContext, routing)
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
└── util/                   # Utility classes (RegionUtil, SecurityUtil, JwtUtil...)
```

## RegionContext & Timezone

Hệ thống dùng `region` (thuộc Company) để xác định timezone và business logic theo vùng.

### Flow: JWT → RegionContext → Timezone

```
Login → JwtUtil.generateAccessToken(..., region) → JWT chứa claim "region"
Request → JwtAuthenticationFilter đọc claim "region" → RegionContext.setCurrentRegion(region)
Service/Entity → RegionContext.getCurrentRegion() → RegionUtil.getTimezone(region) → ZoneId
Response → TenantFilter.doFilter() finally → RegionContext.clear()
```

### RegionContext (datasource/RegionContext.java)

ThreadLocal holder cho region code, tương tự TenantContext:

```java
RegionContext.setCurrentRegion("ja");           // Set từ JWT trong JwtAuthenticationFilter
String region = RegionContext.getCurrentRegion(); // Lấy region hiện tại (null nếu system operation)
RegionContext.clear();                           // Clear trong TenantFilter sau request
```

### RegionUtil (util/RegionUtil.java)

```java
RegionUtil.getTimezone("vi")       // → ZoneId.of("Asia/Ho_Chi_Minh")
RegionUtil.getTimezone("ja")       // → ZoneId.of("Asia/Tokyo")
RegionUtil.getTimezone(null)       // → ZoneId.of("UTC") (fallback)
RegionUtil.isValidRegion("vi")     // → true (chỉ "vi" và "ja" hợp lệ)
RegionUtil.toTimezoneString("ja")  // → "Asia/Tokyo"
RegionUtil.getCurrentLocale()      // → Accept-Language header (cho i18n, không liên quan region)
```

### RegionAwareAuditListener (config/RegionAwareAuditListener.java)

Thay thế Spring `AuditingEntityListener` trên BaseEntity. Tự động set `createdAt`/`updatedAt` theo timezone của region:

```java
@EntityListeners(RegionAwareAuditListener.class)
public abstract class BaseEntity { ... }
// → PrePersist/PreUpdate: LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()))
// → Fallback UTC nếu không có region (scheduler, system operation)
```

### Region values

| Region | Timezone         | Currency | Salary templates |
| ------ | ---------------- | -------- | ---------------- |
| `vi`   | Asia/Ho_Chi_Minh | VND      | Tiếng Việt       |
| `ja`   | Asia/Tokyo       | JPY      | Tiếng Nhật       |

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

## Multi-Tenant Database

### Database Structure

- **Master DB**: `companies`, `wallets`, `plans`, `employee_commissions`, `tamabee_settings`, `deposits`, `system_notifications`, `feedbacks`, `feedback_replies`
- **Tenant DB** (per company): `users`, `user_profiles`, `attendance_records`, `payroll_records`, `notifications`...

### TenantFilter Routing (datasource/TenantFilter.java)

```java
// MASTER_ONLY_PATHS - chỉ query master DB, không set tenant
"/api/admin/settings", "/api/admin/plans", "/api/admin/companies",
"/api/admin/deposits", "/api/admin/commissions", "/api/admin/commission-settings",
"/api/admin/system-notifications", "/api/admin/feedbacks"

// TAMABEE_TENANT_PATHS - set tenant = "tamabee" để query users từ tenant DB
"/api/admin/employees"
```

### Pattern 1: Service cần query Master DB + Tenant DB hiện tại

Dùng `@Qualifier("masterJdbcTemplate")` để inject JdbcTemplate cho master DB, kết hợp Repository cho tenant DB:

```java
@Service
public class MyServiceImpl {
    private final UserRepository userRepository;  // Tenant DB (theo TenantContext)
    private final JdbcTemplate masterJdbcTemplate;  // Master DB

    public MyServiceImpl(
            UserRepository userRepository,
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate) {
        this.userRepository = userRepository;
        this.masterJdbcTemplate = masterJdbcTemplate;
    }

    public void myMethod(Long userId) {
        // Query tenant DB (users) - dùng Repository
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId);

        // Query master DB (companies, wallets...) - dùng JdbcTemplate
        String sql = "SELECT * FROM companies WHERE id = ?";
        masterJdbcTemplate.query(sql, mapper, user.getCompanyId());
    }
}
```

### Pattern 2: Gửi notification/data đến tenant cụ thể (cross-tenant)

Dùng `TenantDataSourceManager.getDataSource(tenantDomain)` để lấy DataSource của tenant bất kỳ, tạo JdbcTemplate riêng:

```java
@Service
public class MyServiceImpl {
    private final TenantDataSourceManager tenantDataSourceManager;

    public void notifyUserInTenant(String tenantDomain, Long userId) {
        DataSource tenantDs = tenantDataSourceManager.getDataSource(tenantDomain);
        if (tenantDs == null) {
            log.error("Không tìm thấy DataSource cho tenant: {}", tenantDomain);
            return;
        }

        JdbcTemplate tenantJdbc = new JdbcTemplate(tenantDs);
        String sql = "INSERT INTO notifications (user_id, code, type, ...) VALUES (?, ?, ?, ...)";
        tenantJdbc.update(sql, userId, code, type);
    }
}
```

### Pattern 3: Lặp qua tất cả tenants (broadcast)

```java
Map<String, DataSource> allTenants = tenantDataSourceManager.getAllDataSources();
for (Map.Entry<String, DataSource> entry : allTenants.entrySet()) {
    String tenantDomain = entry.getKey();
    try {
        JdbcTemplate tenantJdbc = new JdbcTemplate(entry.getValue());
        List<Long> userIds = tenantJdbc.queryForList(
            "SELECT id FROM users WHERE role = ? AND deleted = false", Long.class, role);
        for (Long userId : userIds) {
            insertNotificationWithJdbc(tenantJdbc, userId, code, params, targetUrl, type);
        }
    } catch (Exception e) {
        log.error("Lỗi khi xử lý tenant {}: {}", tenantDomain, e.getMessage());
    }
}
```

### Pattern 4: Query tenant "tamabee" cụ thể

```java
DataSource tamabeeDs = tenantDataSourceManager.getDataSource("tamabee");
JdbcTemplate tamabeeJdbc = new JdbcTemplate(tamabeeDs);

List<Long> staffIds = tamabeeJdbc.queryForList(
    "SELECT id FROM users WHERE role IN ('ADMIN_TAMABEE', 'MANAGER_TAMABEE') AND deleted = false",
    Long.class);
```

### Pattern 5: Master DB entities dùng Repository bình thường

```java
@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotificationEntity, Long> {
    Page<SystemNotificationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

@Service
public class SystemNotificationServiceImpl {
    private final SystemNotificationRepository systemNotificationRepository; // Master DB
    private final TenantDataSourceManager tenantDataSourceManager; // Cross-tenant
}
```

### Rules

- **KHÔNG** dùng Repository cho tenant DB tables khi đang ở context khác tenant (cross-tenant)
- **ĐƯỢC** dùng Repository cho master DB entities (companies, wallets, system_notifications, feedbacks...)
- Dùng `@Qualifier("masterJdbcTemplate")` khi service cần query master DB bằng raw SQL
- Dùng `TenantDataSourceManager.getDataSource()` khi cần query tenant DB cụ thể
- **KHÔNG** switch `TenantContext` — tạo `JdbcTemplate` riêng cho mỗi tenant
- Wrap cross-tenant operations trong try-catch để lỗi 1 tenant không ảnh hưởng tenant khác
- Endpoint phải được thêm vào `TAMABEE_TENANT_PATHS` nếu cần query users table qua Repository
- Endpoint chỉ query master DB thêm vào `MASTER_ONLY_PATHS`
