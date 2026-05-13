# PostgreSQL Setup for M-Koba System

## Prerequisites
1. Install PostgreSQL on your system
2. Make sure PostgreSQL service is running

## Step 1: Install PostgreSQL (if not already installed)

### Windows:
1. Download PostgreSQL from: https://www.postgresql.org/download/windows/
2. Run the installer
3. Remember the password you set for 'postgres' user
4. Note the installation port (usually 5432)

### Linux (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

## Step 2: Create Database and User

### Option A: Using pgAdmin (GUI)
1. Open pgAdmin
2. Connect to your PostgreSQL server
3. Right-click on "Databases" → "Create" → "Database"
4. Name it: `mkoba_db`
5. Click "Save"

### Option B: Using psql (Command Line)
```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE mkoba_db;

# Exit
\q
```

## Step 3: Verify Connection

### Test connection using psql:
```bash
psql -h localhost -p 5432 -U postgres -d mkoba_db
```

### Test connection using Java (optional):
```bash
mvn spring-boot:run
```

## Step 4: Update Application Properties

Make sure your `application.properties` has:
```properties
# PostgreSQL DataSource Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/mkoba_db
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=your_password_here  # Replace with your actual password
```

## Step 5: Run Application

```bash
mvn spring-boot:run
```

The application will automatically create all tables using Hibernate.

## Troubleshooting

### Error: "Connection refused"
- PostgreSQL service is not running
- Wrong port number
- Firewall blocking connection

### Error: "Authentication failed"
- Wrong username/password
- User doesn't have permission to access database

### Error: "Database doesn't exist"
- Database `mkoba_db` not created
- Wrong database name in connection URL

## Manual Table Creation (Optional)

If you want to create tables manually, run:
```bash
psql -h localhost -p 5432 -U postgres -d mkoba_db -f database-setup.sql
```

## Default PostgreSQL Credentials
- Username: postgres
- Password: (what you set during installation)
- Port: 5432
- Host: localhost

## Application URLs After Setup
- Registration: http://localhost:8081/register
- Login: http://localhost:8081/login
- Dashboard: http://localhost:8081/dashboard
