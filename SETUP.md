# Hotel Management System - Setup Guide

## Tong Quan Sua Loi

Da sua cac loi chinh:

1. Docker Compose - them init-db.sql de tao database cho Keycloak
2. Entity RoomType - doi tu Double sang BigDecimal de khop voi DECIMAL(10,2) trong database
3. Migration V1 - viet lai de khop voi entities
4. Bo redis-commander khoi docker-compose

## Yeu Cau He Thong

- Java 21
- Docker & Docker Compose
- Maven
- Node.js 18+ (cho frontend)

## Cac Buoc Chay Du An

### 1. Chay Docker Services

```bash
cd quanlikhachsan/quanlikhachsan

# Xoa data cu (neu can)
docker-compose down -v

# Chay cac services
docker-compose up -d

# Kiem tra logs
docker-compose logs -f

# Doi cho den khi tat ca services healthy
docker-compose ps
```

### 2. Kiem Tra Services

**PostgreSQL:**

```bash
docker exec -it hotel_postgres psql -U hoteluser -d hotelmanagement
# Trong psql:
\dt  # List tables
\q   # Quit
```

**Redis:**

```bash
docker exec -it hotel_redis redis-cli -a redis123
# Trong redis-cli:
PING     # Should return PONG
KEYS *   # List all keys
exit
```

**Keycloak:**

- URL: http://localhost:8180
- Admin: admin / admin123

### 3. Chay Backend Spring Boot

```bash
cd quanlikhachsan/quanlikhachsan

# Clean va build
mvn clean install -DskipTests

# Chay application
mvn spring-boot:run

# Hoac chay tu IDE (IntelliJ IDEA)
```

Backend se chay o: http://localhost:8080

### 4. Chay Frontend React

```bash
cd hotel-aura-dashboard

# Cai dat dependencies
npm install

# Chay dev server
npm run dev
```

Frontend se chay o: http://localhost:5173

## Cau Hinh

### Backend (application.properties/application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hotelmanagement
    username: hoteluser
    password: hotelpass123

  jpa:
    hibernate:
      ddl-auto: validate # Dung Flyway migration
    show-sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true

keycloak:
  auth-server-url: http://localhost:8180
  realm: hotel-management
  resource: hotel-app
```

### Frontend (.env)

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Cau Truc Database

### Lookup Tables

- `room_types` - Loai phong (Standard, Deluxe, Suite, etc.)
- `room_statuses` - Trang thai phong (Available, Occupied, Maintenance)
- `services` - Dich vu khach san

### Main Tables

- `guests` - Thong tin khach hang
- `rooms` - Danh sach phong
- `room_images` - Hinh anh phong
- `reservations` - Dat phong (dung enum cho status)
- `reservation_rooms` - Many-to-many giua reservation va rooms

## API Endpoints

### Reservation

- `POST /api/v1/reservations` - Tao dat phong
- `GET /api/v1/reservations/{id}` - Lay thong tin dat phong
- `GET /api/v1/reservations` - Lay tat ca dat phong
- `GET /api/v1/reservations/guest/{keycloakUserId}` - Dat phong cua guest
- `PUT /api/v1/reservations/{id}` - Cap nhat dat phong
- `POST /api/v1/reservations/{id}/check-in` - Check-in
- `POST /api/v1/reservations/{id}/check-out` - Check-out
- `POST /api/v1/reservations/{id}/cancel` - Huy dat phong

### Room

- `GET /api/v1/rooms` - Lay tat ca phong
- `GET /api/v1/rooms/{id}` - Lay thong tin phong
- `POST /api/v1/rooms/check-availability` - Kiem tra phong kha dung
- `GET /api/v1/rooms/available?checkIn=&checkOut=` - Lay phong kha dung

## Troubleshooting

### Loi: Schema validation failed

**Nguyen nhan:** Type mismatch giua Entity va Database

**Giai phap:**

1. Kiem tra Entity co dung BigDecimal cho DECIMAL columns
2. Kiem tra migration file khop voi Entity
3. Xoa database va chay lai migration:

```bash
docker-compose down -v
docker-compose up -d
```

### Loi: Keycloak connection failed

**Nguyen nhan:** Keycloak chua khoi dong xong

**Giai phap:**

```bash
# Doi them 30s-60s cho Keycloak khoi dong
docker-compose logs keycloak

# Kiem tra health
curl http://localhost:8180/health/ready
```

### Loi: Redis connection refused

**Nguyen nhan:** Sai password hoac Redis chua khoi dong

**Giai phap:**

```bash
# Kiem tra Redis
docker exec -it hotel_redis redis-cli -a redis123 PING

# Neu khong duoc, restart
docker-compose restart redis
```

### Loi: Schema validation - UUID vs VARCHAR

**Loi:**

```
wrong column type encountered in column [id] in table [guests];
found [varchar], but expecting [uuid]
```

**Nguyen nhan:** Database da tao voi schema cu (VARCHAR) nhung Entity dung UUID

**Giai phap - Reset database:**

**Windows:**

```bash
reset-db.bat
```

**Linux/Mac:**

```bash
chmod +x reset-db.sh
./reset-db.sh
```

**Hoac thu cong:**

```bash
docker-compose down -v
docker volume rm quanlikhachsan_postgres_data
docker volume rm quanlikhachsan_redis_data
docker-compose up -d
```

## Testing

### Backend Tests

```bash
mvn test
```

### Frontend Tests

```bash
npm run test
```

## Production Deployment

1. Build backend JAR:

```bash
mvn clean package -DskipTests
```

2. Build frontend:

```bash
npm run build
```

3. Dung Docker images:

```bash
docker build -t hotel-backend:latest .
docker build -t hotel-frontend:latest ./hotel-aura-dashboard
```

## Lien He

Neu gap van de, tao issue tren GitHub hoac lien he qua email.
