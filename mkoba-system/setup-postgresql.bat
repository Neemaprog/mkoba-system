@echo off
echo ========================================
echo M-Koba PostgreSQL Setup Script
echo ========================================
echo.

echo Checking PostgreSQL installation...
pg_isready -h localhost -p 5432 >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] PostgreSQL is not running or not installed.
    echo Please:
    echo 1. Install PostgreSQL from https://www.postgresql.org/download/windows/
    echo 2. Start PostgreSQL service
    echo 3. Run this script again
    pause
    exit /b 1
)

echo [OK] PostgreSQL is running!
echo.

echo Creating database...
psql -h localhost -p 5432 -U postgres -c "CREATE DATABASE mkoba_db;" 2>nul
if %errorlevel% equ 0 (
    echo [OK] Database 'mkoba_db' created successfully!
) else (
    echo [INFO] Database 'mkoba_db' might already exist or needs manual creation.
)

echo.
echo Testing connection to mkoba_db...
psql -h localhost -p 5432 -U postgres -d mkoba_db -c "SELECT version();" >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Connection to mkoba_db successful!
) else (
    echo [ERROR] Cannot connect to mkoba_db. Please check credentials.
)

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Next steps:
echo 1. Update your password in application.properties
echo 2. Run: mvn spring-boot:run
echo.
echo Application will be available at:
echo - Registration: http://localhost:8081/register
echo - Login: http://localhost:8081/login
echo.
pause
