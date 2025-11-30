# Package Structure & Organization

## Overview
The project is organized into 3 main packages based on business domains:

1. **admin/** - Tamabee internal management
2. **company/** - Company self-management  
3. **core/** - Shared components

## Package Details

### 1. Admin Package (admin/)
**Purpose**: Tamabee nội bộ quản lý và hỗ trợ các company

**Responsibilities**:
- Quản lý tất cả companies (CRUD, view, search)
- Xét duyệt deposit requests (approve/reject)
- Quản lý plans và pricing
- Nạp tiền vào wallet của company
- Xem tất cả transactions
- Quản lý referral commissions
- Quản lý Tamabee internal users

**Structure**:
```
admin/
├── controller/
│   ├── AdminCompanyController.java       # Manage all companies
│   ├── AdminDepositController.java       # Approve/reject deposits
│   ├── AdminPlanController.java          # Manage plans
│   ├── AdminWalletController.java        # Add money, view transactions
│   └── AdminUserController.java          # Manage Tamabee users
├── service/
│   ├── AdminCompanyService.java
│   ├── AdminDepositService.java
│   ├── AdminPlanService.java
│   └── AdminWalletService.java
├── dto/
│   ├── request/
│   │   ├── AdminCompanyRequest.java
│   │   ├── AdminDepositApprovalRequest.java
│   │   └── AdminWalletDepositRequest.java
│   └── response/
│       ├── AdminCompanyResponse.java
│       ├── AdminDepositResponse.java
│       └── AdminWalletResponse.java
└── mapper/
    ├── AdminCompanyMapper.java
    └── AdminDepositMapper.java
```

**Access Control**:
- ADMIN_TAMABEE: Full access to all operations
- MANAGER_TAMABEE: Approve deposits, manage wallets
- EMPLOYEE_TAMABEE: Create deposit verification requests only

**API Endpoints**:
```
/api/admin/companies          - Manage companies
/api/admin/deposits           - Approve/reject deposits
/api/admin/plans              - Manage plans
/api/admin/wallets            - Wallet operations
/api/admin/users              - Tamabee users
```

### 2. Company Package (company/)
**Purpose**: Company tự quản lý thông tin và nhân viên của mình

**Responsibilities**:
- Quản lý employees của company (CRUD)
- Cập nhật thông tin company profile
- Xem wallet balance và transaction history
- Tạo deposit requests (gửi bill chuyển khoản)
- Không thể truy cập data của company khác

**Structure**:
```
company/
├── controller/
│   ├── CompanyEmployeeController.java    # Manage employees
│   ├── CompanyProfileController.java     # Update company info
│   ├── CompanyWalletController.java      # View wallet, transactions
│   └── CompanyDepositRequestController.java  # Create deposit requests
├── service/
│   ├── CompanyEmployeeService.java
│   ├── CompanyProfileService.java
│   ├── CompanyWalletService.java
│   └── CompanyDepositRequestService.java
├── dto/
│   ├── request/
│   │   ├── CompanyEmployeeRequest.java
│   │   ├── CompanyProfileRequest.java
│   │   └── CompanyDepositRequest.java
│   └── response/
│       ├── CompanyEmployeeResponse.java
│       ├── CompanyProfileResponse.java
│       └── CompanyWalletResponse.java
└── mapper/
    ├── CompanyEmployeeMapper.java
    └── CompanyProfileMapper.java
```

**Access Control**:
- ADMIN_COMPANY: Full access within company
- MANAGER_COMPANY: Manage employees
- EMPLOYEE_COMPANY: View only
- **CRITICAL**: Always extract companyId from JWT token
- **CRITICAL**: Always validate tokenCompanyId == requestCompanyId

**API Endpoints**:
```
/api/company/employees        - Manage employees
/api/company/profile          - Company profile
/api/company/wallet           - View wallet
/api/company/deposits         - Deposit requests
```

### 3. Core Package (core/)
**Purpose**: Shared components dùng chung cho cả admin và company

**Responsibilities**:
- Database entities và relationships
- Data access repositories
- Security và authentication
- Shared utilities và helpers
- Exception handling
- Email service
- Employee code generation

**Structure**:
```
core/
├── entity/                   # JPA Entities
│   ├── BaseEntity.java
│   ├── User.java
│   ├── Company.java
│   ├── Wallet.java
│   ├── Plan.java
│   ├── WalletTransaction.java
│   ├── DepositRequest.java
│   ├── Referral.java
│   └── TemporaryPassword.java
├── repository/               # Data Access Layer
│   ├── UserRepository.java
│   ├── CompanyRepository.java
│   ├── WalletRepository.java
│   ├── PlanRepository.java
│   ├── WalletTransactionRepository.java
│   ├── DepositRequestRepository.java
│   ├── ReferralRepository.java
│   └── TemporaryPasswordRepository.java
├── enums/                    # Enumerations
│   ├── UserRole.java
│   ├── TransactionType.java
│   └── DepositStatus.java
├── security/                 # Security Components
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── SecurityService.java
│   └── CustomUserDetailsService.java
├── config/                   # Configuration
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   └── WebConfig.java
├── exception/                # Exception Handling
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── UnauthorizedException.java
│   ├── InsufficientBalanceException.java
│   ├── InvalidTemporaryPasswordException.java
│   └── DuplicateEmployeeCodeException.java
├── util/                     # Utilities
│   ├── SecurityUtils.java
│   ├── ValidationUtils.java
│   ├── DateUtils.java
│   └── EmployeeCodeGenerator.java
├── dto/                      # Shared DTOs
│   └── BaseResponse.java
└── service/                  # Shared Services
    ├── EmailService.java
    ├── EmployeeCodeService.java
    └── AuditService.java
```

## Key Principles

### 1. Separation of Concerns
- **Admin**: Tamabee operations, no companyId filtering
- **Company**: Company operations, always filter by companyId
- **Core**: Shared infrastructure

### 2. Data Access
- All repositories in core/repository
- Both admin and company services use same repositories
- Filtering logic in service layer (not repository)

### 3. Security
- Admin: Access all data
- Company: Access only own company data
- CompanyId extracted from JWT token
- Validation in service layer

### 4. Code Reusability
- Entities, repositories, enums in core (shared)
- Utilities and helpers in core/util
- Email service in core/service
- No duplication between admin and company

### 5. Scalability
- Easy to add new admin features
- Easy to add new company features
- Core components remain stable
- Clear boundaries between packages

## Usage Examples

### Admin Service Example
```java
@Service
public class AdminCompanyServiceImpl implements AdminCompanyService {
    
    @Autowired
    private CompanyRepository companyRepository; // From core
    
    @Transactional(readOnly = true)
    public Page<AdminCompanyResponse> getAllCompanies(Pageable pageable) {
        // No companyId filtering - access all companies
        Page<Company> companies = companyRepository
            .findByDeletedFalse(pageable);
        return companies.map(AdminCompanyMapper::toResponse);
    }
}
```

### Company Service Example
```java
@Service
public class CompanyEmployeeServiceImpl implements CompanyEmployeeService {
    
    @Autowired
    private UserRepository userRepository; // From core
    
    @Transactional(readOnly = true)
    public Page<CompanyEmployeeResponse> getEmployees(Pageable pageable) {
        // Extract companyId from JWT token
        Long companyId = SecurityUtils.getCompanyIdFromToken();
        
        // Filter by companyId
        Page<User> employees = userRepository
            .findByDeletedFalseAndCompanyId(companyId, pageable);
        return employees.map(CompanyEmployeeMapper::toResponse);
    }
}
```

## Migration Path
When creating new features:
1. Determine if it's admin or company feature
2. Create controller/service in appropriate package
3. Reuse entities/repositories from core
4. Add utilities to core if reusable
5. Follow naming conventions
