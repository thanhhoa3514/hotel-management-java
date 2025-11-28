# Flyway Database Migration Guide

## 🎯 What is Flyway?

Flyway is a **database migration tool** that:
- ✅ Automatically runs SQL scripts on startup
- ✅ Tracks which migrations have been applied
- ✅ Ensures consistent database schema across environments
- ✅ Version controls your database just like Git for code

---

## 📁 File Structure

```
src/main/resources/
└── db/
    └── migration/
        ├── V1__Initial_schema.sql       ← First migration
        ├── V2__Add_user_table.sql       ← Second migration
        └── V3__Add_email_verified.sql   ← Third migration
```

**Naming Convention:**
- `V` = Versioned migration
- `1` = Version number
- `__` = Double underscore (separator)
- `Initial_schema` = Description
- `.sql` = Extension

---

## 🚀 How It Works

1. **First Run:**
   ```
   Application starts
   → Flyway creates `flyway_schema_history` table
   → Runs V1__Initial_schema.sql
   → Marks V1 as completed
   ```

2. **Add New Migration:**
   ```
   Create V2__Add_booking_notes.sql
   → Application restarts
   → Flyway sees V2 is new
   → Runs V2 only
   → Marks V2 as completed
   ```

3. **Team Sync:**
   ```
   Team member pulls code
   → Gets new migration files
   → Starts application
   → Flyway auto-applies missing migrations
   → Everyone has same schema! ✨
   ```

---

## ⚙️ Configuration

**application.yml:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Don't let Hibernate change schema
  
  flyway:
    enabled: true
    baseline-on-migrate: true  # Handle existing databases
    locations: classpath:db/migration
```

---

## 📝 Creating New Migrations

### Example: Add email verification column

**V2__Add_email_verified_column.sql:**
```sql
-- Add email verified flag to guest table
ALTER TABLE guest
ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;

-- Update existing guests
UPDATE guest
SET email_verified = FALSE;

-- Add index
CREATE INDEX idx_guest_email_verified ON guest(email_verified);
```

**Version numbering:**
- V1 = Initial schema
- V2 = Add column
- V3 = Add table
- V2.1 = Hotfix (use V2_1)

---

## 🔍 Checking Migration Status

**Query flyway_schema_history:**
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

**Output:**
```
installed_rank | version | description      | success | installed_on
1              | 1       | Initial schema   | true    | 2024-11-28 10:00
2              | 2       | Add email column | true    | 2024-11-28 11:00
```

---

## ⚠️ Important Rules

### ✅ DO:
- Create new migration for schema changes
- Use sequential version numbers
- Test migrations on dev first
- Include rollback plan in comments

### ❌ DON'T:
- **NEVER modify existing migration files** (once applied)
- Don't skip version numbers
- Don't use same version number twice
- Don't delete migration files

---

## 🧪 Testing Migrations

### Step 1: Clean Database
```bash
docker-compose down -v  # Remove volumes
docker-compose up -d postgres
```

### Step 2: Run Application
```bash
mvn spring-boot:run
```

### Step 3: Check Logs
```
INFO o.f.core.internal.command.DbValidate : Successfully validated 1 migration
INFO o.f.core.internal.command.DbMigrate  : Migrating schema to version "1 - Initial schema"
INFO o.f.core.internal.command.DbMigrate  : Successfully applied 1 migration
```

---

## 🔄 Common Scenarios

### Scenario 1: Add New Table
**V2__Add_invoice_table.sql:**
```sql
CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    booking_id UUID REFERENCES booking(id),
    amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Scenario 2: Modify Existing Column
**V3__Change_room_price_precision.sql:**
```sql
ALTER TABLE room
ALTER COLUMN price TYPE DECIMAL(12,4);
```

### Scenario 3: Add Sample Data
**V4__Add_test_data.sql:**
```sql
INSERT INTO service (name, price) VALUES
    ('Breakfast', 15.00),
    ('Parking', 10.00);
```

---

## 🐛 Troubleshooting

### Error: "Migration checksum mismatch"
**Cause:** Modified an applied migration
**Fix:**
```sql
-- Nuclear option: reset Flyway (DEV ONLY!)
DELETE FROM flyway_schema_history WHERE version = '2';
```

### Error: "Found non-empty schema without metadata table"
**Cause:** Database has tables but no Flyway history
**Fix:** Already handled by `baseline-on-migrate: true`

---

## 📊 Migration Best Practices

1. **One Change Per Migration:**
   ```
   ❌ V2__Add_many_things.sql
   ✅ V2__Add_user_table.sql
   ✅ V3__Add_role_table.sql
   ```

2. **Include Rollback Info:**
   ```sql
   -- MIGRATION: Add email_verified column
   -- ROLLBACK: ALTER TABLE guest DROP COLUMN email_verified;
   
   ALTER TABLE guest ADD COLUMN email_verified BOOLEAN;
   ```

3. **Test Data Separately:**
   ```
   V1__Schema.sql        ← Schema only
   V2__Test_data.sql     ← Test data (optional)
   ```

---

## 🎓 Summary

**Before Flyway:**
```
😰 Manual SQL imports
😰 Schema conflicts
😰 "Works on my machine"
😰 Production deployment scary
```

**After Flyway:**
```
😊 Automatic migrations
😊 Version controlled schema
😊 Team sync effortless
😊 Confident deployments
```

---

## 🔗 Next Steps

1. ✅ Flyway is now configured
2. ✅ V1 migration created
3. ⏳ Start application to apply migration
4. ⏳ Check `flyway_schema_history` table
5. ⏳ Create V2 when you need schema changes

**Remember:** Once a migration is applied, create a NEW version instead of modifying it!
