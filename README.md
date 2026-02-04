# Prym (Project Group 1)

Full-stack app with:
- **Backend:** Spring Boot (User entity + AuthService + AuthController)
- **Frontend:** React + Vite (Buyer/Seller signup + Login + Dashboard)
- **Auth:** Register + Login endpoints + frontend API integration

---

## Table of Contents
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Setup](#setup)
  - [1) Backend Setup (Spring Boot)](#1-backend-setup-spring-boot)
  - [2) Frontend Setup (React + Vite)](#2-frontend-setup-react--vite)
- [Running the Project](#running-the-project)
- [API Endpoints](#api-endpoints)
- [Common Issues](#common-issues)
- [Notes](#notes)

---

## Project Structure

Typical layout (yours may vary slightly):

```
root/
  backend/               # Spring Boot project (Maven/Gradle)
  frontend/              # React + Vite project
  README.md
```

If your repo uses a different folder layout, just run the commands in the correct backend/frontend directories.

---

## Requirements

### Backend
- **Java 17+** (recommended)
- **Maven** or **Gradle** (depending on your Spring Boot setup)
- (Optional) Database:
  - H2 (in-memory) OR MySQL/Postgres (depending on your configuration)

### Frontend
- **Node.js 18+**
- npm (comes with Node)

---

## Setup

### 1) Backend Setup (Spring Boot)

1. Go into the backend folder:
   ```bash
   cd backend
   ```

2. Install dependencies & build:
   - If Maven:
     ```bash
     mvn clean install
     ```
   - If Gradle:
     ```bash
     ./gradlew build
     ```

3. Configure environment (if needed)

Your backend may require values like DB settings or a JWT secret.
Check for:
- `src/main/resources/application.properties`
- `src/main/resources/application.yml`

If you use a `.env` style setup, create one and add your values.

✅ **If you're using H2**, you may not need anything extra.

---

### 2) Frontend Setup (React + Vite)

1. Go into the frontend folder:
   ```bash
   cd ../frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Configure API base URL (important)

Your frontend calls the backend endpoints.  
Look for a file like:
- `src/services/api.js`
- `src/api.js`
- or wherever you created the API helper

Make sure the backend URL matches where Spring Boot runs (commonly `http://localhost:8080`).

Example:
```js
const BASE_URL = "http://localhost:8080";
```

---

## Running the Project

### Step 1 — Start the Backend

From `backend/`:

- Maven:
  ```bash
  mvn spring-boot:run
  ```
- Gradle:
  ```bash
  ./gradlew bootRun
  ```

Backend usually runs on:
- `http://localhost:8080`

---

### Step 2 — Start the Frontend

From `frontend/`:

```bash
npm run dev
```

Frontend usually runs on:
- `http://localhost:5173`

Open it in your browser:
- `http://localhost:5173`

---

## API Endpoints

Based on your commits, your backend includes:

### Auth
- `POST /register` — create account
- `POST /login` — login

> Exact route prefixes can vary (example: `/api/auth/register` vs `/register`).
> Check your `AuthController` for the final paths.

---

## Common Issues

### 1) White screen on frontend
Most common causes:
- A page/component file is empty or missing a default export
- A route imports a file that doesn’t export a component

✅ Fix:
- Check the browser console (DevTools → Console)
- Ensure all pages do `export default ComponentName;`

---

### 2) CORS error (frontend can’t call backend)
If frontend calls backend and you see CORS errors:
- Add CORS configuration in Spring Boot (controller annotations or a global config)
- Ensure your backend allows requests from `http://localhost:5173`

---

### 3) API calls failing (404 / network error)
Make sure:
- Backend is running on the correct port (often 8080)
- Frontend base URL matches backend URL
- Endpoint paths match your controller mappings

---

### 4) Database errors on backend startup
If Spring Boot fails due to DB config:
- Confirm DB credentials in `application.properties`
- If you want easy mode, switch to H2 (in-memory) for development

---

## Notes
- This repo includes both backend and frontend authentication flow:
  - User entity + repository
  - Auth service + controller
  - Buyer/Seller signup + login pages
  - Simple dashboard for testing navigation
