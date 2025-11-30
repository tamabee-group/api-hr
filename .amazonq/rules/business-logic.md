# Business Logic Rules

## Company & Subscription Management

### Company Registration
1. Company registers and selects a Plan
2. Optional: Enter employee referral code (employee gets commission)
3. Company gets a Wallet account (balance = 0)
4. Company must deposit money to use service

### Wallet Management

#### Deposit Process
1. Company transfers money to Tamabee bank account
2. Company sends deposit request with transfer proof image via inbox
3. EMPLOYEE_TAMABEE can create verification request
4. ADMIN_TAMABEE or MANAGER_TAMABEE verifies and approves
5. System adds money to company wallet
6. EMPLOYEE_TAMABEE CANNOT directly add money to wallet

#### Roles & Permissions
- **ADMIN_TAMABEE**: Can approve deposits, add money to wallet
- **MANAGER_TAMABEE**: Can approve deposits, add money to wallet  
- **EMPLOYEE_TAMABEE**: Can only create verification requests, NO money operations

### Monthly Billing
1. System deducts monthly fee based on Plan
2. Before deduction, check if balance sufficient for NEXT month
3. If insufficient for next month: Send reminder email to company
4. Email should be simple, basic HTML for fast delivery
5. Track billing history in separate table

### Referral System
- Employees can have referral codes
- When company registers with referral code, employee gets commission
- Track referrals and commissions in database
- Commission calculation based on plan price

## User Management

### Employee Code Generation
- Service: EmployeeCodeService
- Generate unique 6-character alphanumeric code
- Check uniqueness before assigning
- Retry if collision occurs

### User Creation by Company
- Extract companyId from JWT token
- Auto-assign companyId to new user
- Validate user role is company-scoped
- Cannot create Tamabee users

### User Creation by Tamabee
- Set companyId = 0
- Can create any user type
- Can assign to specific company if needed

## Email Service

### Email Requirements
- Simple, basic HTML templates
- Fast delivery prioritized over design
- Templates for:
  - Email verification with temporary password
  - Wallet low balance warning
  - Monthly billing notification
  - Referral commission notification

### Email Content
- Minimal styling
- Plain text alternative
- Clear call-to-action
- No heavy images or complex layouts

## Reusable Service Methods

### Create utility methods for logic repeated >2 times:
- Validate company access (check companyId from token)
- Check user permissions
- Generate unique codes
- Send email notifications
- Calculate commissions
- Check wallet balance
- Audit logging (createdBy, updatedBy)

### Common Validation Methods
- validateCompanyAccess(Long companyId, Long tokenCompanyId)
- validateUserRole(UserRole role, List<UserRole> allowedRoles)
- checkWalletBalance(Long companyId, BigDecimal requiredAmount)
- isTemporaryPasswordValid(String code, String password)

## Performance Considerations
- Cache frequently accessed data (plans, company info)
- Batch process monthly billing
- Async email sending
- Index all foreign keys and query fields
- Use database transactions appropriately
