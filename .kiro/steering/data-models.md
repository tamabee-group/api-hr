# Data Models Reference

## Core Entities

### UserEntity

- `employeeCode`: 6-character unique code
- `email`: unique
- `role`: UserRole enum
- `companyId`: 0 for Tamabee users
- `referralCode`: for earning commissions

### CompanyEntity

- `planId`: subscription plan
- `referredByEmployeeCode`: referral tracking
- One-to-one with WalletEntity

### WalletEntity

- `balance`: BigDecimal
- `lastBillingDate`, `nextBillingDate`
- One-to-many with WalletTransactionEntity

### WalletTransactionEntity

- `transactionType`: DEPOSIT, BILLING, REFUND, COMMISSION
- `amount`, `balanceBefore`, `balanceAfter`

### DepositRequestEntity

- `status`: PENDING, APPROVED, REJECTED
- `transferProofUrl`: image URL
- `approvedBy`, `rejectionReason`

### TemporaryPasswordEntity

- For email verification during registration
- `expiredAt`, `used` fields
- Delete after successful account creation

## Enums

```java
// UserRole
ADMIN_TAMABEE, MANAGER_TAMABEE, EMPLOYEE_TAMABEE,
ADMIN_COMPANY, MANAGER_COMPANY, EMPLOYEE_COMPANY

// UserStatus
ACTIVE, INACTIVE, PENDING

// TransactionType
DEPOSIT, BILLING, REFUND, COMMISSION

// DepositStatus
PENDING, APPROVED, REJECTED
```

## BaseEntity Fields

All entities extend BaseEntity:

- `createdAt`, `createdBy`
- `updatedAt`, `updatedBy`
- `deleted` (soft delete)

## Index Strategy

- Index all foreign keys
- Index `deleted` field (CRITICAL)
- Index frequently queried columns
- Composite indexes for common query patterns
