---
name: Backend Coding Rules
description: Backend coding conventions - service layer, repository, response/exception, security, naming, Flyway, file upload
---

# Backend Coding Rules

## Service Layer

- Interface + Implementation: `I{Entity}Service` + `{Entity}ServiceImpl`
- **Tất cả ServiceImpl PHẢI nằm trong `impl/` subfolder**
- `@Transactional` cho write, `@Transactional(readOnly = true)` cho read
- Constructor injection với `@RequiredArgsConstructor`

## Repository

- Spring Data JPA conventions: `findBy...`, `existsBy...`, `countBy...`
- **Repository được tổ chức theo domain** (attendance/, user/, payroll/...)
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
- Service Interface: `I{Domain}Service` (IPayrollService)
- Service Impl: `{Domain}ServiceImpl` (PayrollServiceImpl) - đặt trong `impl/`
- Calculator Interface: `I{Name}Calculator` (IOvertimeCalculator)
- Calculator Impl: `{Name}Calculator` (OvertimeCalculator)
- Mapper: `{Name}Mapper` với `@Component`
- Methods: `toEntity()`, `toResponse()`, `updateEntity()`
- Request DTO: `{Action}{Domain}Request` (CreateUserRequest)
- Response DTO: `{Domain}Response` (UserResponse)
- Query DTO: `{Domain}Query` (ContractQuery)

## Import Paths

```java
// Repository - theo domain
import com.tamabee.api_hr.repository.user.UserRepository;
import com.tamabee.api_hr.repository.attendance.AttendanceRecordRepository;

// DTO
import com.tamabee.api_hr.dto.common.BaseResponse;
import com.tamabee.api_hr.dto.auth.request.LoginRequest;
import com.tamabee.api_hr.dto.auth.response.LoginResponse;

// Datasource (thay vì filter)
import com.tamabee.api_hr.datasource.TenantContext;
import com.tamabee.api_hr.datasource.RegionContext;

// Util
import com.tamabee.api_hr.util.RegionUtil;
```

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

## Timezone & Region

### Khái niệm

- `region` thuộc về **Company** ("vi"/"ja") — quyết định timezone, currency, salary templates
- `language` thuộc về **User** ("vi"/"en"/"ja") — ngôn ngữ giao diện cá nhân
- `[locale]` trong Next.js routing — ngôn ngữ giao diện, KHÔNG liên quan region
- JWT chứa cả `region` (từ company) và `language` (từ user)

### Backend: Lấy timezone-aware LocalDateTime

```java
// Cho các field KHÔNG thuộc BaseEntity (BaseEntity đã tự xử lý qua RegionAwareAuditListener)
LocalDateTime now = LocalDateTime.now(RegionUtil.getTimezone(RegionContext.getCurrentRegion()));

// LUÔN import cả 2:
import com.tamabee.api_hr.util.RegionUtil;
import com.tamabee.api_hr.datasource.RegionContext;
```

### Khi nào cần dùng pattern trên

- `processedAt`, `paidAt`, `lastBillingDate`, `nextBillingDate` — các field timestamp tự quản lý
- `LocalDateTime.now()` trong scheduler, billing, audit log
- **KHÔNG cần** cho `createdAt`/`updatedAt` — BaseEntity + RegionAwareAuditListener đã xử lý

### Validate region

```java
if (!RegionUtil.isValidRegion(request.getRegion())) {
    throw new BadRequestException(ErrorCode.INVALID_REGION);
}
```

### Chỉ ADMIN_TAMABEE được đổi region của company

```java
// Trong CompanyProfileServiceImpl.updateCompanyProfile()
if (request.getRegion() != null && !request.getRegion().equals(company.getRegion())) {
    // Chỉ cho phép nếu caller có role ADMIN_TAMABEE
}
```

## Comments

- Viết bằng tiếng Việt
- KHÔNG comment "Requirements" hoặc "Validates: Requirements"

## Maven

- Dùng `.\mvnw` (Windows) hoặc `./mvnw` (Linux/Mac)
- KHÔNG dùng `mvn` trực tiếp

## File Upload

- Khi update entity có file (avatar, logo, transfer_proof...): xóa file cũ nếu có file mới khác
- Khi delete/cancel entity có file: xóa file đi kèm
- Sử dụng `IUploadService.deleteFile(filePath)` để xóa file

```java
// Xóa file cũ khi update
String oldImageUrl = entity.getImageUrl();
String newImageUrl = request.getImageUrl();
if (oldImageUrl != null && !oldImageUrl.equals(newImageUrl)) {
    uploadService.deleteFile(oldImageUrl);
}

// Xóa file khi delete/cancel
if (entity.getImageUrl() != null) {
    uploadService.deleteFile(entity.getImageUrl());
}
```
