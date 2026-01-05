# Requirements Document

## Introduction

Refactor hệ thống Tamabee HR từ single-database sang multi-tenant architecture với database riêng cho mỗi tenant (company). Đồng thời tối ưu soft delete strategy để cải thiện performance.

## Glossary

- **Tenant**: Một công ty sử dụng hệ thống Tamabee (bao gồm cả Tamabee company với tenantDomain = "tamabee")
- **Tenant_Domain**: Subdomain riêng cho mỗi công ty (ví dụ: `acme` trong `acme.tamabee.com`, `tamabee` cho Tamabee company)
- **Master_DB**: Database chung lưu thông tin companies, users, plans, billing
- **Tenant_DB**: Database riêng cho mỗi tenant lưu attendance, payroll, leaves (kể cả `tamabee_tamabee` cho Tamabee)
- **TenantContext**: ThreadLocal lưu trữ tenantDomain trong mỗi request
- **TenantFilter**: Filter đọc tenantDomain từ JWT token
- **TenantRoutingDataSource**: DataSource routing đến đúng tenant database
- **Soft_Delete**: Đánh dấu deleted = true thay vì xóa thẳng record

## Requirements

### Requirement 1: Tenant Domain Registration

**User Story:** As a company owner, I want to choose a unique tenant domain during registration, so that my company has a dedicated subdomain.

#### Acceptance Criteria

1. WHEN a user enters a tenant domain in Step 1 of registration, THE Registration_Form SHALL validate the format (lowercase, numbers, hyphens, 3-30 chars)
2. WHEN a user enters a tenant domain, THE System SHALL check availability via API with 500ms debounce
3. IF the tenant domain is already taken or reserved, THEN THE System SHALL display an error message and prevent proceeding
4. WHEN registration completes, THE System SHALL store tenantDomain in companies table as unique identifier
5. THE System SHALL reject reserved domains: admin, api, www, app, mail, tamabee

### Requirement 2: Tenant Database Provisioning

**User Story:** As a system administrator, I want new tenant databases to be automatically created when a company registers, so that each tenant has isolated data storage.

#### Acceptance Criteria

1. WHEN a new company is created, THE TenantDatabaseInitializer SHALL create a new database named `tamabee_{tenantDomain}`
2. THE System SHALL have a pre-created database `tamabee_tamabee` for Tamabee company (companyId = 0)
3. WHEN a tenant database is created, THE System SHALL run Flyway migration V1 (single migration file for new app)
4. WHEN migrations complete, THE System SHALL insert default company settings
5. WHEN database provisioning completes, THE TenantDataSourceManager SHALL register the new DataSource
6. IF database creation fails, THEN THE System SHALL set company status to FAILED and log the error
7. WHEN the application starts, THE TenantDataSourceLoader SHALL load all active tenant DataSources including "tamabee"

### Requirement 3: Tenant Request Routing

**User Story:** As a backend developer, I want requests to be automatically routed to the correct tenant database, so that data isolation is guaranteed.

#### Acceptance Criteria

1. WHEN a request arrives with JWT token, THE TenantFilter SHALL extract tenantDomain from the token
2. WHEN tenantDomain is extracted, THE TenantFilter SHALL store it in TenantContext (ThreadLocal)
3. WHEN a repository executes a query, THE TenantRoutingDataSource SHALL route to the correct tenant database
4. WHEN the request completes, THE TenantFilter SHALL clear TenantContext to prevent memory leaks
5. IF tenantDomain is missing for tenant-required endpoints, THEN THE System SHALL return 401 Unauthorized

### Requirement 4: JWT Token Enhancement

**User Story:** As a security engineer, I want JWT tokens to include tenant information, so that tenant context can be established for each request.

#### Acceptance Criteria

1. WHEN a user logs in, THE AuthService SHALL include tenantDomain in the JWT payload
2. WHEN a Tamabee user logs in, THE AuthService SHALL set tenantDomain = "tamabee" in JWT
3. WHEN a user logs in, THE AuthService SHALL include planId in the JWT payload
4. THE JWT_Payload SHALL contain: userId, email, role, tenantDomain, planId, companyId
5. WHEN validating JWT, THE System SHALL verify tenantDomain matches user's company

### Requirement 5: Master/Tenant Data Separation

**User Story:** As a database architect, I want clear separation between master and tenant data, so that the system can scale efficiently.

#### Acceptance Criteria

1. THE Master_DB SHALL store: companies, users (basic auth), plans, wallets, deposits, commissions
2. THE Tenant_DB SHALL store: user_profiles, company_settings, attendance_records, payroll_records, leave_requests
3. WHEN querying master data, THE System SHALL use master repositories
4. WHEN querying tenant data, THE System SHALL use tenant repositories with automatic routing
5. THE System SHALL NOT allow cross-tenant queries from tenant repositories

### Requirement 6: Soft Delete Optimization

**User Story:** As a performance engineer, I want to optimize soft delete strategy, so that high-volume tables don't suffer from unnecessary filtering.

#### Acceptance Criteria

1. THE BaseEntity SHALL NOT contain deleted field
2. WHEN an entity needs soft delete capability, THE Entity SHALL define its own deleted field
3. THE following entities SHALL have soft delete: User, UserProfile, Company, CompanyProfile, CompanySetting, Plan, PlanFeature, PlanFeatureCode, ShiftTemplate, WorkSchedule, Holiday, EmployeeSalary, EmployeeAllowance, EmployeeDeduction, EmploymentContract, DepositRequest, Wallet, TamabeeSetting
4. THE following entities SHALL NOT have soft delete (hard delete): AttendanceRecord, BreakRecord, PayrollRecord, PayrollItem, PayrollPeriod, WalletTransaction, AuditLog, WorkModeChangeLog, MailHistory, LeaveRequest, LeaveBalance, AttendanceAdjustmentRequest, ShiftSwapRequest, ShiftAssignment, WorkScheduleAssignment, ScheduleSelection, EmployeeCommission, EmailVerification
5. WHEN querying entities with soft delete, THE Repository SHALL filter by deleted = false
6. WHEN querying entities without soft delete, THE Repository SHALL NOT filter by deleted

### Requirement 7: Plan Features API

**User Story:** As a frontend developer, I want an API to fetch plan features, so that the sidebar can be dynamically generated.

#### Acceptance Criteria

1. THE System SHALL provide GET /api/plans/{planId}/features endpoint
2. WHEN called, THE API SHALL return list of features with code and enabled status
3. THE Response SHALL include: planId, planName, features array
4. WHEN user has no planId (Tamabee users), THE System SHALL return all features enabled

### Requirement 8: Tamabee as Special Tenant

**User Story:** As a system architect, I want Tamabee to be treated as a special tenant, so that Tamabee employees can use HR features.

#### Acceptance Criteria

1. THE System SHALL treat Tamabee company (companyId = 0) as a tenant with tenantDomain = "tamabee"
2. WHEN a Tamabee user logs in, THE System SHALL set tenantDomain = "tamabee" in JWT
3. THE System SHALL have database `tamabee_tamabee` for Tamabee HR data
4. WHEN Tamabee users access HR features, THE System SHALL route to `tamabee_tamabee` database
5. THE System SHALL allow Tamabee admins to access both /api/admin/\* (platform) and tenant APIs (HR)

### Requirement 9: Database Migration Strategy

**User Story:** As a DevOps engineer, I want a clear migration strategy, so that the new architecture can be deployed cleanly.

#### Acceptance Criteria

1. THE System SHALL use single Flyway V1 migration file for fresh start (new app)
2. THE Master_DB migration SHALL be in `db/master/V1__init.sql`
3. THE Tenant_DB migration template SHALL be in `db/tenant/V1__init.sql`
4. WHEN creating new tenant, THE System SHALL copy and run tenant migration template
5. THE Migration SHALL create all tables, indexes, and constraints in one file

### Requirement 10: Tenant Deprovisioning

**User Story:** As a system administrator, I want to safely deactivate tenants, so that inactive companies don't consume resources.

#### Acceptance Criteria

1. WHEN a company is deactivated, THE System SHALL mark company status as INACTIVE
2. WHEN a company is deactivated, THE TenantDataSourceManager SHALL remove the DataSource from pool
3. THE System SHALL keep tenant database for 90 days for compliance
4. WHEN 90 days pass, THE System SHALL archive and delete the tenant database
5. IF a company is reactivated within 90 days, THE System SHALL restore the DataSource
