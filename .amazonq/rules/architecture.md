# Architecture Rules

## Project Structure
Follow strict layered architecture: Controller → Service (Interface + Impl) → Mapper → Repository → Entity

Package organization by business domain:
- **admin**: Tamabee internal management (manage companies, deposits, plans)
- **company**: Company self-management (manage employees, view wallet)
- **core**: Shared components (entities, repositories, security, utils)
- **mapper**: DTO ↔ Entity conversion (organized by domain)

```
src/main/java/com/tamabee/apihr/
├── admin/                          # TAMABEE INTERNAL MANAGEMENT
│   ├── controller/                 # Admin REST endpoints
│   │   ├── AdminCompanyController.java
│   │   ├── AdminDepositController.java
│   │   ├── AdminPlanController.java
│   │   ├── AdminWalletController.java
│   │   └── AdminUserController.java
│   ├── service/                    # Admin business logic
│   │   ├── IAdminCompanyService.java
│   │   ├── IAdminDepositService.java
│   │   ├── IAdminPlanService.java
│   │   ├── IAdminWalletService.java
│   │   └── impl/
│   │       ├── AdminCompanyServiceImpl.java
│   │       ├── AdminDepositServiceImpl.java
│   │       ├── AdminPlanServiceImpl.java
│   │       └── AdminWalletServiceImpl.java
│   ├── dto/                        # Admin DTOs
│   │   ├── request/
│   │   └── response/
│
├── company/                        # COMPANY SELF-MANAGEMENT
│   ├── controller/                 # Company REST endpoints
│   │   ├── CompanyEmployeeController.java
│   │   ├── CompanyProfileController.java
│   │   ├── CompanyWalletController.java
│   │   └── CompanyDepositRequestController.java
│   ├── service/                    # Company business logic
│   │   ├── ICompanyEmployeeService.java
│   │   ├── ICompanyProfileService.java
│   │   ├── ICompanyWalletService.java
│   │   └── impl/
│   │       ├── CompanyEmployeeServiceImpl.java
│   │       ├── CompanyProfileServiceImpl.java
│   │       └── CompanyWalletServiceImpl.java
│   ├── dto/                        # Company DTOs
│   │   ├── request/
│   │   └── response/
│
├── mapper/                         # DTO ↔ ENTITY MAPPING
│   ├── core/                       # Core entity mappers
│   │   ├── UserMapper.java
│   │   ├── CompanyMapper.java
│   │   └── WalletMapper.java
│   ├── admin/                      # Admin-specific mappers
│   │   ├── AdminCompanyMapper.java
│   │   └── AdminUserMapper.java
│   └── company/                    # Company-specific mappers
│       ├── CompanySettingMapper.java
│       └── CompanyProfileMapper.java
│
└── core/                           # SHARED COMPONENTS
    ├── entity/                     # JPA entities (shared by all)
    │   ├── User.java
    │   ├── Company.java
    │   ├── Wallet.java
    │   ├── Plan.java
    │   ├── WalletTransaction.java
    │   ├── DepositRequest.java
    │   ├── Referral.java
    │   ├── TemporaryPassword.java
    │   └── BaseEntity.java
    ├── repository/                 # Data access (shared by all)
    │   ├── UserRepository.java
    │   ├── CompanyRepository.java
    │   ├── WalletRepository.java
    │   ├── PlanRepository.java
    │   ├── WalletTransactionRepository.java
    │   ├── DepositRequestRepository.java
    │   ├── ReferralRepository.java
    │   └── TemporaryPasswordRepository.java
    ├── enums/                      # Enumerations (shared)
    │   ├── UserRole.java
    │   ├── TransactionType.java
    │   └── DepositStatus.java
    ├── security/                   # Security components
    │   ├── JwtTokenProvider.java
    │   ├── JwtAuthenticationFilter.java
    │   ├── SecurityService.java
    │   └── CustomUserDetailsService.java
    ├── config/                     # Configuration classes
    │   ├── SecurityConfig.java
    │   ├── JwtConfig.java
    │   └── WebConfig.java
    ├── exception/                  # Exception handling
    │   ├── GlobalExceptionHandler.java
    │   ├── ResourceNotFoundException.java
    │   ├── UnauthorizedException.java
    │   ├── InsufficientBalanceException.java
    │   ├── InvalidTemporaryPasswordException.java
    │   └── DuplicateEmployeeCodeException.java
    ├── util/                       # Utility classes
    │   ├── SecurityUtils.java
    │   ├── ValidationUtils.java
    │   ├── DateUtils.java
    │   └── EmployeeCodeGenerator.java
    ├── dto/                        # Shared DTOs
    │   └── BaseResponse.java
    └── service/                    # Shared services
        ├── IEmailService.java
        ├── IAuthService.java
        ├── IEmailVerificationService.java
        └── impl/
            ├── EmailServiceImpl.java
            ├── AuthServiceImpl.java
            └── EmailVerificationServiceImpl.java
```

## Package Responsibilities

### Admin Package (admin/)
**Purpose**: Tamabee internal management - support and manage all companies

**Controllers**:
- AdminCompanyController: CRUD companies, view all companies
- AdminDepositController: Approve/reject deposit requests
- AdminPlanController: Manage subscription plans
- AdminWalletController: Add money to company wallets, view transactions
- AdminUserController: Manage Tamabee internal users

**Services**:
- Business logic for Tamabee operations
- Approve deposits (ADMIN_TAMABEE, MANAGER_TAMABEE only)
- Add money to wallets
- Manage plans and pricing
- Process referral commissions

**Access Control**:
- @PreAuthorize("hasAnyRole('ADMIN_TAMABEE', 'MANAGER_TAMABEE')")
- EMPLOYEE_TAMABEE: Can only create deposit verification requests
- No companyId filtering (access all companies)

### Company Package (company/)
**Purpose**: Company self-management - manage own data only

**Controllers**:
- CompanyEmployeeController: Manage company employees
- CompanyProfileController: Update company info
- CompanyWalletController: View wallet balance and transactions
- CompanyDepositRequestController: Create deposit requests

**Services**:
- Business logic for company operations
- Auto-extract companyId from JWT token
- Filter all queries by companyId
- Cannot access other companies' data

**Access Control**:
- @PreAuthorize("hasAnyRole('ADMIN_COMPANY', 'MANAGER_COMPANY')")
- Always validate: tokenCompanyId == requestCompanyId
- Automatic companyId injection from JWT

### Core Package (core/)
**Purpose**: Shared components used by both admin and company

**Entities**: All database entities (User, Company, Wallet, etc.)
**Repositories**: Data access layer for all entities
**Enums**: UserRole, TransactionType, DepositStatus
**Security**: JWT, authentication, authorization
**Utils**: Reusable utilities (SecurityUtils, EmployeeCodeGenerator)
**Exceptions**: Custom exceptions and global handler
**Services**: Shared services (EmailService, EmployeeCodeService)

## Layer Responsibilities

### Controller Layer
- Handle HTTP requests/responses only
- Validate input using @Valid
- Call service methods
- Return BaseResponse wrapper
- Use @RestController, @RequestMapping
- Apply @PreAuthorize for role-based access
- **Admin controllers**: No companyId filtering
- **Company controllers**: Auto-inject companyId from token

### Service Layer
- **MUST follow Interface + Implementation pattern**
- Interface: `I{Entity}Service` (e.g., IUserService, IAuthService)
- Implementation: `{Entity}ServiceImpl` in `impl/` subfolder
- Implement business logic
- Handle transactions with @Transactional
- Call repository methods (from core/repository)
- **Inject and use Mapper** for DTO ↔ Entity conversion
- Throw custom exceptions for error handling
- Create reusable methods for repeated logic (>2 times)
- **Admin services**: Access all companies
- **Company services**: Filter by companyId from token

### Repository Layer (core/repository)
- Extend JpaRepository or JpaSpecificationExecutor
- Use @Query with deleted=false as FIRST condition
- Create custom queries with proper indexing
- Always check deleted field first in WHERE clause
- Shared by both admin and company services

### Mapper Layer (mapper/)
- **All mappers MUST be @Component**
- Organized by domain: `mapper/core/`, `mapper/admin/`, `mapper/company/`
- Convert between DTO and Entity
- Handle null values properly (always check null)
- **Naming**: `toEntity()`, `toResponse()`, `toDto()`, `updateEntity()`
- **NO business logic** - only data transformation
- Injected into Service layer
- Separate mappers for admin and company DTOs

### Entity Layer (core/entity)
- Extend BaseEntity for common fields
- Define relationships (@OneToOne, @OneToMany, @ManyToMany)
- Add indexes on frequently queried fields
- Use @Table(indexes = {...})
- Shared by all packages

## Naming Conventions

### Admin Package
- Controller: `Admin{Entity}Controller` (e.g., AdminCompanyController)
- Service Interface: `IAdmin{Entity}Service`
- Service Implementation: `Admin{Entity}ServiceImpl` in `service/impl/`
- DTO: `Admin{Entity}Request`, `Admin{Entity}Response`

### Company Package
- Controller: `Company{Entity}Controller` (e.g., CompanyEmployeeController)
- Service Interface: `ICompany{Entity}Service`
- Service Implementation: `Company{Entity}ServiceImpl` in `service/impl/`
- DTO: `Company{Entity}Request`, `Company{Entity}Response`

### Core Package
- Entity: `{Entity}Entity` (e.g., UserEntity, CompanyEntity, WalletEntity)
- Repository: `{Entity}Repository` (e.g., UserRepository)
- Service Interface: `I{Entity}Service` (e.g., IAuthService)
- Service Implementation: `{Entity}ServiceImpl` in `service/impl/`
- Enum: `{Name}` (e.g., UserRole, TransactionType)
- Util: `{Purpose}Utils` or `{Purpose}Generator`
- Exception: `{Type}Exception`

### Mapper Package
- Core Mapper: `{Entity}Mapper` in `mapper/core/` (e.g., UserMapper, CompanyMapper)
- Admin Mapper: `Admin{Entity}Mapper` in `mapper/admin/`
- Company Mapper: `Company{Entity}Mapper` in `mapper/company/`

## Performance Optimization
- Use Pageable for all list APIs
- Add database indexes on foreign keys and frequently queried fields
- Put deleted=false check FIRST in all queries
- Use @Transactional(readOnly = true) for read operations
- Lazy load relationships by default
- Use projection for large result sets
