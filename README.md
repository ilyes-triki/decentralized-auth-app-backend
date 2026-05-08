# Decentralized Auth — Backend

This repository contains the **Spring Boot backend API** for the decentralized authentication platform.
It handles wallet-based authentication, email OTP verification, IP risk detection/blocking, account status enforcement, and admin moderation endpoints.

Features: wallet signature login, OTP email verification, IP risk + auto/manual blocking, admin moderation APIs.  
API base URL examples: `/api/auth` (public auth flow), `/api/admin` (admin-only endpoints).

## Clone And Run (fast setup)

```bash
git clone <your-repo-url>
cd decentralized-auth-backend/backend
./mvnw spring-boot:run
```

Required for real usage: set `APP_AUTH_JWT_SECRET` (strong Base64 secret), configure `APP_CORS_ALLOWED_ORIGINS`, optionally set `APP_AUTH_ADMIN_WALLETS`, and set `SPRING_DATASOURCE_*` if not using local H2.

## Backend Docs

Full backend setup, prerequisites, configuration, and API run instructions:

**[`backend/README.md`](backend/README.md)**

## Quick Start

```bash
cd backend
./mvnw spring-boot:run
```
