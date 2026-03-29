# Database Setup

PRYM uses PostgreSQL as its persistent database. Follow the steps below to set it up locally.

---

## Requirements
- PostgreSQL 14 or higher
- Java 17
- Maven (included via `mvnw` wrapper — no separate install needed)

---

## Step 1 — Install PostgreSQL

Download and install from: https://www.postgresql.org/download/

During installation:
- Keep the default port: **5432**
- Set the password for the `postgres` user to: **`PRYM2026#1`**

> This password is already configured in `backend/src/main/resources/application.properties`.
> If you use a different password, open that file and update `spring.datasource.password`.

---

## Step 2 — Create the Database

After installation, open **SQL Shell (psql)** (search for it in the Start menu on Windows):

1. Press **Enter** four times to accept the default Server, Database, Port, and Username
2. Enter the password: `PRYM2026#1`
3. At the `postgres=#` prompt, run:

```sql
CREATE DATABASE prymdb;
```

4. You should see `CREATE DATABASE`. Type `\q` and press Enter to exit.

Alternatively, in **pgAdmin**:
- Right-click **Databases** → **Create** → **Database**
- Name it `prymdb` → Save

---

## Step 3 — Start the Backend

The backend connects to PostgreSQL and **auto-creates all tables** on first startup (via Hibernate `ddl-auto=update`). No manual SQL scripts are needed.

```bash
# Windows
cd backend
mvnw.cmd spring-boot:run

# Mac / Linux
cd backend
./mvnw spring-boot:run
```

Wait until you see:
```
Started BackendApplication in X.XXX seconds
```

---

## Step 4 — Start the Frontend

Open a second terminal:

```bash
cd uiserver
npm install
npm run dev
```

Open the URL shown (e.g. `http://localhost:5173`) in your browser.

---

## Step 5 — Verify the Database

Open **pgAdmin** and expand:
`prymdb → Schemas → public → Tables`

You should see these tables (auto-created by Hibernate):

| Table | Description |
|---|---|
| `users` | Login accounts for all buyers and sellers |
| `buyers` | Buyer-specific profile data |
| `sellers` | Seller/farm profile data |
| `sessions` | Active login sessions (cookie-based auth) |
| `certifications` | Farm certifications (Halal, Organic, etc.) |
| `cow_types` | Breeds and pricing defined by sellers |
| `cows` | Individual cows listed by sellers |
| `cow_cuts` | The 22 cuts auto-generated per cow |
| `buyer_groups` | Group buying groups created by buyers |
| `buyer_group_members` | Membership + claimed cuts per group |
| `buyer_matches` | Buyer-to-seller matching records |
| `ratings` | Star ratings buyers leave for sellers |
| `rating_codes` | One-time codes sellers generate to allow a buyer to rate them |
| `group_seller_associations` | Lifecycle of group ↔ seller association requests |
| `group_messages` | Chat messages between a buyer group and their associated seller |

---

## Default Test Accounts

The following accounts are created automatically on startup (via `DataInitializer` classes):

### Buyers
| Email | Password |
|---|---|
| `buyer1@test.com` | `buyerPass1` |
| `buyer2@test.com` | `buyerPass2` |

### Sellers
| Email | Password |
|---|---|
| `seller1@test.com` | `sellerPass1` |
| `seller2@test.com` | `sellerPass2` |

---

## Running Tests

### Unit Tests (use H2 in-memory — no PostgreSQL required)
```bash
cd backend
./mvnw test
```

### Integration Tests (use real PostgreSQL — backend must NOT be running)
```bash
cd backend
./mvnw test "-Dspring.profiles.active=integration"
```

Integration tests use `@Transactional` and automatically roll back all changes after each test, so they leave the database clean.

---

## Switching Between Real DB and Stub DB

| Mode | Config file | When used |
|---|---|---|
| Real DB (PostgreSQL) | `backend/src/main/resources/application.properties` | Running the app |
| Stub DB (H2 in-memory) | `backend/src/test/resources/application.properties` | Unit tests |
| Integration DB (PostgreSQL) | `backend/src/test/resources/application-integration.properties` | Integration tests |

To switch the running app to H2 (stub), change **one line** in `application.properties`:
```
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
```
