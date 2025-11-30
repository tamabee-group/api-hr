# API Standards

## Response Format

### BaseResponse Wrapper
ALL APIs MUST return BaseResponse:

```java
public class BaseResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String errorCode;
}
```

### Success Response
```java
return BaseResponse.success(data, "Operation successful");
```

### Error Response
```java
return BaseResponse.error("Error message", "ERROR_CODE");
```

## Pagination

### ALL List APIs MUST Use Pageable
```java
@GetMapping
public BaseResponse<Page<UserResponse>> getUsers(
    @RequestParam(required = false) String search,
    @RequestParam(required = false) UserRole role,
    Pageable pageable
) {
    // Implementation
}
```

### Pageable Parameters
- page: Page number (0-indexed)
- size: Page size
- sort: Sort field and direction (e.g., "createdAt,desc")

### Filter & Sort
- Support filtering by common fields
- Support sorting by any entity field
- Combine with Pageable for optimal performance

## REST Conventions

### HTTP Methods
- GET: Retrieve data (read-only)
- POST: Create new resource
- PUT: Update entire resource
- PATCH: Partial update
- DELETE: Soft delete (set deleted=true)

### URL Patterns
```
GET    /api/users              - List users (paginated)
GET    /api/users/{id}         - Get user by ID
POST   /api/users              - Create user
PUT    /api/users/{id}         - Update user
DELETE /api/users/{id}         - Soft delete user
GET    /api/users/search       - Search users
```

### Request Validation
- Use @Valid on request bodies
- Validate required fields
- Validate field formats (email, phone, etc.)
- Return clear validation error messages

## Security Headers

### Required Annotations
```java
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('ADMIN_TAMABEE', 'ADMIN_COMPANY')")
public class UserController {
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_TAMABEE') or (hasRole('ADMIN_COMPANY') and @securityService.hasCompanyAccess(#companyId))")
    public BaseResponse<Page<UserResponse>> getUsers(...) {
        // Implementation
    }
}
```

## Error Handling

### Global Exception Handler
- Catch all exceptions globally
- Return consistent error format
- Log errors appropriately
- Don't expose sensitive information

### Custom Exceptions
```java
- ResourceNotFoundException
- UnauthorizedException
- InsufficientBalanceException
- InvalidTemporaryPasswordException
- DuplicateEmployeeCodeException
```

## API Documentation

### Swagger/OpenAPI
- Document all endpoints
- Include request/response examples
- Document error responses
- Group by functional areas

### Endpoint Description
- Clear description of functionality
- List required permissions
- Document query parameters
- Show example requests/responses

## Performance

### Response Time Targets
- Simple queries: < 100ms
- Complex queries: < 500ms
- List with pagination: < 200ms

### Optimization Techniques
- Use database indexes
- Implement caching where appropriate
- Lazy load relationships
- Use projections for large datasets
- Batch operations when possible
