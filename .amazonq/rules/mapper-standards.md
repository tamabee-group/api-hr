# Mapper Standards

## Mapper Organization

### Package Structure
```
mapper/
├── core/           # Core entity mappers (User, Company, Wallet)
│   ├── UserMapper.java
│   ├── CompanyMapper.java
│   └── WalletMapper.java
├── admin/          # Admin-specific mappers
│   ├── AdminCompanyMapper.java
│   └── AdminUserMapper.java
└── company/        # Company-specific mappers
    ├── CompanySettingMapper.java
    └── CompanyProfileMapper.java
```

## Mapper Rules

### 1. All Mappers MUST be @Component
```java
@Component
public class UserMapper {
    // Mapper methods
}
```

### 2. Naming Conventions

**Method Names:**
- `toEntity(Request request)` - Convert Request DTO to Entity
- `toResponse(Entity entity)` - Convert Entity to Response DTO
- `toDto(Entity entity)` - Convert Entity to generic DTO
- `updateEntity(Entity entity, Request request)` - Update existing entity from request

**Examples:**
```java
@Component
public class UserMapper {
    
    public UserEntity toEntity(RegisterRequest request) { }
    
    public UserResponse toResponse(UserEntity entity) { }
    
    public void updateEntity(UserEntity entity, UpdateUserRequest request) { }
}
```

### 3. Null Safety - MANDATORY
Always check null at the beginning:
```java
public UserResponse toResponse(UserEntity entity) {
    if (entity == null) {
        return null;
    }
    
    // Mapping logic
    UserResponse response = new UserResponse();
    response.setId(entity.getId());
    // ...
    
    return response;
}
```

### 4. NO Business Logic
Mappers should ONLY transform data, NO:
- ❌ Database queries
- ❌ Validation logic
- ❌ Business calculations
- ❌ Password encoding
- ❌ Setting audit fields (createdAt, updatedAt)

**Good - Only mapping:**
```java
public UserEntity toEntity(RegisterRequest request) {
    if (request == null) return null;
    
    UserEntity entity = new UserEntity();
    entity.setEmail(request.getEmail());
    entity.setLocale(request.getLocale());
    
    return entity;
}
```

**Bad - Contains business logic:**
```java
public UserEntity toEntity(RegisterRequest request) {
    UserEntity entity = new UserEntity();
    entity.setEmail(request.getEmail());
    entity.setPassword(passwordEncoder.encode(request.getPassword())); // ❌ Business logic
    entity.setCreatedAt(LocalDateTime.now()); // ❌ Should be in service
    
    return entity;
}
```

### 5. Partial Mapping
Mappers can return partially filled entities. Service will complete them:

```java
// Mapper - Only map request fields
public UserEntity toEntity(RegisterRequest request) {
    UserEntity entity = new UserEntity();
    entity.setEmail(request.getEmail());
    entity.setLocale(request.getLocale());
    // Role, Status, CompanyId will be set in Service
    
    return entity;
}

// Service - Complete the entity
public void register(RegisterRequest request) {
    UserEntity user = userMapper.toEntity(request);
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(UserRole.ADMIN_COMPANY);
    user.setStatus(UserStatus.ACTIVE);
    
    userRepository.save(user);
}
```

## Usage in Service

### Inject Mapper
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;  // Inject mapper
    
    @Override
    public UserResponse getUser(Long id) {
        UserEntity entity = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return userMapper.toResponse(entity);  // Use mapper
    }
    
    @Override
    public UserResponse createUser(CreateUserRequest request) {
        UserEntity entity = userMapper.toEntity(request);
        
        // Service adds business logic
        entity.setPassword(passwordEncoder.encode(request.getPassword()));
        entity.setRole(UserRole.EMPLOYEE_COMPANY);
        entity.setStatus(UserStatus.ACTIVE);
        
        UserEntity saved = userRepository.save(entity);
        
        return userMapper.toResponse(saved);
    }
}
```

## Complete Mapper Example

```java
package com.tamabee.api_hr.mapper.core;

import com.tamabee.api_hr.dto.request.CreateUserRequest;
import com.tamabee.api_hr.dto.request.UpdateUserRequest;
import com.tamabee.api_hr.dto.response.UserResponse;
import com.tamabee.api_hr.entity.user.UserEntity;
import com.tamabee.api_hr.model.request.RegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    /**
     * Convert RegisterRequest to UserEntity (partial)
     */
    public UserEntity toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        
        UserEntity entity = new UserEntity();
        entity.setEmail(request.getEmail());
        entity.setLocale(request.getLocale());
        entity.setLanguage(request.getLanguage());
        
        return entity;
    }
    
    /**
     * Convert CreateUserRequest to UserEntity
     */
    public UserEntity toEntity(CreateUserRequest request) {
        if (request == null) {
            return null;
        }
        
        UserEntity entity = new UserEntity();
        entity.setEmail(request.getEmail());
        entity.setName(request.getName());
        entity.setPhone(request.getPhone());
        
        return entity;
    }
    
    /**
     * Convert UserEntity to UserResponse
     */
    public UserResponse toResponse(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        
        UserResponse response = new UserResponse();
        response.setId(entity.getId());
        response.setEmail(entity.getEmail());
        response.setName(entity.getName());
        response.setRole(entity.getRole());
        response.setStatus(entity.getStatus());
        response.setLocale(entity.getLocale());
        response.setLanguage(entity.getLanguage());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        
        return response;
    }
    
    /**
     * Update existing entity from request
     */
    public void updateEntity(UserEntity entity, UpdateUserRequest request) {
        if (entity == null || request == null) {
            return;
        }
        
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getPhone() != null) {
            entity.setPhone(request.getPhone());
        }
        if (request.getLocale() != null) {
            entity.setLocale(request.getLocale());
        }
        if (request.getLanguage() != null) {
            entity.setLanguage(request.getLanguage());
        }
    }
}
```

## Benefits

1. ✅ **Separation of Concerns**: Service focuses on business logic, Mapper on data transformation
2. ✅ **Reusability**: Same mapper used across multiple services
3. ✅ **Testability**: Easy to mock mappers in unit tests
4. ✅ **Maintainability**: Changes to mapping don't affect service logic
5. ✅ **Consistency**: All mapping follows same pattern
6. ✅ **Clean Code**: Services are cleaner and more readable
