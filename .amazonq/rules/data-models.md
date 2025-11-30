# Data Models Reference

## Core Entities

### User Entity
```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_deleted", columnList = "deleted"),
    @Index(name = "idx_company_deleted", columnList = "company_id, deleted"),
    @Index(name = "idx_employee_code", columnList = "employee_code", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true)
})
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 6)
    private String employeeCode; // 6-character unique code
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    
    @Column(nullable = false)
    private Long companyId; // 0 for Tamabee users
    
    private String referralCode; // For earning commissions
    
    // Other fields: name, phone, avatar, etc.
}
```

### Company Entity
```java
@Entity
@Table(name = "companies", indexes = {
    @Index(name = "idx_deleted", columnList = "deleted")
})
public class Company extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;
    
    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL)
    private Wallet wallet;
    
    private String referredByEmployeeCode; // For referral tracking
    
    // Other fields: address, phone, tax code, etc.
}
```

### TemporaryPassword Entity
```java
@Entity
@Table(name = "temporary_passwords", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_expired", columnList = "expired_at")
})
public class TemporaryPassword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiredAt;
    
    @Column(nullable = false)
    private Boolean used = false;
}
```

### Wallet Entity
```java
@Entity
@Table(name = "wallets", indexes = {
    @Index(name = "idx_company", columnList = "company_id", unique = true)
})
public class Wallet extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private LocalDateTime lastBillingDate;
    
    @Column(nullable = false)
    private LocalDateTime nextBillingDate;
}
```

### Plan Entity
```java
@Entity
@Table(name = "plans", indexes = {
    @Index(name = "idx_deleted", columnList = "deleted"),
    @Index(name = "idx_active", columnList = "active")
})
public class Plan extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyPrice;
    
    @Column(nullable = false)
    private Integer maxEmployees;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    // Other fields: features, description, etc.
}
```

### WalletTransaction Entity
```java
@Entity
@Table(name = "wallet_transactions", indexes = {
    @Index(name = "idx_wallet_date", columnList = "wallet_id, created_at"),
    @Index(name = "idx_type", columnList = "transaction_type")
})
public class WalletTransaction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType; // DEPOSIT, BILLING, REFUND
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal balanceBefore;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal balanceAfter;
    
    private String description;
    private String referenceId; // For deposit verification
}
```

### DepositRequest Entity
```java
@Entity
@Table(name = "deposit_requests", indexes = {
    @Index(name = "idx_company_status", columnList = "company_id, status"),
    @Index(name = "idx_status", columnList = "status")
})
public class DepositRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String transferProofUrl; // Image URL
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepositStatus status; // PENDING, APPROVED, REJECTED
    
    private Long approvedBy; // Tamabee admin/manager ID
    private LocalDateTime approvedAt;
    private String rejectionReason;
}
```

### Referral Entity
```java
@Entity
@Table(name = "referrals", indexes = {
    @Index(name = "idx_employee", columnList = "employee_code"),
    @Index(name = "idx_company", columnList = "company_id")
})
public class Referral extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String employeeCode; // Referrer
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // Referred company
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal commissionAmount;
    
    @Column(nullable = false)
    private Boolean paid = false;
    
    private LocalDateTime paidAt;
}
```

## Enums

### UserRole
```java
public enum UserRole {
    ADMIN_TAMABEE,      // Full system access
    MANAGER_TAMABEE,    // Manage companies, approve deposits
    EMPLOYEE_TAMABEE,   // Create deposit requests only
    ADMIN_COMPANY,      // Full company access
    MANAGER_COMPANY,    // Manage employees
    EMPLOYEE_COMPANY    // Basic access
}
```

### TransactionType
```java
public enum TransactionType {
    DEPOSIT,    // Money added to wallet
    BILLING,    // Monthly fee deduction
    REFUND,     // Money returned
    COMMISSION  // Referral commission
}
```

### DepositStatus
```java
public enum DepositStatus {
    PENDING,    // Waiting for approval
    APPROVED,   // Approved and money added
    REJECTED    // Rejected with reason
}
```

## Relationships Summary

- User N:1 Company (many users belong to one company)
- Company 1:1 Wallet (each company has one wallet)
- Company N:1 Plan (many companies can have same plan)
- Wallet 1:N WalletTransaction (wallet has many transactions)
- Company 1:N DepositRequest (company can have many deposit requests)
- Company 1:N Referral (company can be referred by one employee)
