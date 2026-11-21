# TaskFlow

TaskFlow is a task tracker for small teams. Create projects, assign tasks, and manage deadlines from a responsive Kanban board.

## Run with Docker Compose

1. Copy `.env.example` to `.env`. Set `DB_PASSWORD` to a strong password and `JWT_SECRET` to at least 32 random bytes encoded as Base64. Generate a JWT key with `openssl rand -base64 32` or the PowerShell command below.
2. Run `docker compose up --build`.
3. Open [TaskFlow](http://localhost:3000), then register an account. Open the [API documentation](http://localhost:8080/swagger-ui.html) to try API endpoints with a Bearer token from login.

```powershell
$key = [byte[]]::new(32)
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($key)
$rng.Dispose()
[Convert]::ToBase64String($key)
```

PostgreSQL persists data in the Compose volume. Flyway applies migrations on backend startup. Set `TASKFLOW_UI_PORT` or `TASKFLOW_API_PORT` in `.env` if the default host ports are occupied.

## Run locally

Requirements are Java 21, Maven 3.9 or later, and Node 20 or later.

```sh
# Terminal 1
cd backend
mvn spring-boot:run

# Terminal 2
cd frontend
npm ci
npm run dev
```

Open [Vite](http://localhost:5173). The default `dev` profile uses in-memory H2 and generates a signing key when no JWT secret is provided. Data and sessions reset after an API restart. To use standalone PostgreSQL, start the application with `SPRING_PROFILES_ACTIVE=prod` and supply `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, and `JWT_SECRET`. The host, port, database, and username default to `localhost`, `5432`, `taskflow`, and `taskflow`.

### Demo data

Set `TASKFLOW_DEMO_PASSWORD` to a local password of at least six characters. Run with `SPRING_PROFILES_ACTIVE=dev,demo` for H2 or set `TASKFLOW_PROFILES=prod,demo` in the Compose environment file. On an empty database, the seed creates projects, tasks, assignees, and sample activity with relative due dates. Sign in as `demo@taskflow.com` with the password you set.

## Tests

- Backend: `cd backend` and run `mvn verify`. JUnit 5 and Mockito cover service decisions and sorting. The Spring Boot MockMvc integration test covers login, JWT protection, ownership, validation, task lifecycle, filters, priority sorting, paging, overdue counts, and activity.
- Frontend: `cd frontend`, run `npm ci`, `npm test`, and `npm run build`. Jest with React Testing Library exercises task cards, form labels and submission, and retry behavior.
- GitHub Actions runs frontend checks and backend verification using a disposable PostgreSQL service. Local tests default to H2.

## Architecture

- `backend/` contains a Spring Boot REST API under feature packages for authentication, projects, and tasks. Controllers validate requests and call transactional services. Services enforce project ownership and use Spring Data repositories. Mappers build response DTOs and keep persistence entities out of HTTP responses.
- **Strategy and Factory.** Sort strategies define database ordering for due date, priority, and creation time. The strategy factory selects the requested implementation. Priority uses a Hibernate formula to sort by business rank. An ID tie-breaker stabilizes pagination.
- **Mapper and Repository.** Mappers convert domain entities to DTOs, including task overdue flags. Repositories handle database access and task specifications assemble filters.
- `frontend/` contains the React client. Axios handles HTTP and session expiry centrally. TanStack Query manages API caching, loading states, and mutation invalidation. React Hook Form and Zod validate forms. React Router protects workspace screens.
- `compose.yaml` runs PostgreSQL, the API, and the frontend. Vite and Nginx proxy `/api` on the frontend origin.

### API behavior and trade-offs

- A project owner is the only user allowed to access its tasks, summary, and activity. Any registered user can be selected as an assignee. Assignment grants no project access. The authenticated directory exposes assignee names and IDs.
- A due date is required. New tasks start in To Do and require today or a future date. Editing can retain a past date. Open tasks due before the server's current calendar date are overdue.
- The board shows one filtered page across all three columns. Summary cards and column totals reflect the entire project. The API uses zero-based pages and caps the page size at 100. Use `assigneeId=0` to filter unassigned tasks.
- JWTs live in browser session storage and expire after 24 hours by default. The client clears expired credentials after a 401. The board uses a keyboard-accessible status dropdown to move tasks.
