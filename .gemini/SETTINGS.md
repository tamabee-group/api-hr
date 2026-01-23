# Backend Rules - API HR (Java/Spring Boot)

## Architecture

### Project Structure
- `controller/` tổ chức theo: `admin/`, `company/`, `core/`
- `service/` có interface `I{Entity}Service` + implementation trong `impl/` subfolder
- `repository/` tổ chức theo domain: `attendance/`, `user/`, `payroll/`...
- `dto/` chia thành: `request/`, `response/`, `common/`, `config/`, `result/`

### Layer Flow
```
Controller → Service (Interface + Impl) → Mapper → Repository → Entity
```

### API Path Access
| Package | API Path | Roles |
|---------|----------|-------|
| admin | `/api/admin/**` | `ADMIN_TAMABEE`, `MANAGER_TAMABEE` |
| company | `/api/company/**` | `ADMIN_COMPANY`, `MANAGER_COMPANY` |
| core | `/api/auth/**` | Public |
| core | `/api/users/me` | All authenticated |

## Coding Rules

### Service Layer
- Interface + Implementation: `I{Entity}Service` + `{Entity}ServiceImpl`
- **ServiceImpl PHẢI nằm trong `impl/` subfolder**
- `@Transactional` cho write, `@Transactional(readOnly = true)` cho read
- Constructor injection với `@RequiredArgsConstructor`

### Repository
- Spring Data JPA conventions: `findBy...`, `existsBy...`, `countBy...`
- **ALWAYS** `deleted = false` check FIRST in queries
- ALL list APIs MUST use `Pageable`

### Response & Exception
- Return `ResponseEntity<BaseResponse<T>>`
- Use `BaseResponse.success()`, `BaseResponse.created()`, `BaseResponse.error()`
- Use `ErrorCode` enum, KHÔNG hardcode error strings
- Custom exceptions: `BadRequestException`, `NotFoundException`, `ConflictException`, `ForbiddenException`
- Static factory methods: `NotFoundException.user(id)`, `ConflictException.emailExists(email)`

### Naming Conventions
- Entity: `{Name}Entity` (UserEntity)
- Service Interface: `I{Domain}Service` (IPayrollService)
- Service Impl: `{Domain}ServiceImpl` trong `impl/`
- Mapper: `{Name}Mapper` với `@Component`
- Mapper methods: `toEntity()`, `toResponse()`, `updateEntity()`
- Request DTO: `{Action}{Domain}Request` (CreateUserRequest)
- Response DTO: `{Domain}Response` (UserResponse)
- Query DTO: `{Domain}Query` (ContractQuery)

### Database (Flyway)
- Hibernate chỉ map, KHÔNG tạo indexes/constraints
- Flyway quản lý ALL schema
- **Dev**: Update trực tiếp V1 (Schema), V2 (Config data), V3 (Test data)

### Maven
- Dùng `.\mvnw` (Windows) hoặc `./mvnw` (Linux/Mac)
- KHÔNG dùng `mvn` trực tiếp

### File Upload
- Khi update entity có file: xóa file cũ nếu có file mới khác
- Khi delete/cancel entity có file: xóa file đi kèm
- Sử dụng `IUploadService.deleteFile(filePath)` để xóa file

## Multi-Tenant Database

### Database Structure
- **Master DB**: `companies`, `wallets`, `plans`, `employee_commissions`, `tamabee_settings`, `deposits`
- **Tenant DB** (per company): `users`, `user_profiles`, `attendance_records`, `payroll_records`...

### Cross-Database Query Pattern
```java
@Service
public class MyServiceImpl {
    private final UserRepository userRepository;  // Tenant DB
    private final JdbcTemplate masterJdbcTemplate;  // Master DB

    public MyServiceImpl(
            UserRepository userRepository,
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate) {
        this.userRepository = userRepository;
        this.masterJdbcTemplate = masterJdbcTemplate;
    }
}
```

### Rules
- **KHÔNG** dùng Repository cho master DB tables trong service cần cross-database query
- Dùng `@Qualifier("masterJdbcTemplate")` để inject JdbcTemplate cho master DB

## Data Models

### Soft Delete Strategy
**Entities CÓ soft delete** (ít data, cần khôi phục):
- User, UserProfile, Company, CompanyProfile, CompanySetting
- Plan, PlanFeature, ShiftTemplate, WorkSchedule, Holiday
- EmployeeSalary, EmployeeAllowance, EmployeeDeduction
- EmploymentContract, DepositRequest, Wallet

**Entities KHÔNG có soft delete** (data lớn):
- AttendanceRecord, BreakRecord, PayrollRecord, PayrollItem
- WalletTransaction, AuditLog, LeaveRequest, LeaveBalance

### Repository Pattern
```java
// CÓ soft delete - luôn filter deleted = false
Optional<UserEntity> findByIdAndDeletedFalse(Long id);

// KHÔNG soft delete - query bình thường
List<AttendanceRecordEntity> findByUserId(Long userId);
```

## Clean Code Principles

### Function Rules
- Max 20 lines/method, ideally 5-10
- One method = one responsibility
- Max 3 parameters, prefer 0-2
- Guard clauses cho early returns

### Naming Rules
| Element | Convention |
|---------|------------|
| Variables | Reveal intent: `userCount` not `n` |
| Methods | Verb + noun: `getUserById()` |
| Booleans | Question form: `isActive`, `hasPermission`, `canEdit` |
| Constants | SCREAMING_SNAKE: `MAX_RETRY_COUNT` |

### Anti-Patterns (KHÔNG làm)
| ❌ Don't | ✅ Do |
|----------|-------|
| Use verbs in REST endpoints (`/getUsers`) | Use nouns (`/users`) |
| Return inconsistent response formats | Use BaseResponse consistently |
| Expose internal errors to clients | Use ErrorCode enum |
| Skip rate limiting | Plan rate limiting |
| SELECT * in production | Select specific columns |
| Ignore N+1 queries | Use JOIN FETCH or @EntityGraph |
| Comment every line | Delete obvious comments |
| Deep nesting | Guard clauses |
| Magic numbers | Named constants |
| God classes/methods | Split by responsibility |

### Before Editing ANY File
- Check: What imports this file? They might break
- Check: What does this file import? Interface changes
- Check: What tests cover this? Tests might fail
- **Edit the file + all dependent files in SAME task**

## API Design Principles

### REST Best Practices
- Resource naming: nouns, plural (`/users`, `/companies`)
- HTTP methods: GET (read), POST (create), PUT/PATCH (update), DELETE (remove)
- Return proper status codes: 200, 201, 400, 401, 403, 404, 409, 500
- Handle errors gracefully với ErrorCode

### Response Format
- ALL responses dùng `BaseResponse<T>`
- Paginated responses dùng Spring `Page<T>`
- Error responses có `errorCode` để frontend translate

### Query Optimization
- Index ALL foreign keys
- Index `deleted` field CHỈ cho entities có soft delete
- Composite indexes for common query patterns
- KHÔNG SELECT * - chọn columns cần thiết
- Tránh N+1 queries với JOIN FETCH

## Comments
- Viết bằng tiếng Việt
- KHÔNG comment "Requirements" hoặc "Validates: Requirements"
