# Prym (Project Group 1)

Full stack webapp with:
- **Backend:** Spring Boot (User entity + AuthService + AuthController)
- **Frontend:** React + Vite (Buyer/Seller signup + Login + Dashboard)
- **Auth:** Register + Login endpoints + frontend API integration

---

## Table of Contents
- [Requirements](#requirements)
- [Setup](#setup)
---


## Requirements

### Backend
- **Java 17+** (recommended)
- **Maven** (Spring Boot setup)
### Frontend
- **Node.js 18+**
- npm (comes with Node)

---

## Setup
For more detailed information, check out our [Wiki](../../wiki):
- [Project Setup](../../wiki/Setup)

Open it in your browser:
- `http://localhost:5173`

---

## Testing

### Backend Tests

From `backend/`:

```bash
./mvnw test -Dtest=LoginTest
```

