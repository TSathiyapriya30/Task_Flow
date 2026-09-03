# TaskFlow

A full-stack task management application built as a learning project. It pairs a
plain **HTML / CSS / vanilla JavaScript** frontend with a **Spring Boot 3 (Java 21)**
REST API backed by **PostgreSQL**, secured with **JWT authentication** and **BCrypt**
password hashing.

> This project is for local development and learning. It is intentionally **not**
> deployed anywhere — you will handle deployment yourself, separately.

---

## 1. Project Overview

TaskFlow lets a user register, log in, and manage a private list of tasks:
create, view, edit, delete, change status, search, filter, and sort. Every task
belongs to exactly one user, and the backend enforces that a user can only ever
see or modify their own tasks — this is checked on the server, never trusted from
the frontend.

## 2. Features

- Register / Login / Logout with JWT-based sessions
- View your profile (`/api/auth/me`)
- Create, view, edit, delete tasks
- Change task status (`PENDING`, `IN_PROGRESS`, `COMPLETED`)
- Priority levels (`LOW`, `MEDIUM`, `HIGH`) with due dates
- Search tasks by title/description
- Filter by status and priority
- Sort by title, due date, priority, status, or created date
- Dashboard with live task counts (total / pending / in progress / completed)
- Ownership enforced server-side — users cannot access each other's tasks
- Consistent JSON error responses, with field-level validation errors
- Backend test suite (unit + integration/MockMvc) covering auth, CRUD, and
  authorization

## 3. Technology Stack

**Frontend:** HTML5, CSS3, vanilla JavaScript (`fetch()` API). No frameworks.

**Backend:** Java 21, Spring Boot 3, Spring Web, Spring Data JPA (Hibernate),
Spring Security, JWT (jjwt), Bean Validation, Maven.

**Database:** PostgreSQL, accessed through Spring Data JPA / Hibernate.

## 4. Architecture

```
┌─────────────────────────────┐
│       Vanilla Frontend      │
│      HTML + CSS + JS        │
└──────────────┬──────────────┘
               │ HTTP/JSON (fetch)
               ▼
┌─────────────────────────────┐
│       Spring Boot API       │
│  Controller → Service        │
│      → Repository            │
│      → JPA / Hibernate       │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│         PostgreSQL          │
└─────────────────────────────┘
```

Every incoming request to a protected endpoint passes through a
`JwtAuthenticationFilter`, which validates the `Authorization: Bearer <token>`
header and populates Spring Security's context with the authenticated user
before the request ever reaches a controller.

## 5. Folder Structure

```
taskflow/
│
├── backend/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/taskflow/
│   │   │   │   ├── TaskFlowApplication.java
│   │   │   │   ├── config/          # SecurityConfig (also wires CORS)
│   │   │   │   ├── controller/      # AuthController, TaskController
│   │   │   │   ├── service/         # AuthService, TaskService
│   │   │   │   ├── repository/      # UserRepository, TaskRepository
│   │   │   │   ├── entity/          # User, Task (JPA entities)
│   │   │   │   ├── dto/             # Request/response DTOs
│   │   │   │   ├── security/        # JwtService, JwtAuthenticationFilter, UserDetailsService
│   │   │   │   ├── exception/       # Custom exceptions + GlobalExceptionHandler
│   │   │   │   └── enums/           # TaskStatus, TaskPriority
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── application-dev.properties
│   │   └── test/
│   │       ├── java/com/taskflow/   # Unit + MockMvc integration tests
│   │       └── resources/
│   │           └── application-test.properties   # H2 in-memory DB for tests
│
├── frontend/
│   ├── index.html            # Landing page
│   ├── login.html
│   ├── register.html
│   ├── dashboard.html
│   ├── create-task.html
│   ├── edit-task.html
│   ├── task-details.html
│   ├── 404.html
│   ├── css/style.css
│   └── js/
│       ├── api.js            # fetch()-based API client
│       ├── auth.js           # token/user storage + route guarding
│       ├── login.js
│       ├── register.js
│       ├── dashboard.js
│       ├── task.js           # create/edit/details page logic
│       └── utils.js          # formatting/escaping helpers
│
├── .gitignore
├── .env.example
└── README.md
```

## 6. Database Design

**users**

| Column      | Type         | Constraints              |
|-------------|--------------|---------------------------|
| id          | BIGINT       | PK, auto-increment        |
| name        | VARCHAR(100) | NOT NULL                  |
| email       | VARCHAR(150) | NOT NULL, UNIQUE, indexed |
| password    | VARCHAR      | NOT NULL (BCrypt hash)    |
| created_at  | TIMESTAMP    | NOT NULL                  |
| updated_at  | TIMESTAMP    | NOT NULL                  |

**tasks**

| Column       | Type          | Constraints                         |
|--------------|---------------|---------------------------------------|
| id           | BIGINT        | PK, auto-increment                    |
| title        | VARCHAR(150)  | NOT NULL                              |
| description  | VARCHAR(2000) |                                        |
| status       | VARCHAR(20)   | NOT NULL, enum: PENDING/IN_PROGRESS/COMPLETED, indexed |
| priority     | VARCHAR(20)   | NOT NULL, enum: LOW/MEDIUM/HIGH, indexed |
| due_date     | DATE          |                                        |
| created_at   | TIMESTAMP     | NOT NULL                              |
| updated_at   | TIMESTAMP     | NOT NULL                              |
| user_id      | BIGINT        | FK → users.id, NOT NULL, indexed      |

Relationship: **User (1) ─── (*) Task**, implemented with `@OneToMany` on `User`
and `@ManyToOne` on `Task`, using `user_id` as the foreign key.

## 7. API Documentation

All endpoints are prefixed with `/api`. Protected endpoints require
`Authorization: Bearer <jwt>`.

### Auth

| Method | Endpoint             | Auth required | Description                  |
|--------|-----------------------|:--------------:|-------------------------------|
| POST   | `/api/auth/register`  | No             | Create a new account          |
| POST   | `/api/auth/login`     | No             | Log in, receive a JWT         |
| GET    | `/api/auth/me`        | Yes            | Get the current user's profile|

**Register/Login response shape:**
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "user": { "id": 1, "name": "Jane Doe", "email": "jane@example.com", "createdAt": "..." }
}
```

### Tasks

| Method | Endpoint                    | Description                              |
|--------|-------------------------------|--------------------------------------------|
| GET    | `/api/tasks`                  | List the current user's tasks (supports `search`, `status`, `priority`, `sortBy`, `order` query params) |
| GET    | `/api/tasks/summary`          | Task counts (total/pending/in-progress/completed) |
| GET    | `/api/tasks/{id}`             | Get a single task (must belong to the user) |
| POST   | `/api/tasks`                  | Create a task                             |
| PUT    | `/api/tasks/{id}`              | Update a task                             |
| PATCH  | `/api/tasks/{id}/status`       | Update only the status                    |
| DELETE | `/api/tasks/{id}`              | Delete a task                             |

Example query: `GET /api/tasks?search=report&status=PENDING&priority=HIGH&sortBy=dueDate&order=asc`

**Error response shape (all errors):**
```json
{
  "success": false,
  "message": "Task not found",
  "status": 404,
  "timestamp": "2026-08-31T10:15:30",
  "errors": ["title: Title is required"]
}
```

## 8. Authentication Flow

1. User registers or logs in via `/api/auth/register` or `/api/auth/login`.
2. On success, the backend returns a signed JWT (HS256) containing the user's
   email as the subject, with an expiration.
3. The frontend stores the token in `localStorage` (`auth.js`) and attaches it
   as `Authorization: Bearer <token>` on every subsequent request (`api.js`).
4. `JwtAuthenticationFilter` runs on every request, validates the token, loads
   the user via `CustomUserDetailsService`, and sets Spring Security's
   authentication context.
5. Controllers read the authenticated user from `Authentication.getName()` —
   they never trust a user ID sent from the client.
6. `TaskService` double-checks ownership on every read/update/delete, returning
   `404 Not Found` (not `403`) if a task belongs to someone else, so task IDs
   belonging to other users aren't confirmed to exist.

Passwords are hashed with BCrypt (`BCryptPasswordEncoder`) before being stored,
and the `User` entity uses `@JsonIgnore` on `password` so it can never be
serialized back into an API response even by accident.

## 9. Environment Variables

See `.env.example` for the full list. Summary:

| Variable            | Purpose                                      |
|---------------------|-----------------------------------------------|
| `DB_URL`             | JDBC URL for PostgreSQL                       |
| `DB_USERNAME`        | Database username                             |
| `DB_PASSWORD`        | Database password                             |
| `JWT_SECRET`         | Base64 secret used to sign JWTs (32+ bytes)   |
| `JWT_EXPIRATION_MS`  | Token lifetime in milliseconds                |
| `FRONTEND_URL`       | Comma-separated CORS allowed origins          |
| `SERVER_PORT`        | Port the backend listens on                   |
| `DDL_AUTO`           | Hibernate schema strategy (see section 12)    |

Spring Boot reads these directly as environment variables — there's no need for
a library to load `.env`, as long as you export them in your shell (see setup
steps below) or configure them in your IDE's run configuration.

## 10. Local Setup

### 10.1 Prerequisites

- **Java 21** — check with `java -version`
- **Maven** (or use the included wrapper if you add one) — check with `mvn -version`
- **PostgreSQL** running locally — check with `psql --version`
- A way to serve static files for the frontend (VS Code "Live Server" extension,
  or Python's built-in server)

### 10.2 Install PostgreSQL & create the database

```bash
# macOS (Homebrew)
brew install postgresql@16
brew services start postgresql@16

# Ubuntu/Debian
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo service postgresql start

# Then create the database:
psql -U postgres -c "CREATE DATABASE taskflow;"
```

### 10.3 Configure environment variables

Copy the example file and fill in your own values:

```bash
cp .env.example .env
```

Generate a strong JWT secret:

```bash
openssl rand -base64 32
```

Paste that value into `JWT_SECRET` in `.env`, and set `DB_PASSWORD` to your
local Postgres password.

Export the variables into your shell before running the backend (Spring Boot
reads them as OS environment variables):

```bash
export $(grep -v '^#' .env | xargs)
```

(On Windows PowerShell, set each variable with `$env:DB_PASSWORD="..."` etc., or
configure them in your IDE's Run Configuration → Environment Variables.)

### 10.4 Run the backend

```bash
cd backend
mvn spring-boot:run
```

The API will start on `http://localhost:8080` (or whatever `SERVER_PORT` you
set). On first run, Hibernate will create the `users` and `tasks` tables
automatically (see section 12).

To build a runnable JAR instead:

```bash
mvn clean package
java -jar target/taskflow-backend.jar
```

### 10.5 Serve the frontend

The frontend is plain static files, so any static file server works. Two easy
options:

```bash
# Option A: Python's built-in server
cd frontend
python3 -m http.server 5500

# Option B: VS Code's "Live Server" extension
# Right-click index.html → "Open with Live Server"
```

Then open `http://localhost:5500` in your browser.

> If you serve the frontend on a different port, add that origin to
> `FRONTEND_URL` in `.env` so CORS allows it.

By default `frontend/js/api.js` points at `http://localhost:8080/api`. If your
backend runs elsewhere, set `window.TASKFLOW_API_BASE_URL` before `api.js`
loads (e.g. add a small `<script>` in the relevant HTML file), or edit the
`API_BASE_URL` constant directly.

### 10.6 Try it out

1. Open the app, click **Sign Up**, and register a user.
2. You'll be redirected to the dashboard automatically (registration logs you in).
3. Click **+ New Task** to create a task, fill in a title/description/priority/due date.
4. From the dashboard, use the search box, status/priority filters, and sort
   dropdown to find tasks.
5. Click a task title to view its details, change its status inline, edit it,
   or delete it.
6. Click **Logout**, then **Log In** again with the same credentials to confirm
   login works end-to-end.

## 11. Running Tests

```bash
cd backend
mvn test
```

Tests use an in-memory H2 database (configured in
`src/test/resources/application-test.properties`) so they don't touch your real
PostgreSQL data. Coverage includes:

- `AuthServiceTest` — registration validation, duplicate email rejection,
  password hashing, login success/failure
- `TaskServiceTest` — CRUD operations, search/filter/sort, and — importantly —
  that one user can never read, update, or delete another user's task
- `AuthControllerTest` / `TaskControllerTest` — full HTTP-level integration
  tests via MockMvc, including the same cross-user authorization checks at the
  API layer

## 12. Database Schema Management

Three different concerns, often confused:

- **Development database creation**: For local development, this project uses
  `spring.jpa.hibernate.ddl-auto=update` (see `DDL_AUTO` env var). Hibernate
  inspects your entities and creates/updates tables to match automatically.
  Convenient for learning and prototyping, but not something you'd want in
  production because it can make unexpected structural changes.
- **Database schema updates during development**: As you add fields to `User`
  or `Task`, restart the backend with `ddl-auto=update` and Hibernate adjusts
  the schema for you. Fine for a solo learning project; risky on a shared team
  database because it doesn't track history or allow rollback.
- **Production database migrations**: In a real production setup, you would
  switch `ddl-auto` to `validate` (Hibernate checks the schema matches but
  never changes it) and use a dedicated migration tool such as **Flyway** or
  **Liquibase** to apply versioned, reviewable, rollback-able SQL migration
  scripts. This project doesn't include Flyway/Liquibase since deployment is
  out of scope for now, but the entities and constraints are already written
  in a way that maps cleanly to migration scripts when you're ready to add one.

## 13. CORS

The browser enforces the **same-origin policy**: a page served from
`http://localhost:5500` is, by default, not allowed to call an API on
`http://localhost:8080` — they're different origins (different ports). Since
this project deliberately serves the frontend and backend separately (no
bundler, no framework, no reverse proxy), the backend must explicitly opt in
to allow the frontend's origin via CORS. This is configured in
`SecurityConfig.corsConfigurationSource()` and driven by the `FRONTEND_URL`
environment variable, so you can add more allowed origins without touching
code.

## 14. Troubleshooting

| Problem | Likely cause / fix |
|---|---|
| Backend fails to start with a datasource error | PostgreSQL isn't running, or `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` are wrong/not exported |
| `401 Unauthorized` on every request | Token missing/expired — log out and log back in; check `JWT_SECRET` didn't change between restarts |
| CORS error in the browser console | The frontend's origin isn't in `FRONTEND_URL`; restart the backend after changing `.env` |
| `JWT_SECRET` startup error about key length | The secret must be Base64 and decode to at least 32 bytes — regenerate with `openssl rand -base64 32` |
| Frontend shows "Could not reach the server" | Backend isn't running, or `API_BASE_URL` in `api.js` doesn't match where it's actually running |
| Tests fail locally | Make sure you're running `mvn test` (uses H2, not your real Postgres) — no extra setup should be needed |
| Table not updating after entity change | Confirm `DDL_AUTO=update` is set; for a clean slate in dev you can drop and recreate the `taskflow` database |

## 15. Git

To turn this into a Git repository:

```bash
cd taskflow
git init
git add .
git commit -m "Initial commit: TaskFlow full-stack app"
```

The included `.gitignore` already excludes `.env`, build output (`target/`),
IDE folders, and other files that should never be committed. Double-check
`git status` before your first commit to make sure no `.env` file or secret
is staged.

---

**Deployment is intentionally not covered here** — this project is set up for
local development only, ready for you to deploy manually whenever you're ready.
