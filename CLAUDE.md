# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## What This Is

Spring Boot 2.7 + MongoDB Atlas backend for the **Cachar District Office** platform. It serves three distinct client apps:

| Client | Auth Role | What it does |
|--------|-----------|--------------|
| Citizen portal (web) | `CITIZEN` | File complaints, track status |
| Officer dashboard (web) | `OFFICER` / officer sub-roles | Manage complaints, tasks, elections |
| SMC Karmachari (Android app) | `WORKER` / `WORKER_ADMIN` | Attendance, photo uploads, squad admin |

Deployed on **Render** as a Docker Web Service. GitHub push → Render auto-deploys via `Dockerfile`.

---

## Build & Run

```bash
# Run locally (uses application.properties — MongoDB Atlas credentials embedded)
mvn spring-boot:run

# Build JAR
mvn clean package -DskipTests

# Build Docker image (same as Render)
docker build -t world_of_dc .
docker run -p 8080:8080 -e JWT_SECRET=<secret> world_of_dc

# Compile check only
mvn compile -q

# Run tests
mvn test
```

Production requires one environment variable: `JWT_SECRET` (set in Render dashboard). MongoDB URI is currently hardcoded in `application-prod.properties` (same Atlas cluster as dev).

---

## Architecture

### Package Layout

```
org.dcoffice.cachar/
  config/          SecurityConfig, JwtAuthenticationFilter, CorsConfig
  controller/      One controller per domain (see list below)
  service/         Business logic — controllers are thin, services own validation
  repository/      MongoRepository interfaces — Spring Data auto-implements queries
  entity/          MongoDB @Document classes
  dto/             Request/response shapes (never expose entity directly)
  exception/       GlobalExceptionHandler
  util/
```

### Auth Flow

JWT is stateless (no session). `JwtAuthenticationFilter` runs on every request:
1. Parses the `Bearer` token → extracts `sub` (userId) and `role` claim
2. Validates user exists in DB (different collection per role)
3. Sets `UsernamePasswordAuthenticationToken` with `ROLE_<role>` authority

**Roles and their token generators in `JwtService`:**
- `CITIZEN` → `generateTokenForCitizen()`
- `OFFICER` / officer sub-roles → `generateTokenForOfficer()` (uses `OfficerRole` enum)
- `WORKER` → `generateTokenForWorker()` when `TrackingMember.admin == false`
- `WORKER_ADMIN` → `generateTokenForWorker()` when `TrackingMember.admin == true`

In controllers, get the caller's ID with `authentication.getName()` (the JWT `sub`).

### Public vs Protected Endpoints

`SecurityConfig` — everything is **protected by default** (`anyRequest().authenticated()`). Explicitly permitted:
- `/auth/**` — worker OTP login/signup
- `/api/tracking/**` — ALL methods (squad/member CRUD has no auth — intentional for DC web UI)
- `/api/citizen/**` — registration and public lookup
- `/api/vehicles/**`, `/api/polling-parties/**`, `/api/polling-stations/**` — election ops
- `/api/files/download/**`, `/api/complaints/track/**` — public read

The `/admin/**` endpoints check `ROLE_WORKER_ADMIN` manually inside the controller via `authentication.getAuthorities()`.

### Worker / Tracking Domain

The SMC Karmachari app uses a separate auth path from the citizen/officer system:

- **Login flow**: `POST /auth/send-otp` (no-op, OTP is `STATIC_OTP = "24052026"` in `WorkerService`) → `POST /auth/login` with `{mobile, otp}` → returns `{user: WorkerUserDto, token}`
- **WorkerUserDto** includes `id, mobile, name, address, createdAt, isAdmin, squadId`
- **Squad scoping**: `GET /admin/workers` and `GET /admin/attendance/today` auto-scope to the caller's `squadId`. If the caller has no `squadId`, returns all members (global fallback)
- **Set supervisor**: `PUT /admin/workers/{memberId}/supervisor?enable=true` (requires existing `WORKER_ADMIN` token). Alternatively, `PUT /api/tracking/members/{memberId}` with `{"admin": true}` works without auth (tracking endpoints are public)
- **File uploads**: worker photos stored under `./uploads/worker-photos/` (local disk — not persisted across Render redeploys). `imageUri` in responses uses `app.base-url` property which defaults to `http://localhost:8080`; the mobile app rewrites this to the real URL client-side

### Key External Dependencies

| Dependency | Used for |
|------------|----------|
| Uber H3 | Geospatial hexagonal indexing for polling station clustering |
| OSRM | Road distance matrix via `OsrmClient` (external HTTP) |
| iTextPDF | Route PDF generation (`PdfRouteService`) |
| ORS (OpenRouteService) | Routing API, key in `application.properties` |
| MongoDB Atlas | Single cluster, database `cachar_complaints` |

### Important Quirks

- **OTP is static**: `WorkerService.STATIC_OTP = "24052026"`. Replace `sendOtp()` body with real SMS provider when ready — the two-step API contract is already in place.
- **`/api/tracking/**` is fully public**: adding auth here will break the DC web UI (it doesn't send a token).
- **File uploads are ephemeral on Render**: Render's filesystem resets on redeploy. Uploaded files (`./uploads/`) are lost. Move to object storage before going to production with real photos.
- **`application.properties` has real credentials**: MongoDB URI and ORS API key are committed in plaintext. Only `jwt.secret` is externalized in prod via `${JWT_SECRET}`.
- **`TrackingMember.admin`** drives both the JWT role (`WORKER_ADMIN`) and the mobile app's Admin tab visibility. Toggle it via the DC web UI Squad Management page or directly via the tracking API.
