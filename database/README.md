# Database Setup

PRYM uses PostgreSQL as its persistent database. Follow the steps below to set it up locally.

## Requirements
- PostgreSQL 14 or higher (the TAs are assumed to have this installed)

## Setup Steps

### 1. Install PostgreSQL
Download from https://www.postgresql.org/download/ if not already installed.
Keep the default port (5432) and set a password for the `postgres` user during installation.

### 2. Create the database
Open pgAdmin or psql and run:
```sql
CREATE DATABASE prymdb;
```

### 3. Configure the application
Copy the example properties file:
```
cp backend/src/main/resources/application.properties.example backend/src/main/resources/application.properties
```
Then open `application.properties` and replace `YOUR_PASSWORD_HERE` with your PostgreSQL password.

### 4. Run the backend
```
cd backend
mvnw.cmd spring-boot:run   # Windows
./mvnw spring-boot:run     # Mac/Linux
```
Hibernate will auto-create all tables on first startup. No manual SQL scripts needed.

### 5. Verify
Open pgAdmin and expand:
`prymdb → Schemas → public → Tables`

You should see the following tables:
- users
- buyers
- sellers
- sessions
- items
- cow_types
- cows
- cow_cuts
- buyer_matches
- certifications

## Switching Between Real DB and Stub DB
- **Real DB (PostgreSQL):** use `src/main/resources/application.properties`
- **Stub DB (H2 in-memory):** used automatically during tests via `src/test/resources/application.properties`

To switch the entire app to the stub DB, change one line in `application.properties`:
```
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

## Notes
- Do NOT commit your real `application.properties` — it is in `.gitignore`
- Use `application.properties.example` as the template
