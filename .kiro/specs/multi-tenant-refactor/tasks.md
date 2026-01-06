# Implementation Plan: Multi-Tenant Refactor (Backend)

## Overview

- Refactor backend hệ thống Tamabee HR sang kiến trúc multi-tenant với database riêng cho mỗi tenant. Bao gồm Tamabee như một tenant đặc biệt với tenantDomain = "tamabee".

- Khi kiro thực hiện task hãy phản hồi tôi bằng tiếng Việt.

## Tasks

### Phase 1: Backend Infrastructure

- [x] 1. Setup Multi-Tenant Core Components

  - [x] 1.1 Tạo TenantContext class (ThreadLocal holder)

    - Tạo file `filter/TenantContext.java`
    - Methods: setCurrentTenant(), getCurrentTenant(), clear()
    - _Requirements: 3.2_

  - [x] 1.2 Tạo TenantFilter class

    - Tạo file `filter/TenantFilter.java`
    - Extends OncePerRequestFilter
    - Extract tenantDomain từ JWT token
    - Set vào TenantContext, clear sau request
    - _Requirements: 3.1, 3.4_

  - [x] 1.3 Tạo TenantRoutingDataSource class

    - Tạo file `datasource/TenantRoutingDataSource.java`
    - Extends AbstractRoutingDataSource
    - Override determineCurrentLookupKey()
    - _Requirements: 3.3_

  - [x] 1.4 Tạo TenantDataSourceManager class

    - Tạo file `datasource/TenantDataSourceManager.java`
    - ConcurrentHashMap để lưu tenant DataSources
    - Methods: addTenant(), getDataSource(), removeTenant()
    - Luôn có sẵn "tamabee" DataSource
    - _Requirements: 2.5, 10.2_

  - [x] 1.5 Tạo TenantDatabaseInitializer class

    - Tạo file `datasource/TenantDatabaseInitializer.java`
    - CREATE DATABASE logic
    - Run Flyway migration
    - Insert default settings
    - _Requirements: 2.1, 2.3, 2.4_

  - [x] 1.6 Write property tests cho TenantContext lifecycle

    - **Property 3: Tenant Context Lifecycle**
    - **Validates: Requirements 3.1, 3.2, 3.4**

- [x] 2. Checkpoint - Verify core components compile

  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Database Schema và Flyway Migration

  - [x] 3.1 Tạo Master DB migration file

    - Tạo file `db/master/V1__init.sql`
    - Tables: companies, users, plans, plan_features, plan_feature_codes, wallets, deposits, commissions
    - Thêm tenant_domain column vào companies
    - Insert Tamabee company (id=0, tenantDomain="tamabee")
    - _Requirements: 5.1, 9.2_

  - [x] 3.2 Tạo Tenant DB migration template

    - Tạo file `db/tenant/V1__init.sql`
    - Tables: user_profiles, company_settings, attendance_records, payroll_records, leave_requests
    - Áp dụng soft delete strategy (chỉ entities cần thiết)
    - _Requirements: 5.2, 9.3_

  - [x] 3.3 Tạo database tamabee_tamabee cho Tamabee

    - Pre-create database cho Tamabee company
    - Run migration V1
    - _Requirements: 2.2_

  - [x] 3.4 Cấu hình Flyway cho multi-tenant
    - Tạo file `config/FlywayMultiTenantConfig.java`
    - Separate locations cho master và tenant
    - _Requirements: 9.1_

- [x] 4. Refactor BaseEntity và Soft Delete

  - [x] 4.1 Bỏ deleted field khỏi BaseEntity

    - Sửa file `entity/BaseEntity.java`
    - Chỉ giữ id, createdAt, updatedAt
    - _Requirements: 6.1_

  - [x] 4.2 Thêm deleted field vào entities cần soft delete

    - User, UserProfile, Company, CompanyProfile, CompanySetting
    - Plan, PlanFeature, PlanFeatureCode
    - ShiftTemplate, WorkSchedule, Holiday
    - EmployeeSalary, EmployeeAllowance, EmployeeDeduction
    - EmploymentContract, DepositRequest, Wallet, TamabeeSetting
    - _Requirements: 6.2, 6.3_

  - [x] 4.3 Xóa deleted field khỏi entities không cần soft delete

    - AttendanceRecord, BreakRecord, PayrollRecord, PayrollItem, PayrollPeriod
    - WalletTransaction, AuditLog, WorkModeChangeLog, MailHistory
    - LeaveRequest, LeaveBalance, AttendanceAdjustmentRequest, ShiftSwapRequest
    - ShiftAssignment, WorkScheduleAssignment, ScheduleSelection
    - EmployeeCommission, EmailVerification
    - _Requirements: 6.4_

  - [x] 4.4 Update repositories cho soft delete pattern

    - Entities có soft delete: giữ `AndDeletedFalse` vào queries
    - Entities không có soft delete: bỏ filter deleted, dùng findById thay vì findByIdAndDeletedFalse
    - Đã cập nhật các repositories: LeaveBalanceRepository, AttendanceAdjustmentRequestRepository, ShiftSwapRequestRepository, ShiftAssignmentRepository, WorkScheduleAssignmentRepository, ScheduleSelectionRepository, EmployeeCommissionRepository
    - Đã cập nhật các services: ShiftServiceImpl, AttendanceServiceImpl, BreakServiceImpl, AttendanceAdjustmentServiceImpl, NotificationEmailServiceImpl, PayrollPeriodServiceImpl
    - _Requirements: 6.5, 6.6_

  - [ ]\* 4.5 Write property tests cho soft delete query behavior
    - **Property 8: Soft Delete Query Filtering**
    - **Validates: Requirements 6.5, 6.6**

- [x] 5. Checkpoint - Verify entity changes compile

  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. JWT Enhancement

  - [x] 6.1 Update JwtService để include tenantDomain và planId

    - Sửa file `service/core/JwtService.java`
    - Thêm tenantDomain, planId vào JWT claims
    - Tamabee users: tenantDomain = "tamabee", planId = null
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 6.2 Update JwtFilter để validate tenantDomain

    - Verify tenantDomain trong JWT match với user's company
    - _Requirements: 4.5_

  - [x] 6.3 Write property tests cho JWT payload
    - **Property 6: JWT Payload Completeness**
    - **Property 7: JWT Tenant Validation**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.5**

- [x] 7. Tenant Domain Registration API

  - [x] 7.1 Tạo API check-domain availability

    - Endpoint: GET /api/auth/check-domain?domain=xxx
    - Validate format, check reserved words, check existence
    - _Requirements: 1.1, 1.3, 1.5_

  - [x] 7.2 Update registration flow để include tenantDomain

    - Sửa RegisterRequest DTO
    - Sửa AuthService để lưu tenantDomain
    - _Requirements: 1.4_

  - [x] 7.3 Write property tests cho domain validation
    - **Property 1: Tenant Domain Validation**
    - **Property 2: Reserved Domain Rejection**
    - **Validates: Requirements 1.1, 1.3, 1.5**

- [x] 8. Tenant Provisioning Service

  - [x] 8.1 Tạo TenantProvisioningService

    - Orchestrate database creation, migration, DataSource registration
    - Handle async provisioning
    - _Requirements: 2.1, 2.3, 2.4, 2.5_

  - [x] 8.2 Update CompanyService để trigger provisioning

    - Gọi TenantProvisioningService khi tạo company
    - Handle provisioning failure
    - _Requirements: 2.6_

  - [x] 8.3 Tạo TenantDataSourceLoader cho app startup

    - Load all active tenant DataSources khi app khởi động
    - Bao gồm "tamabee" DataSource
    - _Requirements: 2.7_

  - [ ]\* 8.4 Write property tests cho tenant routing
    - **Property 4: Tenant Database Routing**
    - **Property 5: Cross-Tenant Isolation**
    - **Validates: Requirements 3.3, 5.4, 5.5**

- [x] 9. Plan Features API

  - [x] 9.1 Tạo PlanFeaturesController

    - Endpoint: GET /api/plans/{planId}/features
    - Endpoint: GET /api/plans/all-features (cho Tamabee users)
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 9.2 Tạo PlanFeaturesService

    - Lấy features theo planId
    - Return all features enabled khi planId = null
    - _Requirements: 7.4_

  - [x] 9.3 Tạo PlanFeaturesResponse DTO

    - planId, planName, features array
    - _Requirements: 7.3_

  - [x] 9.4 Write property tests cho Plan Features API
    - **Property 9: Plan Features API Response**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

- [x] 10. Tamabee Special Tenant Handling

  - [x] 10.1 Update AuthService cho Tamabee login

    - Set tenantDomain = "tamabee" cho Tamabee users
    - Set planId = null (all features enabled)
    - _Requirements: 8.1, 8.2_

  - [x] 10.2 Verify Tamabee routing to tamabee_tamabee

    - Test HR queries route to correct database
    - _Requirements: 8.3, 8.4_

  - [x] 10.3 Update security config cho Tamabee admins

    - Allow access to both /api/admin/\* và /api/tenant/\*
    - _Requirements: 8.5_

  - [x] 10.4 Write property tests cho Tamabee handling
    - **Property 10: Tamabee Special Tenant Handling**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4**

- [x] 11. Tenant Deprovisioning

  - [x] 11.1 Update CompanyService cho deactivation

    - Set status = INACTIVE
    - Remove DataSource from pool
    - _Requirements: 10.1, 10.2_

  - [x] 11.2 Tạo TenantCleanupScheduler (optional)
    - Scheduled job để archive/delete old tenant databases
    - _Requirements: 10.3, 10.4_

- [x] 12. Checkpoint - Backend complete
  - Ensure all tests pass, ask the user if questions arise.

### Phase 2: Integration Testing

- [x] 13. Integration Testing

  - [x] 13.1 Test end-to-end registration flow

    - Register company với tenant domain
    - Verify database created
    - Verify login works
    - _Requirements: 1.4, 2.1_

  - [x] 13.2 Test tenant isolation

    - Create 2 tenants
    - Verify data isolation
    - _Requirements: 5.5_

  - [x] 13.3 Test Tamabee special tenant

    - Login as Tamabee user
    - Verify tenantDomain = "tamabee"
    - Verify HR queries route to tamabee_tamabee
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [x] 13.4 Test Plan Features API
    - Fetch features for different plans
    - Verify Tamabee gets all features
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 14. Final Checkpoint
  - Ensure all tests pass, ask the user if questions arise.
  - Review all changes
  - Verify no regressions

## Notes

- Tasks marked with `*` are optional property-based tests
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Frontend refactor is in separate spec: `tama-hr/.kiro/specs/multi-tenant-frontend/`
