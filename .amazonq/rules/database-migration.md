# Database Migration Rules

## Hibernate vs Flyway Responsibilities

### Hibernate (JPA Entities)

**Purpose**: Map Java objects to database tables only

**Responsibilities**:

- Define table name with @Table
- Define columns with @Column
- Define basic data types
- Define nullable constraints
- NO indexes (@Index)
- NO foreign keys
- NO unique constraints
- NO relationships (@ManyToOne, @OneToMany)

**Example**:

```java
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // NO @Index annotations
    // NO @ManyToOne, @OneToMany
    // NO unique constraints
}
```

### Flyway Migrations

**Purpose**: Manage ALL database schema, constraints, and optimizations

**Responsibilities**:

- Create ALL tables
- Create enum types
- Add ALL indexes (single and composite)
- Add ALL unique constraints
- Add foreign key constraints
- Alter existing tables
- Data migrations
- Performance optimizations

**Example**:

```sql
-- V1__init_database.sql
-- Create enum types first
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('ADMIN_TAMABEE', 'MANAGER_TAMABEE', 'EMPLOYEE_TAMABEE', 'ADMIN_COMPANY', 'MANAGER_COMPANY', 'EMPLOYEE_COMPANY');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create table with enum type
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role user_role NOT NULL,
    status user_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

-- Create indexes and constraints
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted);

-- V2__create_companies_table.sql
CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    plan_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_email ON companies(email);
CREATE INDEX IF NOT EXISTS idx_companies_plan_id ON companies(plan_id);
CREATE INDEX IF NOT EXISTS idx_companies_deleted ON companies(deleted);

ALTER TABLE companies ADD CONSTRAINT IF NOT EXISTS fk_companies_plan
    FOREIGN KEY (plan_id) REFERENCES plans(id);
```

## Migration Workflow

### Step 1: Create Flyway Migration

```sql
-- V3__create_wallet_transactions_table.sql
DO $$ BEGIN
    CREATE TYPE transaction_type AS ENUM ('DEPOSIT', 'BILLING', 'REFUND', 'COMMISSION');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    transaction_type transaction_type NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_type ON wallet_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_deleted ON wallet_transactions(deleted);

ALTER TABLE wallet_transactions
    ADD CONSTRAINT IF NOT EXISTS fk_wallet_transactions_wallet
    FOREIGN KEY (wallet_id) REFERENCES wallets(id);
```

### Step 2: Create Entity (Hibernate)

```java
@Entity
@Table(name = "wallet_transactions")
public class WalletTransactionEntity extends BaseEntity {

    @Column(nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
}
```

### Step 3: Run Application

- Flyway creates table with all constraints
- Hibernate maps entity to existing table
- Application starts successfully

## Best Practices

### 1. Entity Design

- Keep entities simple
- No @JoinColumn or relationship mappings
- Use Long for foreign key fields
- Use String for enum fields (will be converted by Flyway)

### 2. Migration Naming

- V{number}\_\_{description}.sql
- V1: Init database (enums + core tables)
- V2: Create related tables
- V3: Create feature tables
- Each migration includes table + indexes + constraints

### 3. Index Strategy

- Index all foreign keys
- Index frequently queried columns
- Index columns used in WHERE clauses
- Index columns used in JOIN conditions
- Composite indexes for common query patterns

### 4. Foreign Key Strategy

- Add foreign keys in separate migration
- Use descriptive constraint names: fk*{table}*{referenced_table}
- Consider ON DELETE and ON UPDATE actions

### 5. Enum Types

- Create PostgreSQL enum types in Flyway with DO block
- Use @Enumerated(EnumType.STRING) in entity
- Add all possible enum values upfront
- Use duplicate_object exception handling

## Configuration

### application.yaml

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate # Flyway manages ALL schema
    show-sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    clean-disabled: false
    locations: classpath:db/migration
```

## Common Patterns

### Pattern 1: One-to-Many Relationship

```java
// Parent Entity
@Entity
@Table(name = "companies")
public class CompanyEntity extends BaseEntity {
    @Column(nullable = false)
    private String name;
    // NO @OneToMany mapping
}

// Child Entity
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {
    @Column(nullable = false)
    private Long companyId;  // Just the ID, no @ManyToOne
}
```

```sql
-- Flyway Migration
ALTER TABLE users
    ADD CONSTRAINT fk_users_company
    FOREIGN KEY (company_id) REFERENCES companies(id);

CREATE INDEX idx_users_company_id ON users(company_id);
```

### Pattern 2: Unique Composite Constraint

```java
// Entity - No composite unique
@Entity
@Table(name = "referrals")
public class ReferralEntity extends BaseEntity {
    @Column(nullable = false)
    private String employeeCode;

    @Column(nullable = false)
    private Long companyId;
}
```

```sql
-- Flyway Migration
ALTER TABLE referrals
    ADD CONSTRAINT uk_referrals_employee_company
    UNIQUE (employee_code, company_id);

CREATE INDEX idx_referrals_employee_code ON referrals(employee_code);
CREATE INDEX idx_referrals_company_id ON referrals(company_id);
```

## Troubleshooting

### Issue: Checksum Mismatch

**Solution**: Never modify existing migrations. Create new migration to alter.

### Issue: Foreign Key Violation

**Solution**: Ensure parent table exists before adding foreign key.

### Issue: Enum Type Already Exists

**Solution**: Use `CREATE TYPE IF NOT EXISTS` or check existence first.

### Issue: Index Already Exists

**Solution**: Use `CREATE INDEX IF NOT EXISTS` or drop before recreating.
