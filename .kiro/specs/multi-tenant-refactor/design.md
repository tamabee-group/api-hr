# Design Document: Multi-Tenant Refactor

## Overview

Thiết kế chi tiết cho việc refactor hệ thống Tamabee HR sang kiến trúc multi-tenant với database riêng cho mỗi tenant. Document này bao gồm architecture, components, data models, và implementation details.

**Key Concept: Tamabee as Special Tenant**

- Tamabee company (companyId = 0) được xử lý như một tenant đặc biệt với `tenantDomain = "tamabee"`
- Tamabee employees cũng cần sử dụng HR features (attendance, payroll) giống như tenant users
- Database `tamabee_tamabee` lưu trữ HR data của Tamabee
- Tamabee admins có thể truy cập cả platform management (/api/admin/_) và HR features (/api/tenant/_)

Tham khảo chi tiết tại: `tama-hr/docs/saas-multi-tenant-architecture.md`

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              BACKEND                                     │
│                      (Spring Boot + JPA)                                │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                      API Layer                                    │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │  │
│  │  │ /api/admin/*    │  │ /api/tenant/*   │  │ /api/plans/*    │  │  │
│  │  │ Platform Mgmt   │  │ HR Features     │  │ Plan Features   │  │  │
│  │  │ (Tamabee only)  │  │ (All tenants)   │  │ (Public)        │  │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                      Request Processing                           │  │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │  │
│  │  │ TenantFilter    │→ │ TenantContext   │→ │ TenantRouting   │  │  │
│  │  │ (JWT → Domain)  │  │ (ThreadLocal)   │  │ DataSource      │  │  │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Repository Layer                              │   │
│  │  ┌─────────────────────┐      ┌─────────────────────┐          │   │
│  │  │  Master Repositories│      │  Tenant Repositories │          │   │
│  │  │  - CompanyRepo      │      │  - AttendanceRepo    │          │   │
│  │  │  - UserRepo         │      │  - PayrollRepo       │          │   │
│  │  │  - PlanRepo         │      │  - LeaveRepo         │          │   │
│  │  └─────────────────────┘      └─────────────────────┘          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           DATABASE LAYER                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐    ┌──────────────────────────────────────────┐  │
│  │   MASTER DB      │    │           TENANT DATABASES               │  │
│  │   (tamabee_hr)   │    │                                          │  │
│  │                  │    │  ┌────────────┐  ┌────────────┐          │  │
│  │  • companies     │    │  │ tamabee_   │  │ tamabee_   │          │  │
│  │  • users         │    │  │  tamabee   │  │   acme     │  ...     │  │
│  │  • plans         │    │  │ (Tamabee)  │  │ (Tenant)   │          │  │
│  │  • wallets       │    │  │            │  │            │          │  │
│  │  • deposits      │    │  │ • profiles │  │ • profiles │          │  │
│  │  • commissions   │    │  │ • settings │  │ • settings │          │  │
│  └──────────────────┘    │  │ • attendance│ │ • attendance│         │  │
│                          │  │ • payroll  │  │ • payroll  │          │  │
│                          │  └────────────┘  └────────────┘          │  │
│                          └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### Backend Components

#### 1. TenantContext

```java
/**
 * ThreadLocal holder cho tenant domain.
 * Mỗi request có tenant riêng, không ảnh hưởng request khác.
 */
public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantDomain) {
        CURRENT_TENANT.set(tenantDomain);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
```

#### 2. TenantFilter

```java
/**
 * Filter đọc tenantDomain từ JWT và lưu vào TenantContext.
 * Chạy trước tất cả controllers.
 * Tamabee users có tenantDomain = "tamabee".
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        try {
            String tenantDomain = extractTenantFromJwt(request);
            if (tenantDomain != null) {
                TenantContext.setCurrentTenant(tenantDomain);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();  // Luôn clear để tránh memory leak
        }
    }

    private String extractTenantFromJwt(HttpServletRequest request) {
        // Đọc JWT từ Authorization header
        // Decode và lấy tenantDomain claim
        // Tamabee users: tenantDomain = "tamabee"
        // Company users: tenantDomain = company.tenantDomain
    }
}
```

#### 3. TenantRoutingDataSource

```java
/**
 * DataSource routing dựa trên TenantContext.
 * Spring tự động gọi determineCurrentLookupKey() trước mỗi query.
 * "tamabee" → tamabee_tamabee database
 * "acme" → tamabee_acme database
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenant();
    }
}
```

#### 4. TenantDataSourceManager

```java
/**
 * Quản lý DataSource pool cho tất cả tenants.
 * Hỗ trợ thêm/xóa tenant runtime.
 * Luôn có sẵn "tamabee" DataSource cho Tamabee company.
 */
@Component
public class TenantDataSourceManager {
    private final Map<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();

    public void addTenant(String tenantDomain) { /* ... */ }
    public DataSource getDataSource(String tenantDomain) { /* ... */ }
    public void removeTenant(String tenantDomain) { /* ... */ }
}
```

#### 5. TenantDatabaseInitializer

```java
/**
 * Tạo database mới cho tenant.
 * Chạy Flyway migration và insert default data.
 * Database tamabee_tamabee được tạo sẵn cho Tamabee.
 */
@Service
public class TenantDatabaseInitializer {

    public void createTenantDatabase(String tenantDomain) {
        String dbName = "tamabee_" + tenantDomain;
        // 1. CREATE DATABASE
        // 2. Run Flyway migration
        // 3. Insert default settings
    }
}
```

#### 6. PlanFeaturesController

```java
/**
 * API endpoint để frontend lấy plan features.
 * Dùng cho dynamic sidebar.
 */
@RestController
@RequestMapping("/api/plans")
public class PlanFeaturesController {

    @GetMapping("/{planId}/features")
    public ResponseEntity<BaseResponse<PlanFeaturesResponse>> getFeatures(
            @PathVariable Long planId) {
        // Return list of features with code and enabled status
    }

    @GetMapping("/all-features")
    public ResponseEntity<BaseResponse<PlanFeaturesResponse>> getAllFeatures() {
        // Return all features enabled (for Tamabee users without planId)
    }
}
```

#### 7. PlanFeaturesResponse

```java
/**
 * Response DTO cho Plan Features API.
 */
@Data
@Builder
public class PlanFeaturesResponse {
    private Long planId;
    private String planName;
    private List<FeatureItem> features;

    @Data
    @Builder
    public static class FeatureItem {
        private String code;
        private String name;
        private boolean enabled;
    }
}
```

## Data Models

### Master Database Schema

```sql
-- db/master/V1__init.sql

CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    tenant_domain VARCHAR(50) UNIQUE NOT NULL,
    plan_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Tamabee company (companyId = 0, tenantDomain = "tamabee")
INSERT INTO companies (id, name, email, tenant_domain, status)
VALUES (0, 'Tamabee', 'admin@tamabee.com', 'tamabee', 'ACTIVE');

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(10) UNIQUE,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    company_id BIGINT REFERENCES companies(id),
    tenant_domain VARCHAR(50),  -- "tamabee" for Tamabee users
    status VARCHAR(20) DEFAULT 'ACTIVE',
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE plans (
    id BIGSERIAL PRIMARY KEY,
    name_vi VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    name_ja VARCHAR(100) NOT NULL,
    monthly_price DECIMAL(15,2) NOT NULL,
    max_employees INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE plan_features (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT REFERENCES plans(id),
    feature_code VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE plan_feature_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name_vi VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    name_ja VARCHAR(100) NOT NULL,
    description TEXT,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- wallets, deposits, commissions... (có soft delete)
```

### Tenant Database Schema

```sql
-- db/tenant/V1__init.sql
-- Áp dụng cho cả tamabee_tamabee và tamabee_{tenantDomain}

CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE company_settings (
    id BIGSERIAL PRIMARY KEY,
    attendance_config JSONB,
    payroll_config JSONB,
    overtime_config JSONB,
    break_config JSONB,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- attendance_records, payroll_records... (KHÔNG có soft delete)
CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    check_in_time TIMESTAMP,
    check_out_time TIMESTAMP,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
    -- KHÔNG có deleted field
);
```

### Tamabee Special Tenant

| Attribute    | Value                                            |
| ------------ | ------------------------------------------------ |
| companyId    | 0                                                |
| tenantDomain | "tamabee"                                        |
| database     | tamabee_tamabee                                  |
| planId       | null (all features)                              |
| roles        | ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE |

**Tamabee User JWT Payload:**

```json
{
  "userId": 1,
  "email": "admin@tamabee.com",
  "role": "ADMIN_TAMABEE",
  "tenantDomain": "tamabee",
  "planId": null,
  "companyId": 0
}
```

**Regular Tenant User JWT Payload:**

```json
{
  "userId": 100,
  "email": "user@acme.com",
  "role": "ADMIN_COMPANY",
  "tenantDomain": "acme",
  "planId": 2,
  "companyId": 5
}
```

### Soft Delete Strategy

| Entity Type                          | Has Soft Delete | Reason                      |
| ------------------------------------ | --------------- | --------------------------- |
| User, Company, Plan                  | ✅ Yes          | Ít data, cần khôi phục      |
| ShiftTemplate, WorkSchedule          | ✅ Yes          | Template có thể tái sử dụng |
| EmployeeSalary, Allowance, Deduction | ✅ Yes          | Cấu hình quan trọng         |
| AttendanceRecord, BreakRecord        | ❌ No           | Data lớn, tăng liên tục     |
| PayrollRecord, PayrollItem           | ❌ No           | Data lớn mỗi kỳ lương       |
| WalletTransaction, AuditLog          | ❌ No           | Log data, có thể archive    |

## Correctness Properties

_A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do._

### Property 1: Tenant Domain Validation

_For any_ input string, the tenant domain validation function SHALL return true only if the string contains only lowercase letters, numbers, and hyphens, has length between 3-30 characters, and does not start or end with a hyphen.

**Validates: Requirements 1.1**

### Property 2: Reserved Domain Rejection

_For any_ reserved domain (admin, api, www, app, mail, tamabee), the domain availability check SHALL return false.

**Validates: Requirements 1.3, 1.5**

### Property 3: Tenant Context Lifecycle

_For any_ HTTP request with valid JWT containing tenantDomain, the TenantContext SHALL contain the correct tenantDomain during request processing and SHALL be cleared after the request completes.

**Validates: Requirements 3.1, 3.2, 3.4**

### Property 4: Tenant Database Routing

_For any_ tenant repository query, when TenantContext contains tenantDomain X, the query SHALL execute against database `tamabee_X` and NOT against any other tenant database.

**Validates: Requirements 3.3, 5.4**

### Property 5: Cross-Tenant Isolation

_For any_ two different tenants A and B, queries executed in tenant A's context SHALL NOT return data from tenant B's database.

**Validates: Requirements 5.5**

### Property 6: JWT Payload Completeness

_For any_ successful login, the generated JWT SHALL contain all required fields: userId, email, role, tenantDomain (including "tamabee" for Tamabee users), planId, and companyId.

**Validates: Requirements 4.1, 4.2, 4.3, 4.4**

### Property 7: JWT Tenant Validation

_For any_ JWT with tenantDomain claim, the system SHALL reject the token if tenantDomain does not match the user's company's tenantDomain in the database.

**Validates: Requirements 4.5**

### Property 8: Soft Delete Query Filtering

_For any_ entity with soft delete capability, repository queries SHALL only return records where deleted = false. _For any_ entity without soft delete, repository queries SHALL return all records regardless of any deleted field.

**Validates: Requirements 6.5, 6.6**

### Property 9: Plan Features API Response

_For any_ valid planId, the Plan Features API SHALL return all features associated with that plan. _For any_ request without planId (Tamabee users), the API SHALL return all features with enabled = true.

**Validates: Requirements 7.1, 7.2, 7.3, 7.4**

### Property 10: Tamabee Special Tenant Handling

_For any_ Tamabee user (companyId = 0), the system SHALL set tenantDomain = "tamabee" in JWT and route HR queries to `tamabee_tamabee` database.

**Validates: Requirements 8.1, 8.2, 8.3, 8.4**

## Error Handling

### Backend Errors

| Error Case                   | ErrorCode                    | HTTP Status |
| ---------------------------- | ---------------------------- | ----------- |
| Missing tenantDomain in JWT  | `TENANT_REQUIRED`            | 401         |
| Invalid tenantDomain format  | `INVALID_TENANT_DOMAIN`      | 400         |
| Tenant domain already taken  | `TENANT_DOMAIN_EXISTS`       | 409         |
| Reserved tenant domain       | `TENANT_DOMAIN_RESERVED`     | 400         |
| Tenant database not found    | `TENANT_NOT_FOUND`           | 404         |
| Database provisioning failed | `TENANT_PROVISIONING_FAILED` | 500         |
| Plan not found               | `PLAN_NOT_FOUND`             | 404         |
| Unauthorized access          | `UNAUTHORIZED`               | 401         |
| Forbidden access             | `FORBIDDEN`                  | 403         |

### Error Response Format

```java
@Data
@Builder
public class ErrorResponse {
    private boolean success = false;
    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
}
```

## Testing Strategy

### Unit Tests

- TenantContext: set/get/clear operations
- TenantFilter: JWT extraction, context setting
- Domain validation: format rules, reserved words
- PlanFeaturesService: feature retrieval logic
- Tamabee special tenant handling

### Property-Based Tests (jqwik)

- **Property 1**: Generate random strings, verify validation rules
- **Property 2**: Test all reserved domains are rejected
- **Property 3**: Generate requests, verify context lifecycle
- **Property 5**: Generate multi-tenant scenarios, verify isolation
- **Property 8**: Generate queries, verify soft delete filtering
- **Property 9**: Generate plan/feature combinations, verify API response
- **Property 10**: Generate Tamabee user scenarios, verify routing

### Integration Tests

- Tenant provisioning: create company → verify database exists
- Request routing: set context → verify correct database queried
- JWT flow: login → verify token contains all fields
- Plan Features API: fetch features → verify response format
- Tamabee tenant: login as Tamabee user → verify tenantDomain = "tamabee"

### Test Configuration

```java
// Property-based tests với jqwik
@Property(tries = 100)
void tenantDomainValidation(@ForAll @StringLength(min = 1, max = 50) String input) {
    // Test validation logic
}

@Property(tries = 100)
void tamabeeUserAlwaysHasTenantDomain(@ForAll @From("tamabeeUsers") UserEntity user) {
    // Verify Tamabee users get tenantDomain = "tamabee"
}
```
