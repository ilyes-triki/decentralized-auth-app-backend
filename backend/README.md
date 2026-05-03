# Decentralized Auth — Backend API

Spring Boot service that powers wallet-based authentication: nonce challenges, signature verification (via Web3j), JWT sessions, role-aware REST endpoints, rate limiting, structured errors, and HTTP access auditing.

---

## Tech stack

| Area | Technology |
|------|------------|
| Runtime | Java **21** |
| Framework | **Spring Boot** 3.5.x |
| Security | Spring Security, **JWT** (JJWT) |
| Persistence | **Spring Data JPA**, **Flyway** migrations |
| Databases | **H2** (file, dev default), **PostgreSQL** (recommended for production) |
| Crypto / Web3 | **Web3j** (Ethereum signature verification) |

---

## Prerequisites

- **JDK 21** (`JAVA_HOME` must point to the JDK)
- **Maven** (or use the included `./mvnw` wrapper)

Optional:

- **PostgreSQL** 14+ when not using the embedded H2 profile

---

## Quick start (local, H2)

From this directory (`backend/`):

```bash
./mvnw spring-boot:run
```

Default datasource is an **H2 file database** (`./data/authdb.mv.db` relative to the process working directory). Flyway applies migrations on startup.

API base path for authentication flows is under **`/api/auth`**. Default HTTP port is **8080** (Spring Boot default).

---

## Quick start (PostgreSQL)

1. Create a database (example name: `decentralized_auth`).
2. Run with the `postgres` Spring profile and JDBC settings:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/decentralized_auth"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="your-password"

./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Profile-specific defaults live in `src/main/resources/application-postgres.properties` (including Flyway repair disabled for production-grade behaviour).

---

## Configuration

All keys can be supplied via environment variables or `application.properties`. Important variables:

| Variable | Purpose |
|----------|---------|
| `APP_AUTH_JWT_SECRET` | **Required in production:** Base64-capable secret for signing JWTs. Replace the development default. |
| `APP_AUTH_JWT_EXPIRATION_SECONDS` | JWT lifetime (default `3600`). |
| `APP_AUTH_NONCE_TTL_SECONDS` | Server nonce validity window. |
| `APP_AUTH_ADMIN_WALLETS` | Comma-separated **lower-case** wallet addresses granted admin role at login. |
| `APP_CORS_ALLOWED_ORIGINS` | Allowed browser origins (default includes `http://localhost:4200`). |
| `APP_SECURITY_RATE_LIMIT_MAX_REQUESTS` | Max requests per client per window for sensitive endpoints. |
| `APP_SECURITY_RATE_LIMIT_WINDOW_SECONDS` | Rate-limit sliding window length. |
| `SPRING_DATASOURCE_*` | JDBC URL, user, password, driver (see `application.properties`). |

Logging: HTTP audit lines use logger **`AUDIT_HTTP`** (see `logging.level.AUDIT_HTTP`).

---

## REST API overview

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/auth/nonce` | Public | Issues a sign-in challenge for a wallet address. |
| POST | `/api/auth/login` | Public | Verifies signature and returns JWT + role. |
| GET | `/api/profile/me` | Bearer JWT | Returns the authenticated user profile from token claims. |
| GET | `/api/admin/health` | Bearer JWT (admin) | Simple admin health check. |
| GET | `/api/admin/login-history` | Bearer JWT (admin) | Login history sample for analytics. |
| GET | `/api/admin/stats` | Bearer JWT (admin) | Aggregated login statistics. |
| GET | `/api/admin/access-log` | Bearer JWT (admin) | Recent HTTP access audit rows. |

Errors use a structured JSON body (`message`, `status`, `timestamp`, `errorCode`, `requestId`) via `GlobalExceptionHandler`.

---

## Database migrations

Flyway scripts live under `src/main/resources/db/migration/`. Hibernate runs with **`ddl-auto=validate`** — schema changes must go through Flyway.

---

## Testing

```bash
./mvnw test
```

Integration tests cover JWT security and controller behaviour; ensure **JDK 21** is available before running.

---

## Security notes for production

1. Set a strong, unique **`APP_AUTH_JWT_SECRET`** — never ship the development default.
2. Restrict **`APP_CORS_ALLOWED_ORIGINS`** to your real frontend origins.
3. Use **PostgreSQL** (or another managed DB) instead of H2 for durability and concurrency.
4. Disable Flyway repair-on-start (`app.flyway.repair-before-migrate=false`) unless you maintain migrations deliberately (postgres profile already aligns with this).
5. Keep admin wallet lists minimal and review **`APP_AUTH_ADMIN_WALLETS`** regularly.

---

## Troubleshooting

| Issue | Suggestion |
|-------|------------|
| `JAVA_HOME` not set | Install JDK 21 and point `JAVA_HOME` at it; restart the IDE/terminal. |
| Port 8080 in use | Set `server.port` (e.g. `export SERVER_PORT=8081`). |
| Flyway checksum errors (H2 dev) | Repair is enabled by default for local H2; for Postgres use controlled migrations only. |
| CORS errors from browser | Align `APP_CORS_ALLOWED_ORIGINS` with your Angular dev server URL. |

---

## License

Use and license terms follow your organization or repository root `LICENSE` file (if present).
