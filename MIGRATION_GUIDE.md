# Database Migration Guide

Hướng dẫn chạy database migrations cho dự án Hotel Management.

## Tổng quan

Dự án sử dụng **Flyway** để quản lý database migrations. Flyway tự động chạy các migration scripts khi application khởi động.

## Migration Files

Các file migration nằm trong: `src/main/resources/db/migration/`

### V1\_\_Initial_schema.sql

- Tạo tất cả các bảng cơ bản
- Tạo indexes
- Tạo foreign keys
- Insert dữ liệu mẫu (room types, statuses)

### V2\_\_Add_capacity_and_size_to_room_types.sql

- Thêm cột `capacity` (INTEGER) vào bảng `room_types`
- Thêm cột `size` (DECIMAL) vào bảng `room_types`
- Update dữ liệu có sẵn với giá trị mặc định

## Cách chạy Migration

### 1. Tự động (Khuyến nghị)

Migration tự động chạy khi start application:

```bash
cd quanlikhachsan/quanlikhachsan
mvn spring-boot:run
```

Flyway sẽ:

1. Kiểm tra bảng `flyway_schema_history`
2. Chạy các migration chưa được thực thi
3. Ghi lại lịch sử migration

### 2. Chạy riêng Migration

Nếu muốn chạy migration mà không start app:

```bash
mvn flyway:migrate
```

### 3. Kiểm tra trạng thái Migration

```bash
mvn flyway:info
```

Output:

```
+-----------+---------+---------------------+------+---------------------+
| Category  | Version | Description         | Type | Installed On        |
+-----------+---------+---------------------+------+---------------------+
| Versioned | 1       | Initial schema      | SQL  | 2024-11-30 10:00:00 |
| Versioned | 2       | Add capacity and... | SQL  | 2024-11-30 18:00:00 |
+-----------+---------+---------------------+------+---------------------+
```

## Chi tiết V2 Migration

### Thêm Columns

```sql
ALTER TABLE room_types
ADD COLUMN IF NOT EXISTS capacity INTEGER,
ADD COLUMN IF NOT EXISTS size DECIMAL(10,2);
```

### Update Existing Data

```sql
UPDATE room_types
SET capacity = CASE
    WHEN LOWER(name) LIKE '%standard%' THEN 2
    WHEN LOWER(name) LIKE '%deluxe%' THEN 3
    WHEN LOWER(name) LIKE '%suite%' THEN 4
    WHEN LOWER(name) LIKE '%presidential%' THEN 6
    ELSE 2
END,
size = CASE
    WHEN LOWER(name) LIKE '%standard%' THEN 25.00
    WHEN LOWER(name) LIKE '%deluxe%' THEN 35.00
    WHEN LOWER(name) LIKE '%suite%' THEN 50.00
    WHEN LOWER(name) LIKE '%presidential%' THEN 80.00
    ELSE 25.00
END
WHERE capacity IS NULL OR size IS NULL;
```

### Giá trị mặc định:

| Room Type    | Capacity | Size (m²) |
| ------------ | -------- | --------- |
| Standard     | 2        | 25.00     |
| Deluxe       | 3        | 35.00     |
| Suite        | 4        | 50.00     |
| Presidential | 6        | 80.00     |

## Kiểm tra Migration đã chạy

### 1. Kết nối PostgreSQL

```bash
psql -U hoteluser -d hotelmanagement
```

### 2. Kiểm tra bảng flyway_schema_history

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

Expected output:

```
 installed_rank | version | description                    | type | script                                      | checksum    | installed_by | installed_on        | execution_time | success
----------------+---------+--------------------------------+------+---------------------------------------------+-------------+--------------+---------------------+----------------+---------
              1 | 1       | Initial schema                 | SQL  | V1__Initial_schema.sql                      | 1234567890  | hoteluser    | 2024-11-30 10:00:00 |           1234 | t
              2 | 2       | Add capacity and size to...    | SQL  | V2__Add_capacity_and_size_to_room_types.sql | 9876543210  | hoteluser    | 2024-11-30 18:00:00 |            123 | t
```

### 3. Kiểm tra columns mới

```sql
\d room_types
```

Expected output:

```
                                    Table "public.room_types"
     Column      |          Type          | Collation | Nullable |      Default
-----------------+------------------------+-----------+----------+-------------------
 id              | uuid                   |           | not null |
 name            | character varying(100) |           | not null |
 description     | text                   |           |          |
 price_per_night | numeric(10,2)          |           | not null |
 capacity        | integer                |           |          |  ← NEW
 size            | numeric(10,2)          |           |          |  ← NEW
```

### 4. Kiểm tra dữ liệu

```sql
SELECT id, name, capacity, size FROM room_types;
```

Expected output:

```
                  id                  |     name      | capacity | size
--------------------------------------+---------------+----------+-------
 uuid-1                               | Standard      |        2 | 25.00
 uuid-2                               | Deluxe        |        3 | 35.00
 uuid-3                               | Suite         |        4 | 50.00
 uuid-4                               | Presidential  |        6 | 80.00
```

## Rollback Migration

### ⚠️ Cảnh báo

Flyway Community Edition không hỗ trợ rollback tự động. Nếu cần rollback:

### Option 1: Manual Rollback

Tạo file migration mới để revert changes:

```sql
-- V3__Rollback_capacity_and_size.sql
ALTER TABLE room_types
DROP COLUMN IF EXISTS capacity,
DROP COLUMN IF EXISTS size;
```

### Option 2: Database Restore

```bash
# Restore từ backup
pg_restore -U hoteluser -d hotelmanagement backup.dump
```

### Option 3: Clean & Rebuild

```bash
# Drop database
dropdb -U hoteluser hotelmanagement

# Create database
createdb -U hoteluser hotelmanagement

# Restart application (migrations will run from V1)
mvn spring-boot:run
```

## Troubleshooting

### Error: "Migration checksum mismatch"

**Nguyên nhân:** File migration đã được sửa sau khi chạy

**Giải pháp:**

```bash
# Option 1: Repair checksum
mvn flyway:repair

# Option 2: Clean và migrate lại (⚠️ XÓA TẤT CẢ DỮ LIỆU)
mvn flyway:clean
mvn flyway:migrate
```

### Error: "Flyway failed to initialize"

**Nguyên nhân:** Database connection issue

**Giải pháp:**

1. Kiểm tra PostgreSQL đang chạy
2. Kiểm tra credentials trong `application.yml`
3. Kiểm tra database đã tồn tại

```bash
# Check PostgreSQL
docker ps | grep postgres

# Check database exists
psql -U hoteluser -l | grep hotelmanagement
```

### Error: "Column already exists"

**Nguyên nhân:** Migration đã chạy một phần

**Giải pháp:**

```sql
-- Manually complete the migration
ALTER TABLE room_types ADD COLUMN IF NOT EXISTS capacity INTEGER;
ALTER TABLE room_types ADD COLUMN IF NOT EXISTS size DECIMAL(10,2);

-- Update flyway history
UPDATE flyway_schema_history
SET success = true
WHERE version = '2';
```

### Migration không chạy

**Nguyên nhân:** Flyway disabled hoặc file không đúng format

**Kiểm tra:**

1. `application.yml` có `spring.flyway.enabled: true`
2. File migration có format `V{version}__{description}.sql`
3. File nằm trong `src/main/resources/db/migration/`

## Best Practices

### 1. Naming Convention

```
V{version}__{description}.sql
V1__Initial_schema.sql
V2__Add_capacity_and_size_to_room_types.sql
V3__Add_amenities_table.sql
```

### 2. Version Numbers

- Use sequential integers: V1, V2, V3
- Never reuse version numbers
- Never modify executed migrations

### 3. Migration Content

- One logical change per migration
- Use `IF NOT EXISTS` / `IF EXISTS` for safety
- Include rollback instructions in comments
- Test migrations on dev database first

### 4. Data Migrations

- Separate schema changes from data changes
- Use transactions for data updates
- Provide default values for new columns
- Consider performance for large tables

### 5. Testing

```bash
# Test on local database
mvn flyway:migrate

# Verify changes
psql -U hoteluser -d hotelmanagement -c "\d room_types"

# Test application
mvn spring-boot:run
```

## Configuration

### application.yml

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    sql-migration-suffixes: .sql
```

### Flyway Properties

- `baseline-on-migrate: true` - Cho phép migrate database đã có data
- `locations` - Thư mục chứa migration files
- `sql-migration-suffixes` - Extension của migration files

## Next Steps

Sau khi migration chạy thành công:

1. ✅ Verify columns exist: `\d room_types`
2. ✅ Verify data updated: `SELECT * FROM room_types`
3. ✅ Test API: `GET /api/v1/room-types`
4. ✅ Test frontend: Open AddRoomModal and check dropdown

## References

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Flyway](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)
- [PostgreSQL ALTER TABLE](https://www.postgresql.org/docs/current/sql-altertable.html)
