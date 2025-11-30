# Architecture Rules

## Project Structure
Follow strict layered architecture: Controller → Service → Repository → Entity

Package organization by business domain:
- **admin**: Tamabee internal management (manage companies, deposits, plans)
- **company**: Company self-management (manage employees, view wallet)
- **core**: Shared components (entities, repositories, security, utils)

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
│   │   ├── AdminCompanyService.java
│   │   ├── AdminDepositService.java
│   │   ├── AdminPlanService.java
│   │   └── AdminWalletService.java
│   ├── dto/                        # Admin DTOs
│   │   ├── request/
│   │   └── response/
│   └── mapper/                     # Admin mappers
│
├── company/                        # COMPANY SELF-MANAGEMENT
│   ├── controller/                 # Company REST endpoints
│   │   ├── CompanyEmployeeController.java
│   │   ├── CompanyProfileController.java
│   │   ├── CompanyWalletController.java
│   │   └── CompanyDepositRequestController.java
│   ├── service/                    # Company business logic
│   │   ├── CompanyEmployeeService.java
│   │   ├── CompanyProfileService.java
│   │   ├── CompanyWalletService.java
│   │   └── CompanyDepositRequestService.java
│   ├── dto/                        # Company DTOs
│   │   ├── request/
│   │   └── response/
│   └── mapper/                     # Company mappers
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
        ├── EmailService.java
        ├── EmployeeCodeService.java
        └── AuditService.java
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
- Implement business logic
- Handle transactions with @Transactional
- Call repository methods (from core/repository)
- Use mapper for DTO ↔ Entity conversion
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

### Mapper Layer
- Convert between DTO and Entity
- Use MapStruct or manual mapping
- Handle null values properly
- Map relationships carefully
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
- Service: `Admin{Entity}Service` + `Admin{Entity}ServiceImpl`
- Mapper: `Admin{Entity}Mapper`
- DTO: `Admin{Entity}Request`, `Admin{Entity}Response`

### Company Package
- Controller: `Company{Entity}Controller` (e.g., CompanyEmployeeController)
- Service: `Company{Entity}Service` + `Company{Entity}ServiceImpl`
- Mapper: `Company{Entity}Mapper`
- DTO: `Company{Entity}Request`, `Company{Entity}Response`

### Core Package
- Entity: `{Entity}` (e.g., User, Company, Wallet)
- Repository: `{Entity}Repository` (e.g., UserRepository)
- Enum: `{Name}` (e.g., UserRole, TransactionType)
- Util: `{Purpose}Utils` or `{Purpose}Generator`
- Exception: `{Type}Exception`

## Performance Optimization
- Use Pageable for all list APIs
- Add database indexes on foreign keys and frequently queried fields
- Put deleted=false check FIRST in all queries
- Use @Transactional(readOnly = true) for read operations
- Lazy load relationships by default
- Use projection for large result sets
