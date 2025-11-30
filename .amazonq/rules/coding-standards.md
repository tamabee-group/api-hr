# Coding Standards

## General Principles

### Code Quality
- Write clean, readable, maintainable code
- Follow DRY principle (Don't Repeat Yourself)
- Create reusable methods for logic used >2 times
- Keep methods small and focused (single responsibility)
- Use meaningful variable and method names

### Performance First
- Optimize database queries
- Use appropriate indexes
- Minimize N+1 query problems
- Use lazy loading by default
- Cache when beneficial

### Scalability
- Design for horizontal scaling
- Stateless services
- Database connection pooling
- Async processing for heavy operations
- Queue-based processing for batch jobs

## Java Conventions

### Naming
- Classes: PascalCase (UserService, CompanyController)
- Methods: camelCase (getUserById, createCompany)
- Constants: UPPER_SNAKE_CASE (MAX_RETRY_COUNT)
- Variables: camelCase (userId, companyName)

### Annotations
- Use Lombok to reduce boilerplate (@Data, @Builder, @NoArgsConstructor)
- Use Spring annotations appropriately (@Service, @Repository, @Controller)
- Use validation annotations (@NotNull, @Email, @Size)

### Exception Handling
- Use custom exceptions for business logic errors
- Don't catch generic Exception unless necessary
- Always log exceptions with context
- Return meaningful error messages to client

## Service Layer Best Practices

### Transaction Management
```java
@Transactional // For write operations
public UserResponse createUser(UserRequest request) {
    // Implementation
}

@Transactional(readOnly = true) // For read operations
public UserResponse getUserById(Long id) {
    // Implementation
}
```

### Reusable Methods
Create utility methods in service for repeated logic:
```java
// Good - Reusable
private void validateCompanyAccess(Long companyId) {
    Long tokenCompanyId = SecurityUtils.getCompanyIdFromToken();
    if (!companyId.equals(tokenCompanyId)) {
        throw new UnauthorizedException("Access denied");
    }
}

// Use in multiple methods
public void updateUser(Long userId, UserRequest request) {
    validateCompanyAccess(request.getCompanyId());
    // Update logic
}
```

### Audit Fields
Always set audit fields:
```java
private void setAuditFields(BaseEntity entity, boolean isUpdate) {
    Long currentUserId = SecurityUtils.getCurrentUserId();
    if (!isUpdate) {
        entity.setCreatedAt(LocalDateTime.now());
        entity.setCreatedBy(currentUserId);
    }
    entity.setUpdatedAt(LocalDateTime.now());
    entity.setUpdatedBy(currentUserId);
}
```

## Repository Best Practices

### Query Optimization
```java
// ALWAYS put deleted check FIRST
@Query("SELECT u FROM User u WHERE u.deleted = false AND u.companyId = :companyId AND u.role = :role")
Page<User> findByCompanyAndRole(@Param("companyId") Long companyId, @Param("role") UserRole role, Pageable pageable);

// Use native query for complex operations
@Query(value = "SELECT * FROM users WHERE deleted = false AND company_id = ?1 ORDER BY created_at DESC", nativeQuery = true)
List<User> findRecentUsers(Long companyId, Pageable pageable);
```

### Method Naming
Follow Spring Data JPA conventions:
- findBy{Field}: Find by single field
- findBy{Field}And{Field}: Multiple conditions
- findBy{Field}OrderBy{Field}: With sorting
- existsBy{Field}: Check existence
- countBy{Field}: Count records

## Utility Classes

### Create Utils for Common Operations
```java
// SecurityUtils - Extract info from JWT
public class SecurityUtils {
    public static Long getCurrentUserId() { }
    public static Long getCompanyIdFromToken() { }
    public static UserRole getCurrentUserRole() { }
}

// ValidationUtils - Common validations
public class ValidationUtils {
    public static boolean isValidEmail(String email) { }
    public static boolean isValidEmployeeCode(String code) { }
}

// DateUtils - Date operations
public class DateUtils {
    public static LocalDateTime getNextMonthDate() { }
    public static boolean isExpired(LocalDateTime date) { }
}
```

## Testing

### Unit Tests
- Test business logic in services
- Mock dependencies
- Test edge cases and error scenarios
- Aim for >80% code coverage

### Integration Tests
- Test API endpoints
- Test database operations
- Test security configurations
- Use test database

## Comments

### When to Comment
- Complex business logic
- Non-obvious algorithms
- Important assumptions
- TODO items with context

### When NOT to Comment
- Obvious code
- Self-explanatory method names
- Redundant information

## Code Review Checklist
- [ ] Follows layered architecture
- [ ] Uses BaseResponse wrapper
- [ ] Implements pagination
- [ ] Has proper error handling
- [ ] Includes audit fields
- [ ] Optimized queries (deleted check first)
- [ ] Proper transaction management
- [ ] Security annotations present
- [ ] No code duplication
- [ ] Meaningful names
