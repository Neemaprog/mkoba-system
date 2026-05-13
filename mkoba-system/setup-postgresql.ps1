# M-Koba PostgreSQL Setup Script
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "M-Koba PostgreSQL Setup Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if PostgreSQL is running
Write-Host "Checking PostgreSQL installation..." -ForegroundColor Yellow
try {
    $result = & pg_isready -h localhost -p 5432 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] PostgreSQL is running!" -ForegroundColor Green
    } else {
        throw "PostgreSQL not running"
    }
} catch {
    Write-Host "[ERROR] PostgreSQL is not running or not installed." -ForegroundColor Red
    Write-Host "Please:" -ForegroundColor White
    Write-Host "1. Install PostgreSQL from https://www.postgresql.org/download/windows/" -ForegroundColor White
    Write-Host "2. Start PostgreSQL service" -ForegroundColor White
    Write-Host "3. Run this script again" -ForegroundColor White
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""

# Create database
Write-Host "Creating database..." -ForegroundColor Yellow
try {
    & psql -h localhost -p 5432 -U postgres -c "CREATE DATABASE mkoba_db;" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Database 'mkoba_db' created successfully!" -ForegroundColor Green
    } else {
        Write-Host "[INFO] Database 'mkoba_db' might already exist or needs manual creation." -ForegroundColor Yellow
    }
} catch {
    Write-Host "[WARNING] Database creation failed. You may need to create it manually." -ForegroundColor Yellow
}

Write-Host ""

# Test connection
Write-Host "Testing connection to mkoba_db..." -ForegroundColor Yellow
try {
    & psql -h localhost -p 5432 -U postgres -d mkoba_db -c "SELECT version();" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Connection to mkoba_db successful!" -ForegroundColor Green
    } else {
        throw "Connection failed"
    }
} catch {
    Write-Host "[ERROR] Cannot connect to mkoba_db. Please check credentials." -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor White
Write-Host "1. Update your password in application.properties" -ForegroundColor White
Write-Host "2. Run: mvn spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "Application will be available at:" -ForegroundColor White
Write-Host "- Registration: http://localhost:8081/register" -ForegroundColor Cyan
Write-Host "- Login: http://localhost:8081/login" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"
