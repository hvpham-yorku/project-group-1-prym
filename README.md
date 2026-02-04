# Prym (Project Group 1)

Full stack webapp with:
- **Backend:** Spring Boot (User entity + AuthService + AuthController)
- **Frontend:** React + Vite (Buyer/Seller signup + Login + Dashboard)
- **Auth:** Register + Login endpoints + frontend API integration

---

## Table of Contents
- [Requirements](#requirements)
- [Setup](#setup)
  - [1) Backend Setup (Spring Boot)](#1-backend-setup-spring-boot)
  - [2) Frontend Setup (React + Vite)](#2-frontend-setup-react--vite)
- [Running the Project](#running-the-project)


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
you need two seperate terminals running, one for frontend and one for backend
### 0) Clone the Repository

1. Clone the repo:
   ```bash
   git clone https://github.com/hvpham-yorku/project-group-1-prym.git
   ```

2. Move into the project folder:
   ```bash
   cd project-group-1-prym
   ```
### 1) Backend Setup (Spring Boot)

1. Go into the backend folder:
   ```bash
   cd backend
   ```

2. Install dependencies & build:
     ```bash
     mvn clean install
     ```
### 2) Frontend Setup (React + Vite)

1. Go into the frontend folder:
   ```bash
   cd uiserver
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

---

## Running the Project

### Step 1 — Start the Backend

From `backend/`:

- Maven:
  ```bash
  mvn spring-boot:run
  ```


### Step 2 — Start the Frontend

From `uiserver/`:

```bash
npm run dev
```

Open it in your browser:
- `http://localhost:5173`




