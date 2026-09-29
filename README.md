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

- Spring Boot
- Java
- MySql
- REST APIs
- Entity Modeling
- Git & GitHub

## Architecture

The project follows a structured backend design with separation between:

- Domain entities
- Business logic
- Data access
- API layer
- Validation

## Project Status

Active development.

## Local configuration

The application reads credentials and signing material from environment variables. Use [`.env.example`](.env.example) as the list of required values; Spring Boot does not automatically load `.env` files, so provide the variables through your IDE, shell, container, or deployment secret manager.

Required secrets:

- `DB_PASSWORD`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `JWT_SECRET`, encoded as Base64 and containing at least 32 random bytes before encoding

The database URL, database username, mail host, mail port, and JWT lifetime have development defaults and may also be overridden through the variables shown in `.env.example`.

## Related Repository

Frontend:
`https://github.com/ghalebshhab/Salon-Booking-App-FrontEnd`

## Author

**Ghaleb Shhab**

- LinkedIn: https://www.linkedin.com/in/ghaleb-shhab/
- GitHub: https://github.com/ghalebshhab
