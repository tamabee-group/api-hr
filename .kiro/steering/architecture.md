# Architecture Rules

## Project Structure

```
src/main/java/com/tamabee/api_hr/
├── controller/
│   ├── admin/          # Tamabee internal management
│   ├── company/        # Company self-management
│   └── core/           # Auth, shared endpoints
├── service/
│   ├── admin/          # Admin business logic
│   ├── company/        # Company business logic
│   └── core/           # Shared services (Auth, Email)
├── mapper/
│   ├── admin/          # Admin-specific mappers
│   ├── company/        # Company-specific mappers
│   └── core/           # Core entity mappers
├── entity/             # JPA entities
├── repository/         # Data access layer
├── dto/
│   ├── request/        # Request DTOs
│   └── response/       # Response DTOs
├── enums/              # Enumerations
├── exception/          # Custom exceptions
├── config/             # Configuration classes
└── util/               # Utility classes
```

## Package Responsibilities

### Admin Package

- Tamabee internal management
- Manage all companies, deposits, plans, wallets
- Access: `ADMIN_TAMABEE`, `MANAGER_TAMABEE`
- No companyId filtering

### Company Package

- Company self-management
- Manage employees, view wallet, create deposit requests
- Access: `ADMIN_COMPANY`, `MANAGER_COMPANY`
- Always filter by companyId from JWT token

### Core Package

- Shared components: entities, repositories, security, utils
- Auth service, Email service
- Used by both admin and company packages

## Layer Responsibilities

### Controller Layer

- Handle HTTP requests/responses only
- Validate input using `@Valid`
- Return `ResponseEntity<BaseResponse<T>>`
- Apply `@PreAuthorize` for role-based access

### Service Layer

- Interface + Implementation pattern
- Business logic and transactions
- Use Mapper for DTO ↔ Entity conversion
- Throw custom exceptions

### Mapper Layer

- `@Component` annotation required
- Methods: `toEntity()`, `toResponse()`, `updateEntity()`
- Always check null at beginning
- NO business logic - only data transformation

### Repository Layer

- Extend `JpaRepository`
- Use Spring Data JPA conventions
- `deleted = false` check FIRST in queries

## User Roles

| Role               | Description                        | CompanyId  |
| ------------------ | ---------------------------------- | ---------- |
| `ADMIN_TAMABEE`    | Full system access                 | 0          |
| `MANAGER_TAMABEE`  | Manage companies, approve deposits | 0          |
| `EMPLOYEE_TAMABEE` | Create deposit requests only       | 0          |
| `ADMIN_COMPANY`    | Full company access                | Company ID |
| `MANAGER_COMPANY`  | Manage employees                   | Company ID |
| `EMPLOYEE_COMPANY` | Basic access                       | Company ID |
