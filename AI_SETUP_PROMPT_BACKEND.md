# Backend Setup Prompt (Any AI)

```text
Help me set up and run the backend from a fresh clone.

Backend path:
- ./decentralized-auth-backend/backend

Tasks:
1) Verify Java and Maven wrapper.
2) Configure required backend environment values.
3) Run with local H2 first.
4) Optionally run with PostgreSQL profile.
5) Run tests.
6) Summarize final configured values and run commands.

Required variable guidance:
- APP_AUTH_JWT_SECRET: required for real usage; strong Base64 secret.
- APP_CORS_ALLOWED_ORIGINS: must include frontend URL (http://localhost:4200 in dev).
- APP_AUTH_ADMIN_WALLETS: optional comma-separated lower-case admin wallets.
- APP_AUTH_DEV_FIXED_OTP: dev/test helper only; do not keep in production.
- SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD: set only when using PostgreSQL.

Run commands:
- Local H2:
  - cd decentralized-auth-backend/backend
  - ./mvnw spring-boot:run
- Tests:
  - ./mvnw test
- PostgreSQL:
  - set datasource env vars
  - ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres

Validation checks:
- Confirm app starts on localhost:8080.
- Confirm /api/auth endpoints respond.
- Confirm /api/admin/health requires valid admin JWT.
- Confirm Flyway migration status is healthy.

If errors occur:
- diagnose and fix environment/config issues,
- explain root cause briefly,
- continue until backend is usable.
```

