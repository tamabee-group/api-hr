# Backend Coding Rules

## Service Layer

- Interface + Implementation: `I{Entity}Service` + `{Entity}ServiceImpl`
- `@Transactional` cho write, `@Transactional(readOnly = true)` cho read
- Constructor injection với `@RequiredArgsConstructor`

## Repository

- Spring Data JPA conventions: `findBy...`, `existsBy...`, `countBy...`
- **ALWAYS** `deleted = false` check FIRST in queries
- ALL list APIs MUST use `Pageable`

## Response & Exception

- Return `ResponseEntity<BaseResponse<T>>`
- Use `BaseResponse.success()`, `BaseResponse.created()`, `BaseResponse.error()`
- Use `ErrorCode` enum, KHÔNG hardcode error strings
- Custom exceptions: `BadRequestException`, `NotFoundException`, `ConflictException`, `ForbiddenException`, `UnauthorizedException`
- Static factory methods: `NotFoundException.user(id)`, `ConflictException.emailExists(email)`

## Security

```java
@RestController
@RequestMapping("/api/admin/companies")
@PreAuthorize("hasRole('ADMIN_TAMABEE')")
public class CompanyController { }
```

## Naming

- Entity: `{Name}Entity` (UserEntity)
- Mapper: `{Name}Mapper` với `@Component`
- Methods: `toEntity()`, `toResponse()`, `updateEntity()`

## Database (Flyway)

- Hibernate chỉ map, KHÔNG tạo indexes/constraints
- Flyway quản lý ALL schema
- **Dev**: Update trực tiếp V1, V2, V3 thay vì tạo file mới
  - V1: Schema (tables, indexes)
  - V2: Config data (plans, features)
  - V3: Test data

## Annotations

```java
@Data @Builder @RequiredArgsConstructor  // Lombok
@Service @Repository @RestController     // Spring
@NotNull @Email @Size @Valid             // Validation
```

## Comments

- Viết bằng tiếng Việt
- KHÔNG comment "Requirements" hoặc "Validates: Requirements"

## Maven

- Dùng `.\mvnw` (Windows) hoặc `./mvnw` (Linux/Mac)
- KHÔNG dùng `mvn` trực tiếp
