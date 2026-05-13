@echo off
echo ========================================
echo Creating PostgreSQL Database
echo ========================================
echo.

echo Creating database 'm_koba'...
psql -h localhost -p 5432 -U postgres -c "CREATE DATABASE m_koba;" 2>nul

if %errorlevel% equ 0 (
    echo [SUCCESS] Database 'm_koba' created successfully!
) else (
    echo [INFO] Database might already exist or connection failed.
    echo Trying to connect anyway...
)

echo.
echo Testing connection to 'm_koba'...
psql -h localhost -p 5432 -U postgres -d m_koba -c "SELECT 'Connection successful!' as status;" 2>nul

if %errorlevel% equ 0 (
    echo [SUCCESS] Connected to database 'm_koba'!
    echo.
    echo You can now run: mvn spring-boot:run
) else (
    echo [ERROR] Still cannot connect to database.
    echo.
    echo Please check:
    echo 1. PostgreSQL service is running
    echo 2. Password in application.properties is correct
    echo 3. User 'postgres' has permission to create databases
    echo.
    echo Manual steps:
    echo 1. Open pgAdmin or psql
    echo 2. Run: CREATE DATABASE m_koba;
    echo 3. Test with: psql -h localhost -p 5432 -U postgres -d m_koba
)

echo.
pause
