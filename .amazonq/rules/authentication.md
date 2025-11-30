# Authentication & Authorization Rules

## Authentication Flow

### Registration Process
1. User submits registration with email
2. System generates 6-character unique employee code
3. System creates temporary password and stores in `TemporaryPassword` table
4. Send verification email with temporary password (simple, basic HTML)
5. User verifies email and enters temporary password
6. If correct, create User account and delete temporary password
7. User can login with employee code OR email

### Login Methods
- Login by employee code (6 characters, unique)
- Login by email
- Return JWT token with user info and company context

## User Roles & Permissions

### UserRole Enum
```java
ADMIN_TAMABEE       // Full system access, companyId = 0
MANAGER_TAMABEE     // Manage companies, wallet operations, companyId = 0
EMPLOYEE_TAMABEE    // Limited access, can request wallet verification, companyId = 0
ADMIN_COMPANY       // Full access within company scope
MANAGER_COMPANY     // Manage employees within company
EMPLOYEE_COMPANY    // Basic access within company
```

### Access Control Rules
- **TAMABEE roles**: Access all APIs, companyId = 0
- **COMPANY roles**: Access only within their company scope
- Extract companyId from JWT token for company users
- Always filter by companyId for company-scoped operations
- Use @PreAuthorize annotations on controller methods

## Security Implementation

### JWT Token
- Include: userId, email, employeeCode, role, companyId
- Validate on every request
- Extract companyId from token for filtering

### Password Management
- Temporary passwords stored in separate table
- Delete temporary password after successful account creation
- Temporary password has expiration time

### Employee Code Generation
- 6 characters, alphanumeric
- Must be unique across system
- Create EmployeeCodeService for generation
- Check uniqueness before assigning

## Company Context
- Company users: companyId from token
- Tamabee users: companyId = 0 (no company)
- When company creates user: auto-assign company from token
- When tamabee creates user: companyId = 0
