# OpenEdge Progress Dialect for Hibernate ORM

This implementation adds support for OpenEdge Progress database to Hibernate ORM.

## Overview

The `OpenEdgeDialect` class provides Hibernate ORM support for OpenEdge Progress database version 12.0 and above. This dialect includes:

- Progress-specific data type mappings
- Function mappings and SQL syntax adaptations
- Proper handling of Progress-specific limitations
- Support for Progress boolean (`logical`) types
- Appropriate pagination handling with TOP clause

## Features

### Supported Data Types

| SQL Type | Progress Type | Notes |
|----------|---------------|---------|
| BOOLEAN | logical | Progress native boolean type |
| INTEGER | integer | Standard integer |
| BIGINT | int64 | Progress 64-bit integer |
| DECIMAL/NUMERIC | decimal($p,$s) | Configurable precision/scale |
| VARCHAR | character($l) varying | Variable length character |
| CHAR | character($l) | Fixed length character |
| BLOB | blob | Binary large object |
| CLOB | clob | Character large object |
| DATE | date | Date type |
| TIME | time | Time type |
| TIMESTAMP | timestamp | Timestamp type |

### Supported Functions

- Math functions: `abs()`, `sqrt()`, `log()`, `log10()`, `ceiling()`, `floor()`, `round()`
- String functions: `length()`, `lower()`, `upper()`, `substring()`, `trim()`
- Date/time functions: Date/time extraction and manipulation
- Aggregate functions: Standard SQL aggregates
- Window functions: Basic window function support

### Database Limitations Handled

- No native sequence support (uses TABLE generation strategy)
- No row-level locking (`FOR UPDATE` not supported)
- No `LIMIT`/`OFFSET` support (uses `TOP` clause instead)
- No support for row value constructors
- Limited Unicode support
- IN clause limited to 1000 items

## Configuration

### Using the Dialect

To use the OpenEdge Progress dialect in your Hibernate configuration:

#### hibernate.cfg.xml
```xml
<property name="hibernate.dialect">org.hibernate.dialect.OpenEdgeDialect</property>
```

#### application.properties (Spring Boot)
```properties
spring.jpa.database-platform=org.hibernate.dialect.OpenEdgeDialect
```

#### Java Configuration
```java
properties.put("hibernate.dialect", "org.hibernate.dialect.OpenEdgeDialect");
```

### Connection Properties

For OpenEdge Progress JDBC connections, you typically need:

```properties
jdbc.url=jdbc:datadirect:openedge://host:port;databaseName=database
jdbc.driver=com.ddtek.jdbc.openedge.OpenEdgeDriver
jdbc.username=username
jdbc.password=password
```

## Implementation Details

### Boolean Handling

Progress uses "yes"/"no" string values for boolean representation:
- `true` maps to `"yes"`
- `false` maps to `"no"`

### Pagination

Progress supports `TOP` clause for limiting results but does not support `OFFSET`:
- `SELECT TOP 10 * FROM table` - supported
- `LIMIT` with `OFFSET` - not supported, will throw `UnsupportedOperationException`

### Date/Time Literals

Progress uses specific formats for date/time literals:
- Dates: `'MM/DD/YYYY'` format
- Times: `'HH:MM:SS'` format  
- Timestamps: Combined date/time format

### Primary Key Generation

Since Progress doesn't support:
- Auto-increment columns
- Sequences

The dialect defaults to `GenerationType.TABLE` for primary key generation.

## Compatibility

- **Minimum Progress Version**: 12.0
- **Hibernate Version**: 6.0+
- **Java Version**: 17+

## Known Issues and Limitations

1. **No Offset Support**: Pagination with offset requires application-level implementation
2. **No Row Locking**: Progress doesn't support `FOR UPDATE` or row-level locking
3. **Limited GUID Support**: No native GUID generation
4. **String Length Limits**: Maximum VARCHAR length is 32,767 characters
5. **IN Clause Limits**: Maximum 1000 items in IN clauses
6. **Subquery Restrictions**: Limited subquery support in DML operations

## Testing

The dialect includes comprehensive unit tests covering:
- Data type mappings
- Function support
- Feature limitations
- SQL generation
- Boolean handling
- Pagination behavior

Run tests with:
```bash
./gradlew test --tests "*OpenEdgeDialectTest*"
```

## Examples

### Entity Definition
```java
@Entity
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;
    
    @Column(name = "name", length = 100)
    private String name;
    
    @Column(name = "active")
    private Boolean active; // Maps to Progress logical type
    
    @Column(name = "created_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;
    
    // ... getters/setters
}
```

### Generated SQL
```sql
CREATE TABLE customer (
    id int64 NOT NULL,
    name character(100) varying,
    active logical,
    created_date timestamp,
    PRIMARY KEY (id)
);
```

### Query with Pagination
```java
// This works - uses TOP clause
Query query = em.createQuery("SELECT c FROM Customer c");
query.setMaxResults(10); // Generates: SELECT TOP 10 ...

// This will throw UnsupportedOperationException
query.setFirstResult(20); // OFFSET not supported
query.setMaxResults(10);
```

## Contributing

When contributing to the OpenEdge Progress dialect:

1. Follow existing code patterns in other dialects
2. Add comprehensive tests for new features
3. Update documentation for any behavioral changes
4. Test against actual Progress database instances
5. Consider Progress-specific limitations and constraints

## References

- [OpenEdge Progress Documentation](https://docs.progress.com/category/openedge)
- [Progress SQL Reference](https://docs.progress.com/bundle/openedge-sql-reference)
- [Hibernate ORM Documentation](https://hibernate.org/orm/documentation/)
- [Hibernate Dialect Development Guide](https://hibernate.org/community/contribute/)
