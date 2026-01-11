@echo off
REM ======================================================================
REM Hotel Management System - Setup Script (Windows)
REM ======================================================================

setlocal EnableDelayedExpansion

REM Colors (Windows 10+)
set "GREEN=[32m"
set "RED=[31m"
set "YELLOW=[33m"
set "BLUE=[34m"
set "NC=[0m"

REM ======================================================================
REM Main Script
REM ======================================================================

echo.
echo %BLUE%======================================%NC%
echo %BLUE%  Hotel Management System Setup%NC%
echo %BLUE%======================================%NC%
echo.

echo This script will:
echo   1. Check prerequisites
echo   2. Start Docker services (PostgreSQL, Redis, Keycloak)
echo   3. Wait for services to be healthy
echo   4. Build the application
echo   5. Optionally run the application
echo.

REM ----------------------------------------------------------------------
REM Step 1: Check Prerequisites
REM ----------------------------------------------------------------------
echo.
echo %BLUE%======================================%NC%
echo %BLUE%  Step 1: Checking Prerequisites%NC%
echo %BLUE%======================================%NC%
echo.

set MISSING_DEPS=0

REM Check Java
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo %GREEN%[OK]%NC% Java is installed
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VERSION=%%g
    )
    echo      Version: !JAVA_VERSION!
) else (
    echo %RED%[ERROR]%NC% Java is not installed
    set MISSING_DEPS=1
)

REM Check Maven
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo %GREEN%[OK]%NC% Maven is installed
) else (
    echo %RED%[ERROR]%NC% Maven is not installed
    set MISSING_DEPS=1
)

REM Check Docker
where docker >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo %GREEN%[OK]%NC% Docker is installed
) else (
    echo %RED%[ERROR]%NC% Docker is not installed
    set MISSING_DEPS=1
)

REM Check Docker Compose
docker compose version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo %GREEN%[OK]%NC% Docker Compose is installed
) else (
    docker-compose version >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo %GREEN%[OK]%NC% docker-compose is installed
    ) else (
        echo %RED%[ERROR]%NC% Docker Compose is not installed
        set MISSING_DEPS=1
    )
)

REM Check curl
where curl >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo %GREEN%[OK]%NC% curl is installed
) else (
    echo %YELLOW%[WARN]%NC% curl is not installed (optional, for health checks)
)

if !MISSING_DEPS! EQU 1 (
    echo.
    echo %RED%Missing dependencies. Please install them and try again.%NC%
    pause
    exit /b 1
)

REM ----------------------------------------------------------------------
REM Step 2: Start Docker Services
REM ----------------------------------------------------------------------
echo.
echo %BLUE%======================================%NC%
echo %BLUE%  Step 2: Starting Docker Services%NC%
echo %BLUE%======================================%NC%
echo.

REM Check if Docker daemon is running
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo %RED%[ERROR]%NC% Docker daemon is not running. Please start Docker Desktop and try again.
    pause
    exit /b 1
)

REM Stop existing containers
echo Stopping any existing containers...
docker compose down >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    docker-compose down >nul 2>&1
)

REM Ask user if they want to reset data
set /p RESET_DATA="Do you want to reset all data (remove volumes)? [y/N]: "
if /i "!RESET_DATA!"=="y" (
    echo Removing existing volumes...
    docker compose down -v >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        docker-compose down -v >nul 2>&1
    )
    echo %GREEN%[OK]%NC% Volumes removed
)

REM Start services
echo Starting Docker services...
docker compose up -d >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo %GREEN%[OK]%NC% Docker services started
) else (
    docker-compose up -d >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo %GREEN%[OK]%NC% Docker services started
    ) else (
        echo %RED%[ERROR]%NC% Failed to start Docker services
        pause
        exit /b 1
    )
)

REM ----------------------------------------------------------------------
REM Step 3: Wait for Services
REM ----------------------------------------------------------------------
echo.
echo %BLUE%======================================%NC%
echo %BLUE%  Step 3: Waiting for Services%NC%
echo %BLUE%======================================%NC%
echo.

REM Wait for PostgreSQL
echo Checking PostgreSQL...
set POSTGRES_READY=0
for /L %%i in (1,1,30) do (
    if !POSTGRES_READY! EQU 0 (
        docker exec hotel_postgres pg_isready -U hoteluser -d hotelmanagement >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            set POSTGRES_READY=1
            echo %GREEN%[OK]%NC% PostgreSQL is ready!
        ) else (
            echo   Attempt %%i/30 - PostgreSQL not ready...
            timeout /t 2 /nobreak >nul
        )
    )
)
if !POSTGRES_READY! EQU 0 (
    echo %RED%[ERROR]%NC% PostgreSQL failed to start
    pause
    exit /b 1
)

REM Wait for Redis
echo Checking Redis...
set REDIS_READY=0
for /L %%i in (1,1,20) do (
    if !REDIS_READY! EQU 0 (
        docker exec hotel_redis redis-cli -a redis123 ping >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            set REDIS_READY=1
            echo %GREEN%[OK]%NC% Redis is ready!
        ) else (
            echo   Attempt %%i/20 - Redis not ready...
            timeout /t 2 /nobreak >nul
        )
    )
)
if !REDIS_READY! EQU 0 (
    echo %RED%[ERROR]%NC% Redis failed to start
    pause
    exit /b 1
)

REM Wait for Keycloak
echo Checking Keycloak (this may take a minute)...
set KEYCLOAK_READY=0
for /L %%i in (1,1,60) do (
    if !KEYCLOAK_READY! EQU 0 (
        curl -s http://localhost:8180/health/ready >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            set KEYCLOAK_READY=1
            echo %GREEN%[OK]%NC% Keycloak is ready!
        ) else (
            echo   Attempt %%i/60 - Keycloak not ready...
            timeout /t 2 /nobreak >nul
        )
    )
)
if !KEYCLOAK_READY! EQU 0 (
    echo %YELLOW%[WARN]%NC% Keycloak may still be starting. Check manually at http://localhost:8180
)

REM ----------------------------------------------------------------------
REM Step 4: Build Application
REM ----------------------------------------------------------------------
echo.
echo %BLUE%======================================%NC%
echo %BLUE%  Step 4: Building Application%NC%
echo %BLUE%======================================%NC%
echo.

echo Running Maven build...
call mvn clean install -DskipTests -q
if %ERRORLEVEL% EQU 0 (
    echo %GREEN%[OK]%NC% Application built successfully!
) else (
    echo %RED%[ERROR]%NC% Build failed. Check the error messages above.
    pause
    exit /b 1
)

REM ----------------------------------------------------------------------
REM Step 5: Run Application (Optional)
REM ----------------------------------------------------------------------
echo.
echo %BLUE%======================================%NC%
echo %BLUE%  Setup Complete!%NC%
echo %BLUE%======================================%NC%
echo.

echo Service URLs:
echo   - API:      http://localhost:8080
echo   - Keycloak: http://localhost:8180 (admin/admin123)
echo   - Postgres: localhost:5432 (hoteluser/hotelpass123)
echo   - Redis:    localhost:6379 (password: redis123)
echo.

set /p START_APP="Do you want to start the application now? [Y/n]: "
if /i NOT "!START_APP!"=="n" (
    echo.
    echo %BLUE%======================================%NC%
    echo %BLUE%  Starting Application%NC%
    echo %BLUE%======================================%NC%
    echo.
    echo Starting Spring Boot application...
    echo Press Ctrl+C to stop the application
    echo.
    call mvn spring-boot:run
) else (
    echo.
    echo To start the application later, run:
    echo   mvn spring-boot:run
    echo.
    echo Or run directly with Java:
    echo   java -jar target\quanlikhachsan-0.0.1-SNAPSHOT.jar
    echo.
)

echo %GREEN%[OK]%NC% Done!
pause
