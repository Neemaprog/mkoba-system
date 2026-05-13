@echo off
echo ========================================
echo PostgreSQL Connection Test
echo ========================================
echo.

echo Testing connection to PostgreSQL...
psql -h localhost -p 5432 -U postgres -d m_koba -c "SELECT version();" >nul 2>&1

if %errorlevel% equ 0 (
    echo [SUCCESS] Connected to PostgreSQL database 'm_koba'!
    echo.
    echo Next steps:
    echo 1. Update your password in application.properties
    echo 2. Run: mvn spring-boot:run
) else (
    echo [ERROR] Cannot connect to PostgreSQL database 'm_koba'
    echo.
    echo Please check:
    echo - PostgreSQL service is running
    echo - Database 'm_koba' exists
    echo - Username 'postgres' is correct
    echo - Password is correct
    echo.
    echo To create database manually:
    echo psql -h localhost -p 5432 -U postgres -c "CREATE DATABASE m_koba;"
)

echo.
pause
