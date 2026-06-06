# General Project Setup Prompt (Cursor Agent)

Use this prompt **first**, together with `AI_SETUP_PROMPT_BACKEND.md` and `AI_SETUP_PROMPT_FRONTEND.md`.

```text
You are a Cursor agent helping me set up this full-stack Web3 authentication project from a fresh clone on a new PC.

Read and follow ALL THREE files in order:
1) AI_SETUP_PROMPT_GENERAL.md   (this file — orchestration)
2) AI_SETUP_PROMPT_BACKEND.md   (Spring Boot API)
3) AI_SETUP_PROMPT_FRONTEND.md  (Angular app)

======================================================================
PROJECT STRUCTURE
======================================================================

Monorepo / workspace layout:
- Frontend:     ./decentralized-auth
- Backend repo: ./decentralized-auth-backend
- Backend app:  ./decentralized-auth-backend/backend   ← run backend ONLY from here

Optional reference files (may not exist on every clone — ASK ME if you need them):
- ./mock_wallet_emails_and_threat_profiles.txt  — demo wallet/emails for admin dashboard
- ./IMPLEMENTATION_REPORT.html                  — project documentation

======================================================================
PREREQUISITES (verify before installing)
======================================================================

| Tool            | Required version | Check command        |
|-----------------|------------------|----------------------|
| JDK             | 21               | java -version        |
| Node.js         | 20+ LTS          | node -version        |
| npm             | 10+              | npm -version         |
| Git             | any recent       | git --version        |
| MetaMask        | browser ext.     | ask me if installed  |

If any prerequisite is missing, tell me exactly what to install and STOP until I confirm.

======================================================================
STARTUP ORDER (mandatory)
======================================================================

1. Backend first  → http://localhost:8080
2. Frontend second → http://localhost:4200
3. Browser → MetaMask + app UI

Never start frontend-only testing before backend responds on port 8080.

======================================================================
ASK-ME PROTOCOL (do NOT guess local/personal data)
======================================================================

Before finishing setup, check each item below. If you cannot determine it from the
repo or environment, STOP and ASK ME explicitly. Do not invent wallets, secrets,
emails, or OTP values.

### A) Identity & wallets (personal — must ask if not provided)

| Item | Why needed | Default in repo (if any) |
|------|------------|----------------------------|
| My MetaMask wallet address | Login + optional admin role | none — personal |
| Admin wallet for this PC | Admin dashboard access | `0x97550031867e9483c6f9cff121e683eddeac6f5e` in application.properties — ASK if I want to use MY wallet instead |
| Email for sign-in test | OTP flow | any email works in dev — ASK which email I want to use |

Question template:
"I need your MetaMask wallet address (0x…) to configure admin access. Should I set APP_AUTH_ADMIN_WALLETS to your address, or keep the repo default?"

### B) Demo / mock data (optional — ask if I need admin dashboard demos)

Flyway migrations auto-seed demo users, login history, IP events, and blocklist rows
on first backend start (H2). Reference file: mock_wallet_emails_and_threat_profiles.txt

| Item | Ask me if… |
|------|------------|
| Demo wallet list | I want to log in as a pre-seeded demo user (not my own MetaMask) |
| mock_wallet_emails_and_threat_profiles.txt | File is missing and I need the demo wallet/email/IP table |
| Admin dashboard screenshots | I need to know which demo wallets map to LOW/MEDIUM/HIGH risk |

If I need demo accounts AND the reference file is missing, ask me to provide it or paste the wallet/email list.

### C) Secrets & environment (ask before changing production-like values)

| Item | Dev default | Ask me if… |
|------|-------------|------------|
| APP_AUTH_JWT_SECRET | Dev value in application.properties | Deploying beyond local dev |
| APP_CORS_ALLOWED_ORIGINS | http://localhost:4200 | Frontend runs on another port/host |
| APP_AUTH_DEV_FIXED_OTP | empty (random OTP in logs) | I want a fixed OTP like `123456` for easier testing |
| PostgreSQL URL/user/password | not needed for H2 | I want PostgreSQL instead of H2 |
| Backend port | 8080 | Port 8080 is taken — ask which port to use |

Never commit real secrets. Never overwrite my .env files without showing me the diff.

### D) IP blocking awareness (warn me during setup)

If login fails with "Could not verify wallet email status" or IP_BLOCKED:
- Check whether 127.0.0.1 is in ip_blocklist (blocks ALL local API access).
- ASK before unblocking or deleting DB rows unless I explicitly request it.
- Warn me: blocking 127.0.0.1 blocks my own machine from login.

======================================================================
YOUR TASKS (execute end-to-end)
======================================================================

1) Verify prerequisites (JDK 21, Node 20+, MetaMask).
2) Run ASK-ME protocol for any missing items in sections A–C above.
3) Follow AI_SETUP_PROMPT_BACKEND.md — install/run backend from backend/ folder.
4) Follow AI_SETUP_PROMPT_FRONTEND.md — npm install, configure, run frontend.
5) Smoke-test:
   - GET http://localhost:8080/api/auth/email-status?address=0xabc… → not connection refused
   - App loads at http://localhost:4200
   - Login: email → OTP from backend log (EMAIL_OTP_DEV) → MetaMask sign
   - If admin wallet configured: /admin loads after login
6) Report final checklist (see below).
7) If anything fails: diagnose, fix, re-test until both apps run OR ask me for missing input.

======================================================================
COMMANDS QUICK REFERENCE
======================================================================

Windows (PowerShell / cmd):
  cd decentralized-auth-backend\backend
  mvnw.cmd spring-boot:run

  cd decentralized-auth
  npm install
  npm start

bash (Git Bash / Mac / Linux):
  cd decentralized-auth-backend/backend
  ./mvnw spring-boot:run

  cd decentralized-auth
  npm install
  npm start

If npm install fails with peer dependency errors:
  npm install --legacy-peer-deps

======================================================================
FINAL CHECKLIST (report to me when done)
======================================================================

- [ ] JDK 21 verified
- [ ] Backend running on port _____ (working directory: backend/)
- [ ] Flyway migrations applied (schema v6+)
- [ ] Frontend running on port 4200
- [ ] apiBaseUrl = http://localhost:8080/api/auth (or what I chose)
- [ ] CORS includes frontend origin
- [ ] Admin wallet configured: _____ (mine or default)
- [ ] MetaMask installed and tested
- [ ] Login smoke test: OTP read from backend logs
- [ ] Demo/mock data: using Flyway seeds / reference file / not needed
- [ ] Issues encountered and how they were resolved

If any checkbox could not be completed, explain why and what you need from me.
```
