# Database Rules

## Base Entity
All entities MUST extend BaseEntity with common fields:

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false, updatable = false)
    private Long createdBy;
    
    private LocalDateTime updatedAt;
    private Long updatedBy;
    
    @Column(nullable = false)
    private Boolean deleted = false;
}
```

## Entity Requirements

### Mandatory Fields
- createdAt: Timestamp when record created
- createdBy: User ID who created
- updatedAt: Timestamp when record updated
- updatedBy: User ID who updated
- deleted: Soft delete flag (default false)

### Indexes
Add indexes on:
- Foreign keys
- Frequently queried fields
- deleted field (CRITICAL for performance)
- companyId field
- Composite indexes for common query combinations

Example:
```java
@Table(name = "users", indexes = {
    @Index(name = "idx_deleted", columnList = "deleted"),
    @Index(name = "idx_company_deleted", columnList = "company_id, deleted"),
    @Index(name = "idx_employee_code", columnList = "employee_code", unique = true),
    @Index(name = "idx_email", columnList = "email", unique = true)
})
```

## Relationships

### One-to-One (1-1)
```java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "related_id")
private RelatedEntity related;
```

### One-to-Many (1-N)
```java
@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ChildEntity> children;
```

### Many-to-Many (N-N)
```java
@ManyToMany
@JoinTable(
    name = "entity_relation",
    joinColumns = @JoinColumn(name = "entity_a_id"),
    inverseJoinColumns = @JoinColumn(name = "entity_b_id")
)
private Set<RelatedEntity> relations;
```

## Query Optimization

### Deleted Check FIRST
ALWAYS put deleted=false as FIRST condition:
```java
@Query("SELECT u FROM User u WHERE u.deleted = false AND u.companyId = :companyId")
```

### Use Indexes
- Create indexes on all foreign keys
- Create composite indexes for common query patterns
- Index on deleted field is MANDATORY

### Pagination
ALL list queries MUST use Pageable:
```java
Page<User> findByDeletedFalseAndCompanyId(Long companyId, Pageable pageable);
```

## Soft Delete
- Never physically delete records
- Set deleted = true
- Always filter deleted = false in queries
- Put deleted check FIRST for query performance
