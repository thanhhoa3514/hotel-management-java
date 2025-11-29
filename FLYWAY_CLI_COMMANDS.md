# Flyway CLI Commands - Complete Guide

## 📚 Table of Contents
- [Installation](#installation)
- [Basic Commands](#basic-commands)
- [Migration Commands](#migration-commands)
- [Configuration](#configuration)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## 🚀 Installation

### Via Maven/Gradle (Recommended)
```xml
<!-- Already in pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### Via Command Line Tool
```bash
# Download
wget -qO- https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/9.22.3/flyway-commandline-9.22.3-windows-x64.zip

# Extract
unzip flyway-commandline-9.22.3-windows-x64.zip

# Add to PATH
setx PATH "%PATH%;C:\flyway\flyway-9.22.3"
```

---

## 📋 Basic Commands

### 1. **info** - Check Migration Status
```bash
flyway info

# Output:
+-----------+---------+---------------------+--------+
| Version   | Status  | Description         | Date   |
+-----------+---------+---------------------+--------+
| 1         | Success | Initial schema      | 2024.. |
| 2         | Pending | Add email verified  |        |
+-----------+---------+---------------------+--------+
```

**Usage in Spring Boot:**
```java
Flyway flyway = Flyway.configure()
    .dataSource(url, user, password)
    .load();
    
MigrationInfoService info = flyway.info();
info.pending(); // Get pending migrations
```

### 2. **migrate** - Run Migrations
```bash
# Run all pending migrations
flyway migrate

# With specific target
flyway migrate -target=2.1

# Dry run (no changes)
flyway migrate -dryRunOutput=migration-report.sql
```

**Spring Boot Auto-Migration:**
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
```

### 3. **validate** - Check Migration Integrity
```bash
flyway validate

# Checks:
# - Migration files haven't changed
# - Checksums match
# - No missing migrations
```

### 4. **clean** - Drop Everything ⚠️
```bash
flyway clean

# ⚠️ DANGER: Drops ALL objects
# Use only in DEV!
```

**Production Safety:**
```yaml
spring:
  flyway:
    clean-disabled: true  # ← REQUIRED for production
```

### 5. **baseline** - Mark Existing DB
```bash
# For databases with existing schema
flyway baseline -baselineVersion=1

# Mark as baseline without running V1
flyway baseline -baselineVersion=1 -baselineDescription="Existing schema"
```

### 6. **repair** - Fix Metadata
```bash
# Remove failed migration entries
flyway repair

# Recalculate checksums
flyway repair -repairChecksum
```

---

## 🔧 Migration Commands

### Creating Migrations

**Naming Convention:**
```
V{version}__{description}.sql

Examples:
V1__Initial_schema.sql
V2__Add_email_verified.sql
V2.1__Hotfix_user_table.sql
V3__Add_booking_notes.sql
```

**Versioned Migration:**
```sql
-- V2__Add_email_verified.sql
-- Description: Add email verification flag

ALTER TABLE guest
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;

CREATE INDEX idx_guest_email_verified 
ON guest(email_verified);
```

**Repeatable Migration:**
```sql
-- R__Update_views.sql
-- Runs every time checksum changes

CREATE OR REPLACE VIEW active_bookings AS
SELECT * FROM booking WHERE status = 'ACTIVE';
```

### Migration Types

| Type | Prefix | When Runs | Example |
|------|--------|-----------|---------|
| Versioned | `V` | Once | `V1__Create_table.sql` |
| Undo | `U` | Rollback | `U1__Drop_table.sql` |
| Repeatable | `R` | Checksum change | `R__Update_view.sql` |
| Callback | Java | Lifecycle events | `afterMigrate.sql` |

---

## ⚙️ Configuration

### Via flyway.conf
```properties
# flyway.conf
flyway.url=jdbc:postgresql://localhost:5432/hotelmanagement
flyway.user=hoteluser
flyway.password=hotelpass123
flyway.locations=filesystem:./src/main/resources/db/migration
flyway.baselineOnMigrate=true
flyway.validateOnMigrate=true
flyway.outOfOrder=false
```

### Via application.yml
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 1
    baseline-description: "Initial baseline"
    
    # Locations
    locations: classpath:db/migration
    
    # Validation
    validate-on-migrate: true
    out-of-order: false
    ignore-missing-migrations: false
    
    # Placeholders
    placeholders:
      schema: public
      appName: hotel-management
    
    # Table name
    table: flyway_schema_history
```

### Environment Variables
```bash
# Windows
set FLYWAY_URL=jdbc:postgresql://localhost:5432/hotelmanagement
set FLYWAY_USER=hoteluser
set FLYWAY_PASSWORD=hotelpass123

# Linux/Mac
export FLYWAY_URL=jdbc:postgresql://localhost:5432/hotelmanagement
export FLYWAY_USER=hoteluser
export FLYWAY_PASSWORD=hotelpass123
```

---

## 📝 Advanced Usage

### Placeholders
```sql
-- V3__Create_schema.sql
CREATE SCHEMA ${schema};

CREATE TABLE ${schema}.bookings (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

```yaml
spring:
  flyway:
    placeholders:
      schema: prod_schema
```

### Callbacks
```sql
-- beforeMigrate.sql
SET search_path TO public;

-- afterMigrate.sql
ANALYZE;
VACUUM;
```

### Java-based Migrations
```java
@Component
public class V4__ComplexDataMigration implements JavaMigration {
    
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement statement = context.getConnection().createStatement()) {
            // Complex logic here
            statement.execute("UPDATE users SET role = 'GUEST' WHERE role IS NULL");
        }
    }
}
```

---

## 🎯 Best Practices

### 1. Never Modify Applied Migrations
```bash
❌ BAD: Edit V1__Initial_schema.sql after it ran
✅ GOOD: Create V2__Fix_initial_schema.sql
```

### 2. Use Descriptive Names
```bash
❌ BAD: V2__Update.sql
✅ GOOD: V2__Add_email_verified_column_to_guest.sql
```

### 3. Include Rollback Comments
```sql
-- MIGRATION: Add email_verified column
-- ROLLBACK: ALTER TABLE guest DROP COLUMN email_verified;

ALTER TABLE guest 
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;
```

### 4. Test Migrations Locally First
```bash
# 1. Clean local DB
docker-compose down -v
docker-compose up -d postgres

# 2. Run migration
mvn flyway:migrate

# 3. Check result
flyway info
```

### 5. Use Transactions
```sql
-- Automatic rollback on error
BEGIN;

ALTER TABLE guest ADD COLUMN phone VARCHAR(20);
UPDATE guest SET phone = '000-000-0000' WHERE phone IS NULL;

COMMIT;
```

---

## 🔍 Monitoring & Debugging

### Check Schema History
```sql
SELECT 
    installed_rank,
    version,
    description,
    type,
    script,
    success,
    execution_time,
    installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

### Enable Debug Logging
```yaml
logging:
  level:
    org.flywaydb: DEBUG
```

### Migration Report
```bash
flyway migrate -outputType=json > migration-report.json
```

---

## 🐛 Troubleshooting

### Error: Migration checksum mismatch

**Problem:** Modified an applied migration

**Solution:**
```bash
# Option 1: Repair (updates checksum)
flyway repair

# Option 2: Create new migration
# V3__Fix_previous_migration.sql
```

### Error: Found non-empty schema without metadata table

**Problem:** Existing DB without Flyway history

**Solution:**
```yaml
spring:
  flyway:
    baseline-on-migrate: true  # ← Add this
```

### Error: Migration failed

**Check logs:**
```bash
tail -f logs/hotel-management.log | grep Flyway
```

**Manual fix:**
```sql
-- Mark migration as failed
UPDATE flyway_schema_history 
SET success = false 
WHERE version = '2';

-- Delete failed entry
DELETE FROM flyway_schema_history 
WHERE version = '2' AND success = false;
```

---

## 📊 Common Workflows

### Workflow 1: Fresh Database
```bash
# 1. Start database
docker-compose up -d postgres

# 2. Run application (Flyway auto-migrates)
mvn spring-boot:run

# 3. Verify
flyway info
```

### Workflow 2: Add New Column
```sql
-- V2__Add_guest_address.sql
ALTER TABLE guest
ADD COLUMN address TEXT;

CREATE INDEX idx_guest_address ON guest(address);
```

```bash
# Application restart auto-applies V2
mvn spring-boot:run
```

### Workflow 3: Team Sync
```bash
# 1. Pull latest code (includes new migrations)
git pull

# 2. Start app (Flyway detects and runs new migrations)
mvn spring-boot:run

# 3. Everyone has same schema! ✨
```

---

## 🚀 Production Deployment

### Pre-deployment Checklist
- [ ] Test migrations on staging
- [ ] Backup production database
- [ ] Review `flyway_schema_history`
- [ ] Check disk space
- [ ] Plan rollback strategy

### Deployment Steps
```bash
# 1. Backup
pg_dump -h prod-db -U user dbname > backup-$(date +%F).sql

# 2. Dry run (optional)
flyway migrate -dryRunOutput=prod-migration.sql
# Review prod-migration.sql

# 3. Apply migration
flyway migrate -url=jdbc:postgresql://prod-db:5432/hotelmanagement

# 4. Verify
flyway info
flyway validate
```

### Rollback Plan
```sql
-- If V3 fails, rollback manually
-- U3__Rollback_booking_notes.sql
ALTER TABLE booking DROP COLUMN notes;
```

---

## 📚 Quick Reference

| Command | Purpose | Example |
|---------|---------|---------|
| `migrate` | Apply migrations | `flyway migrate` |
| `info` | Show status | `flyway info` |
| `validate` | Check integrity | `flyway validate` |
| `baseline` | Mark existing DB | `flyway baseline` |
| `repair` | Fix metadata | `flyway repair` |
| `clean` | Drop all | `flyway clean` ⚠️ |

---

## 🔗 Resources

- [Official Docs](https://flywaydb.org/documentation/)
- [Maven Plugin](https://flywaydb.org/documentation/usage/maven/)
- [Spring Boot Integration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)

---

**✨ Flyway CLI Mastery Complete!**
