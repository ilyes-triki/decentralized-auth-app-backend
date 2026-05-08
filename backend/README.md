# Decentralized Auth — Backend API

Spring Boot service that powers **email-verified** wallet authentication: OTP lifecycle (dev-mock logging), ticket-bound nonce challenges, signature verification (via Web3j), JWT sessions, **IP risk scoring and blocklist** enforcement, role-aware REST endpoints, rate limiting, structured errors, and HTTP access auditing.

Features: OTP + wallet login binding, trusted returning-wallet login, IP threat scoring/auto-block, admin moderation APIs.  
API base URL examples: `/api/auth` for authentication and `/api/admin` for moderation/admin dashboards.

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
git clone <your-repo-url>
cd decentralized-auth-backend/backend
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
| `APP_SECURITY_IP_TRUST_X_FORWARDED_FOR` | When `true`, first `X-Forwarded-For` hop is trusted for client IP (off by default to reduce spoofing). |
| `APP_SECURITY_IP_AUTO_BLOCK_MINUTES` | Duration for **HIGH** risk and auth-failure auto-blocks (default 30). |
| `APP_SECURITY_IP_AUTH_FAILURE_WINDOW_SECONDS` / `APP_SECURITY_IP_AUTH_FAILURE_THRESHOLD` | Sliding window + count of failed logins toward auto-block. |
| `APP_AUTH_EMAIL_OTP_TTL_SECONDS` | OTP validity (default 600s). |
| `APP_AUTH_EMAIL_TICKET_TTL_SECONDS` | Email ticket TTL for nonce/login binding (default 900s). |
| `APP_AUTH_EMAIL_OTP_MAX_VERIFY_ATTEMPTS` | Max OTP verification attempts per issued code. |
| `APP_AUTH_DEV_FIXED_OTP` | **Tests / local only:** if set, accept this OTP without persisting random codes (empty in production). |
| `SPRING_DATASOURCE_*` | JDBC URL, user, password, driver (see `application.properties`). |

Logging: HTTP audit lines use logger **`AUDIT_HTTP`** (see `logging.level.AUDIT_HTTP`). Email OTP codes in dev use **`EMAIL_OTP_DEV`**.

### Minimum environment for cloned project

- **Must set in staging/production:** `APP_AUTH_JWT_SECRET` (strong Base64 secret; never keep development default).
- **Must align with frontend URL:** `APP_CORS_ALLOWED_ORIGINS` (for example `http://localhost:4200` in dev).
- **Optional but recommended:** `APP_AUTH_ADMIN_WALLETS` for admin access control.
- **Database choice:** use default H2 for local quick start, or set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (+ `postgres` profile) for PostgreSQL.
- **Dev-only OTP shortcut:** `APP_AUTH_DEV_FIXED_OTP` can be set locally for deterministic testing; keep empty in production.

---

## REST API overview

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/email/start` | Public | Start email verification; creates OTP (logged in dev via `EMAIL_OTP_DEV`). |
| POST | `/api/auth/email/verify` | Public | Verifies OTP; returns short-lived **emailTicket** for nonce/login. |
| GET | `/api/auth/email-status` | Public | Returns whether a wallet is already linked to a verified email (used by wallet-only returning login UX). |
| GET | `/api/auth/nonce` | Public | Issues a sign-in challenge; accepts `emailTicket` for first-time email-linking flow, or no ticket for wallets already linked to a verified email. |
| POST | `/api/auth/login` | Public | Verifies signature + nonce; `emailTicket` required for first-time linking and optional for trusted returning wallets already linked to verified email; records `client_ip` on login history. |
| GET | `/api/profile/me` | Bearer JWT | Returns profile from token claims (address, email fields, `lastLoginIp` when known). |
| GET | `/api/admin/health` | Bearer JWT (admin) | Simple admin health check. |
| GET | `/api/admin/login-history` | Bearer JWT (admin) | Login history sample for analytics (includes `clientIp`). |
| GET | `/api/admin/stats` | Bearer JWT (admin) | Aggregated login statistics. |
| GET | `/api/admin/access-log` | Bearer JWT (admin) | Recent HTTP access audit rows. |
| GET | `/api/admin/ip-events` | Bearer JWT (admin) | Filterable IP risk events for review. |
| GET | `/api/admin/ip-blocks` | Bearer JWT (admin) | Active IP blocklist entries (auto + manual). |
| POST | `/api/admin/ip-blocks` | Bearer JWT (admin) | Manual block (optional permanent flag + reason). |
| DELETE | `/api/admin/ip-blocks/{ip}` | Bearer JWT (admin) | Remove a block entry (unblock). |

Errors use a structured JSON body (`message`, `status`, `timestamp`, `errorCode`, `requestId`) via `GlobalExceptionHandler`. Blocked IPs respond with **`403`** and **`errorCode: IP_BLOCKED`** before controllers run.

---

## Database migrations

Flyway scripts live under `src/main/resources/db/migration/`. Hibernate runs with **`ddl-auto=validate`** — schema changes must go through Flyway. Migration **`V4__email_ip_security.sql`** adds email OTP/ticket tables, IP risk/blocklist tables, and account email columns.

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
6. Leave **`APP_AUTH_DEV_FIXED_OTP`** empty in production; wire real email (SMTP/transactional provider) when replacing dev-mock OTP.
7. Only enable **`APP_SECURITY_IP_TRUST_X_FORWARDED_FOR`** behind a trusted reverse proxy that sanitizes client headers.

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
