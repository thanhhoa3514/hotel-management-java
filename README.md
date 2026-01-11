# Hotel Management System

A full-stack hotel management system built with **Spring Boot 3.5** and **Java 21**, featuring secure authentication, payment processing, and real-time room management.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Database Migrations](#database-migrations)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## Features

- **Authentication & Authorization** - JWT-based authentication with Keycloak integration
- **Room Management** - CRUD operations for rooms with image upload support
- **Reservation System** - Complete booking workflow with check-in/check-out
- **Payment Processing** - Stripe integration for secure payments
- **Email Notifications** - OTP verification and booking confirmations
- **Caching** - Redis-based caching for improved performance
- **Database Migrations** - Flyway for version-controlled schema changes

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming Language |
| Spring Boot | 3.5.7 | Application Framework |
| Spring Security | - | Authentication & Authorization |
| Spring Data JPA | - | Database ORM |
| Hibernate | - | JPA Implementation |

### Database & Caching
| Technology | Version | Purpose |
|------------|---------|---------|
| PostgreSQL | 15 | Primary Database |
| Redis | 7 | Caching & Session Storage |
| Flyway | - | Database Migration |

### Authentication & Payments
| Technology | Version | Purpose |
|------------|---------|---------|
| Keycloak | 23.0.0 | Identity & Access Management |
| JWT (jjwt) | 0.12.3 | Token-based Authentication |
| Stripe | - | Payment Processing |

### DevOps & Tools
| Technology | Purpose |
|------------|---------|
| Docker & Docker Compose | Containerization |
| Maven | Build Tool |
| Lombok | Boilerplate Reduction |
| Apache Commons Lang3 | Utilities |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Clients                               │
│              (Web Browser / Mobile App)                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot API                           │
│                   (Port 8080)                               │
├─────────────────────────────────────────────────────────────┤
│  Controllers → Services → Repositories → Entities          │
└───────┬──────────────┬──────────────┬──────────────────────┘
        │              │              │
        ▼              ▼              ▼
┌───────────────┐ ┌──────────┐ ┌─────────────┐
│  PostgreSQL   │ │  Redis   │ │  Keycloak   │
│  (Port 5432)  │ │ (6379)   │ │ (Port 8180) │
└───────────────┘ └──────────┘ └─────────────┘
```

---

## Prerequisites

Ensure you have the following installed:

- **Java 21** - [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop/)
- **Git** - [Download](https://git-scm.com/downloads)

---

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/thanhhoa3514/hotel-management-v2.git
cd hotel-management-v2/quanlikhachsan
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL, Redis, and Keycloak
docker-compose up -d

# Verify all services are healthy
docker-compose ps
```

**Service Health Check:**
| Service | URL | Credentials |
|---------|-----|-------------|
| PostgreSQL | `localhost:5432` | `hoteluser` / `hotelpass123` |
| Redis | `localhost:6379` | Password: `redis123` |
| Keycloak | `http://localhost:8180` | `admin` / `admin123` |

### 3. Build & Run the Application

```bash
# Clean and build (skip tests for faster build)
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run
```

The API will be available at: `http://localhost:8080`

### 4. Verify Installation

```bash
# Test the API
curl http://localhost:8080/api/v1/rooms
```

---

## Configuration

### Environment Variables

Create a `.env` file or set the following environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/hotelmanagement` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `hoteluser` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `hotelpass123` | Database password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | `redis123` | Redis password |
| `KEYCLOAK_AUTH_SERVER_URL` | `http://localhost:8180` | Keycloak server URL |
| `JWT_SECRET` | Auto-generated | JWT signing secret |
| `JWT_EXPIRATION` | `86400000` | Token expiration (24 hours) |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP server |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` | - | Email username |
| `MAIL_PASSWORD` | - | Email password |
| `STRIPE_SECRET_KEY` | - | Stripe API secret key |
| `STRIPE_WEBHOOK_SECRET` | - | Stripe webhook secret |

### Application Configuration

Main configuration file: `src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/hotelmanagement}
    username: ${SPRING_DATASOURCE_USERNAME:hoteluser}
    password: ${SPRING_DATASOURCE_PASSWORD:hotelpass123}
    
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway manages schema

  flyway:
    enabled: true
    baseline-on-migrate: true
```

---

## API Reference

### Reservation Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/reservations` | Create a reservation |
| `GET` | `/api/v1/reservations/{id}` | Get reservation by ID |
| `GET` | `/api/v1/reservations` | List all reservations |
| `GET` | `/api/v1/reservations/guest/{keycloakUserId}` | Get guest reservations |
| `PUT` | `/api/v1/reservations/{id}` | Update reservation |
| `POST` | `/api/v1/reservations/{id}/check-in` | Check-in |
| `POST` | `/api/v1/reservations/{id}/check-out` | Check-out |
| `POST` | `/api/v1/reservations/{id}/cancel` | Cancel reservation |

### Room Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/rooms` | List all rooms |
| `GET` | `/api/v1/rooms/{id}` | Get room by ID |
| `POST` | `/api/v1/rooms/check-availability` | Check room availability |
| `GET` | `/api/v1/rooms/available?checkIn=&checkOut=` | Get available rooms |

---

## Database Migrations

This project uses **Flyway** for database migrations.

### Migration Files Location
```
src/main/resources/db/migration/
├── V1__Initial_schema.sql
├── V2__Add_capacity_and_size_to_room_types.sql
├── V3__Add_Payment_paymentstatus.sql
└── V4__Add_stripe_session_id_to_payments.sql
```

### Creating New Migrations

1. Create a new file following the naming convention:
   ```
   V{version}__{description}.sql
   ```
   Example: `V5__Add_guest_preferences.sql`

2. Migrations run automatically on application startup

For detailed guidance, see [FLYWAY_GUIDE.md](./FLYWAY_GUIDE.md)

---

## Project Structure

```
src/main/java/com/hotelmanagement/quanlikhachsan/
├── config/           # Configuration classes
├── controller/       # REST API controllers
├── dto/              # Data Transfer Objects
├── exception/        # Custom exceptions & handlers
├── mapper/           # Entity-DTO mappers
├── model/            # JPA Entities
├── repository/       # Spring Data JPA repositories
├── security/         # Security configuration
├── services/         # Business logic
├── util/             # Utility classes
└── QuanlikhachsanApplication.java
```

---

## Contributing

We welcome contributions! Please follow these steps:

### 1. Fork & Clone

```bash
git clone https://github.com/YOUR_USERNAME/hotel-management-v2.git
cd hotel-management-v2
```

### 2. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

### 3. Make Your Changes

- Follow existing code style and conventions
- Add/update tests for new functionality
- Update documentation if needed

### 4. Run Tests

```bash
mvn test
```

### 5. Commit & Push

```bash
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### 6. Create a Pull Request

Open a PR against the `main` branch with a clear description of your changes.

---

## 🔧 Troubleshooting

### Schema Validation Failed

**Error:** Type mismatch between Entity and Database

**Solution:**
```bash
docker-compose down -v
docker-compose up -d
```

### Keycloak Connection Failed

**Error:** Keycloak hasn't finished starting

**Solution:**
```bash
# Wait 30-60 seconds and check health
curl http://localhost:8180/health/ready
```

### Redis Connection Refused

**Solution:**
```bash
docker exec -it hotel_redis redis-cli -a redis123 PING
# If not responding, restart Redis
docker-compose restart redis
```

### UUID vs VARCHAR Error

**Solution:** Reset the database completely:
```bash
docker-compose down -v
docker volume rm quanlikhachsan_postgres_data
docker volume rm quanlikhachsan_redis_data
docker-compose up -d
```

---

## 📖 Additional Documentation

- [SETUP.md](./SETUP.md) - Detailed setup instructions
- [FLYWAY_GUIDE.md](./FLYWAY_GUIDE.md) - Database migration guide
- [KEYCLOAK_MANUAL_SETUP.md](./KEYCLOAK_MANUAL_SETUP.md) - Keycloak configuration
- [IMAGE_UPLOAD.md](./IMAGE_UPLOAD.md) - Image upload functionality
- [REDIS_CACHING_PATTERNS.md](./REDIS_CACHING_PATTERNS.md) - Redis caching strategies
- [MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md) - Migration best practices

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Contact

For questions or issues, please [create an issue](https://github.com/thanhhoa3514/hotel-management-v2/issues) on GitHub.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/thanhhoa3514">thanhhoa3514</a>
</p>
