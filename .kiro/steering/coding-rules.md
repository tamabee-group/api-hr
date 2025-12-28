# Backend Coding Rules (Java/Spring Boot)

## Architecture

- Layered architecture: Controller → Service (Interface + Impl) → Mapper → Repository → Entity
- Package by domain: `admin/`, `company/`, `core/`, `mapper/`
- Service MUST follow Interface + Implementation pattern: `I{Entity}Service` + `{Entity}ServiceImpl`
- Mapper MUST be `@Component`, organized by domain

## Exception Handling

- Sử dụng `ErrorCode` enum từ `com.tamabee.api_hr.enums.ErrorCode` cho tất cả error codes
- Không hardcode error code string, luôn dùng enum: `ErrorCode.INVALID_CREDENTIALS` thay vì `"INVALID_CREDENTIALS"`
- Sử dụng các custom exception: `BadRequestException`, `UnauthorizedException`, `ForbiddenException`, `NotFoundException`, `ConflictException`, `InternalServerException`
- Ưu tiên sử dụng static factory methods: `NotFoundException.user(id)`, `ConflictException.emailExists(email)`, `InternalServerException.fileUploadFailed(cause)`

## Response

- Controller trả về `ResponseEntity<BaseResponse<T>>` để kiểm soát HTTP status code
- Sử dụng `BaseResponse.success()`, `BaseResponse.created()`, `BaseResponse.error()` để tạo response
- ALL List APIs MUST use Pageable

## Locale/Timezone

- Sử dụng `LocaleUtil.toTimezone()` để chuyển đổi locale code (vi, ja) sang timezone (Asia/Ho_Chi_Minh, Asia/Tokyo)
- Khi tạo user, locale được lưu dưới dạng timezone format
- Timezone mặc định là `Asia/Tokyo`

## Service Layer

- Sử dụng `@Transactional` cho write operations
- Sử dụng `@Transactional(readOnly = true)` cho read operations
- Inject dependencies qua constructor với `@RequiredArgsConstructor`
- Create reusable methods for logic used >2 times

## Repository

- Sử dụng Spring Data JPA conventions cho method naming
- Ưu tiên `findBy...`, `existsBy...`, `countBy...`
- ALWAYS put `deleted = false` check FIRST in queries
- Add indexes on frequently queried fields

## Naming Conventions

- Classes: PascalCase (UserService, CompanyController)
- Methods: camelCase (getUserById, createCompany)
- Constants: UPPER_SNAKE_CASE (MAX_RETRY_COUNT)
- Variables: camelCase (userId, companyName)
- Entity: `{Entity}Entity` (UserEntity, CompanyEntity)
- Mapper: `{Entity}Mapper` (UserMapper, CompanyMapper)

## Annotations

- Use Lombok: `@Data`, `@Builder`, `@RequiredArgsConstructor`
- Use Spring: `@Service`, `@Repository`, `@RestController`
- Use validation: `@NotNull`, `@Email`, `@Size`, `@Valid`

## Comments

- Viết comment bằng tiếng Việt
- Ghi chú mục đích của method và các tham số quan trọng
- Comment complex business logic, non-obvious algorithms

## Security & Role Management

- Sử dụng `@PreAuthorize` để kiểm tra quyền truy cập cho từng API
- Roles hierarchy: `ADMIN_TAMABEE` > `ADMIN_COMPANY` > `EMPLOYEE`

### API Role Mapping

| Package | API Path          | Allowed Roles                      |
| ------- | ----------------- | ---------------------------------- |
| admin   | `/api/admin/**`   | `ADMIN_TAMABEE`                    |
| company | `/api/company/**` | `ADMIN_COMPANY`, `MANAGER_COMPANY` |
| core    | `/api/auth/**`    | Public (no auth)                   |
| core    | `/api/users/me`   | All authenticated users            |

## Performance

- Use Pageable for all list APIs
- Use `@Transactional(readOnly = true)` for read operations
- Lazy load relationships by default
- Response time targets: Simple < 100ms, Complex < 500ms, List < 200ms

## Database Migration (Flyway)

- Hibernate chỉ map Java objects to tables, KHÔNG tạo indexes/constraints
- Flyway quản lý ALL schema: tables, indexes, foreign keys, enum types
- Migration naming: `V{number}__{description}.sql`
- Luôn dùng `CREATE INDEX IF NOT EXISTS`, `CREATE TYPE ... EXCEPTION WHEN duplicate_object`
- Entity KHÔNG dùng `@ManyToOne`, `@OneToMany` - chỉ dùng Long cho foreign key fields

## Email Service

- Templates location: `src/main/resources/templates/email/{language}/`
- Inline CSS only, max-width 600px
- Placeholders format: `{variableName}`
- Brand color: `#00b1ce`
- Always provide fallback to English template
