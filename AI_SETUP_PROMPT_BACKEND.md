# Backend Setup Prompt (Cursor Agent)

Use with **AI_SETUP_PROMPT_GENERAL.md** (read that first) and **AI_SETUP_PROMPT_FRONTEND.md**.

```text
Help me set up and run the Spring Boot backend from a fresh clone.

Backend path (MANDATORY working directory):
- ./decentralized-auth-backend/backend

WARNING — H2 database location:
- H2 file is ./data/authdb relative to the PROCESS WORKING DIRECTORY.
- Always run from backend/ folder. If IntelliJ/Cursor uses parent folder as cwd,
  a SECOND database is created at decentralized-auth-backend/data/authdb (causes confusion).
- Set IDE run configuration "Working directory" to: .../decentralized-auth-backend/backend

======================================================================
PREREQUISITES
======================================================================

- JDK 21 (JAVA_HOME must point to JDK, not JRE)
- Maven wrapper included (mvnw / mvnw.cmd) — no global Maven install required
- Port 8080 free (or ask me for alternate port)

Verify:
  java -version    → must show 21
  echo %JAVA_HOME% (Windows) or echo $JAVA_HOME (bash)

If JDK 21 missing → tell me to install Temurin/OpenJDK 21 and STOP until confirmed.

======================================================================
ASK ME BEFORE PROCEEDING (personal / missing local data)
======================================================================

Do NOT guess. Ask me explicitly if any of these apply:

1) ADMIN WALLET
   - Repo default: 0x97550031867e9483c6f9cff121e683eddeac6f5e (application.properties)
   - Env override: APP_AUTH_ADMIN_WALLETS (comma-separated lowercase 0x addresses)
   - ASK: "Which MetaMask address should have admin role on this PC?"
   - If I give my address → set APP_AUTH_ADMIN_WALLETS or update run config env var
   - Admin role is granted at LOGIN time when wallet matches this list

2) DEMO / MOCK DATA
   - Flyway V2 + V5 seed demo users, login history, IP risk events, blocklist on first start
   - Reference: ../mock_wallet_emails_and_threat_profiles.txt (project root, may be absent)
   - ASK if I need demo login credentials:
     * wallet1.active.demo@mock-security.local + wallet 0x11c759…dae0d7 (LOW, active)
     * Other demo wallets are for admin dashboard display — not for MetaMask login unless I import those keys (I won't)
   - If file missing and I need demo table → ask me to provide mock_wallet_emails_and_threat_profiles.txt

3) JWT SECRET
   - Dev default exists in application.properties (fine for local only)
   - ASK before changing if this is staging/production

4) DATABASE CHOICE
   - Default: H2 file (zero config) — use this unless I say PostgreSQL
   - If PostgreSQL → ASK for: SPRING_DATASOURCE_URL, USERNAME, PASSWORD, database name

5) FIXED OTP (optional dev shortcut)
   - Default: random OTP logged as EMAIL_OTP_DEV in backend console
   - ASK: "Do you want APP_AUTH_DEV_FIXED_OTP=123456 for easier testing?"
   - Never set in production

6) PORT CONFLICT
   - If 8080 in use → ASK: stop existing process OR use SERVER_PORT=8081
   - If port changes → remind me to update frontend environment.ts apiBaseUrl

7) IP BLOCKLIST
   - If DB copied from another machine may contain 127.0.0.1 block
   - ASK before deleting blocklist rows unless login fails with IP_BLOCKED

======================================================================
INSTALL & RUN (no npm — Maven only)
======================================================================

Backend does NOT use npm. Maven downloads Java dependencies on first run.

Windows:
  cd decentralized-auth-backend\backend
  mvnw.cmd spring-boot:run

bash:
  cd decentralized-auth-backend/backend
  ./mvnw spring-boot:run

IntelliJ:
  Open BackendApplication.java → Run
  Working directory MUST be: decentralized-auth-backend/backend

Optional env vars (set in IDE run config or shell):
  APP_AUTH_ADMIN_WALLETS=0x<my-lowercase-address>
  APP_CORS_ALLOWED_ORIGINS=http://localhost:4200
  SERVER_PORT=8080

======================================================================
POSTGRESQL (only if I requested it)
======================================================================

  set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/decentralized_auth
  set SPRING_DATASOURCE_USERNAME=postgres
  set SPRING_DATASOURCE_PASSWORD=<ask me>
  mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres

======================================================================
VALIDATION (run these; fix until pass)
======================================================================

1) Startup log contains: "Tomcat started on port(s): 8080"
2) Flyway: "Successfully applied" or "Schema is up to date" (version 6+)
3) Filter registration logs show security filters "disabled" for servlet registration (expected)
4) Smoke request:
   curl http://localhost:8080/api/auth/email-status?address=0xabc1234567890abcdef1234567890abcdef12
   → JSON response (not connection refused; 400/200 OK, NOT 403 IP_BLOCKED unless testing blocks)
5) Admin health (needs JWT — skip or use after login):
   GET /api/admin/health → 403 without token is OK

Run tests:
  mvnw.cmd test   (Windows)
  ./mvnw test     (bash)
  → all tests should pass

======================================================================
DEV LOGIN — OTP LOCATION (tell me during setup)
======================================================================

Email OTP is NOT sent to real inbox in dev. After POST /api/auth/email/start:
- Read backend console for log line: EMAIL_OTP_DEV ... otp=XXXXXX
- Tell me where to find it when demonstrating login

======================================================================
TROUBLESHOOTING
======================================================================

| Error | Action |
|-------|--------|
| Port 8080 already in use | netstat -ano \| findstr :8080 → kill LISTENING PID; ASK me first |
| JAVA_HOME not set | Install JDK 21, set JAVA_HOME, restart terminal |
| Database file locked | Only one backend instance; stop duplicate process |
| Flyway checksum error (H2 dev) | app.flyway.repair-before-migrate=true handles local H2 |
| CORS errors from browser | APP_CORS_ALLOWED_ORIGINS must include http://localhost:4200 |
| IP_BLOCKED on all requests | Unblock 127.0.0.1 in ip_blocklist — ASK me first |

======================================================================
REPORT BACK
======================================================================

When done, summarize:
- Working directory used
- Port and database path (backend/data/authdb.mv.db)
- Admin wallet configured (address)
- Env vars changed (if any)
- Test results (mvn test)
- What you still need from me (if anything)
```
