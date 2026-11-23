# Salon Hub Backend

Backend services for **Salon Hub**, a full-stack salon management and booking platform.

## Overview

Salon Hub is designed to support salon discovery, service management, booking workflows, hiring posts, user profiles, media sharing, and interactions between salon owners and users.

This repository contains the backend implementation, including domain entities, business logic, database operations, and application APIs.

## Features

- User profile management
- Salon creation and management
- Service management
- Booking workflows
- Hiring posts
- Media and image handling
- Owner-user interactions
- Business logic and validation
- Database integration
- API endpoints for frontend integration

## Tech Stack

- Java 17 and Spring Boot 4.1 (Web, Security, Data JPA, Validation, Mail)
- Hibernate 7 with Flyway migrations (`src/main/resources/db/migration`)
- MySQL 8 locally; TiDB Cloud (MySQL compatible) in production
- httpOnly cookie sessions with rotating refresh tokens
- Docker image for deployment

## Architecture

The project follows a structured backend design with separation between:

- Domain entities
- Business logic
- Data access
- API layer
- Validation

## Project Status

Version 1.0.0, released on 23 November 2026, is in production. Each release is a Git tag (`v1.0.0`) with a GitHub release.

## Local configuration

The application reads credentials and signing material from environment variables. [`.env.example`](.env.example) lists every value. Copy it to a git-ignored `.env` file in this folder: `application.properties` imports `./.env` when it exists. You can also set the values in your IDE, shell or container.

Required secrets:

- `DB_PASSWORD`
- `JWT_SECRET`, encoded as Base64 and containing at least 32 random bytes before encoding
- `AUTH_CODE_SECRET`, a second, different random value
- `MAIL_PASSWORD` (SMTP) or `BREVO_API_KEY` (Brevo), depending on `MAIL_DELIVERY`

The `local` profile (`spring.profiles.active=local`) listens on port 8081. The database URL, database username, mail host, mail port and session lifetimes have development defaults and may be overridden through the variables shown in `.env.example`.

## Production

- **API:** https://salon-hub-backend.onrender.com, a Docker web service on Render. Every push to `master` deploys automatically. Health check: `/actuator/health`.
- **Database:** TiDB Cloud Starter. Flyway creates and updates the schema at start-up. Hibernate uses `TiDBCompatibleDialect`, because TiDB rejects MySQL 8's `FOR UPDATE OF <alias>` locks.
- **Email:** Render's free plan blocks SMTP, so production sends through the Brevo HTTPS API (`MAIL_DELIVERY=brevo`).
- **Values:** [`documentation/production-environment.md`](documentation/production-environment.md) lists every production setting. Secrets live only in the Render settings, never in Git.

## Related Repositories

- Website: [Salon-hub-frontend](https://github.com/ghalebshhab/Salon-hub-frontend), live at https://salon-community-frontend.vercel.app
- Admin dashboard: [salon-hub-admin-dashboard](https://github.com/ghalebshhab/salon-hub-admin-dashboard)

## Author

**Ghaleb Shhab**

- LinkedIn: https://www.linkedin.com/in/ghaleb-shhab/
- GitHub: https://github.com/ghalebshhab
