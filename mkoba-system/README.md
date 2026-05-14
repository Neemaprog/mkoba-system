# Mkoba System

Mfumo wa Mkoba kwa usimamizi wa vikundi vya akiba na mikopo.

## Mahitaji ya Kuanzisha

- Java 17 au zaidi
- Maven 3.6+
- PostgreSQL 12+

## Kuanzisha Database

1. Sakinisha PostgreSQL.
2. Tengeneza database iitwayo `m_koba`.
3. Endesha script za database:
   - `database-setup.sql`
   - `update-admin-schema.sql`
   - `fix-database.sql`

Au tumia batch files:

- `setup-postgresql.bat` (kwa Windows)
- `setup-postgresql.ps1` (kwa PowerShell)

## Kuendesha Mfumo

1. Nakala repository:

   ```
   git clone <URL ya repo>
   cd mkoba-system
   ```

2. Hakikisha database imeanzishwa.

3. Endesha application:

   ```
   mvn spring-boot:run
   ```

   Au:

   ```
   ./mvnw spring-boot:run
   ```

4. Fungua browser na nenda http://localhost:8080

## Configuration

Angalia `application.properties` kwa mipangilio ya database na AzamPay.

Kwa production, badilisha URLs za callback na redirect.

## Features

- Usajili na login ya watumiaji
- Usimamizi wa vikundi
- Mikopo na akiba
- Malipo kupitia AzamPay
- Ripoti

## Msaada

Kwa maswali, angalia HELP.md au POSTGRESQL-SETUP.md.
