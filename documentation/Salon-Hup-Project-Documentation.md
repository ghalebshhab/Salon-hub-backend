# Salon Hup Project Documentation

**Document type:** Product and engineering specification
**Project:** Salon Hup
**Backend version:** 0.0.1 SNAPSHOT
**Document version:** 1.4.2
**Last updated:** 1 November 2026
**Status:** Active development

## Document purpose

This document is the maintained source of truth for the Salon Hup product. It covers the React frontend, Spring Boot backend, browser-to-API integration, product vision, implemented business rules, data model, complete API surface, security model, current delivery status, known risks, and planned development. It is intended for developers, reviewers, future contributors, quality assurance, operations, and stakeholders preparing the product for deployment.

The document distinguishes between implemented behavior and planned behavior. A capability is only marked as implemented when corresponding source code exists in the repository. Future changes should update this source after the implementation has been reviewed and approved.

> **Document convention:** `Implemented` means working source code exists in this repository. `Partial` means a foundation exists but production controls are incomplete. `Planned` describes an approved direction that has not yet been implemented. Planned content is never presented as current behavior.

## Executive overview

Salon Hup is a responsive web platform for salon discovery, service catalogs, salon ownership, employment, hiring, appointment booking, and verified customer reviews. The React client presents the customer, career, owner, and employee journeys; the Spring Boot API connects them through authenticated identity, explicit ownership rules, transactional state changes, and DTO-based contracts.

The application currently supports verified user registration, JWT login, secure email-code verification, password recovery, a unified normal/career user profile, personalized hiring-post discovery, salon creation requests with administrative approval, salon profile management, employment requests and owner invitations, employee leave and salon transfers, reversible administrative user and salon blocking, hiring-post creation, ranked applications, owner acceptance and rejection, an owner-managed public service catalog, employee service capabilities and schedules, appointment requests and decisions, customer-approved rescheduling, employee assignment and reassignment explanations, customer and employee booking views, persistent user-specific booking notifications, completion, and booking-verified salon reviews. The React experience also includes an owner-only operational dashboard, a shared plum-and-gold light/dark design system, contextual action loading, branded route-transition loading, centralized backend-error presentation, and a prototype route for reviewing completed product states.

The booking workflow now calculates appointment slots on the server from salon hours, selected-service duration, employee capabilities, weekly schedules, and accepted-booking conflicts. The React booking page displays those slots and the eligible employees for each time. Booking creation revalidates the chosen slot so stale or manipulated clients cannot bypass current availability. Booking decisions and material changes create durable inbox events in the same transaction. Salon reviews require a completed booking owned by the customer and remain linked to that booking. Schedule exceptions, push delivery, payment processing, employee ratings, and service ratings remain planned.

### Product status snapshot

| Capability group | Delivery state | Product meaning |
| --- | --- | --- |
| Identity and access | Implemented | Registration, email verification, resend cooldown, login, logout, password recovery, JWT authentication and invalidation, role authorization, and blocked-account enforcement exist |
| Career and hiring | Implemented | Unified career profiles, discovery ranking, applications, owner decisions, and hiring lifecycle exist |
| Salon administration | Implemented | Request review, rejection correction, resubmission, listing, blocking, and restoration exist |
| Workforce management | Implemented | Join, invitation, leave, transfer, and linked hiring acceptance flows exist |
| Salon service catalog | Implemented | Approved owners manage priced, timed services and public salon pages receive active offerings |
| Customer booking | Implemented | Customers select server-calculated slots; owners decide, assign, reschedule through customer-approved proposals, and complete requests |
| Employee availability | Implemented | Fifteen-minute slots reflect selected-service duration, qualified employees, weekly schedules, accepted conflicts, and a 30-minute same-day lead time |
| Notification inbox | Implemented | Durable booking events, authenticated pagination, unread count, owned read operations, and frontend polling are connected |
| Verified salon reviews | Implemented | A customer can review a completed owned booking once; public lists and aggregate summaries exclude blocked customers |
| Web experience | Implemented | Responsive React routes cover discovery, authentication, profiles, hiring, owner operations, bookings, employee setup, reviews, and an owner-only dashboard in a consistent light/dark theme |
| Interaction feedback | Implemented | Contextual Signature Snip action loader, branded route-transition loader, stable overlapping-request handling, and clear server-error messages are shared across active journeys |
| Frontend/API integration | Implemented | Core journeys use shared Axios and `ApiResponse`; salon cover and employee portfolio media use authenticated backend Cloudinary routes |
| Production operations | Partial | Environment secrets and health foundation exist; migrations, containers, durable revocation, monitoring, and hosting remain |

## Product scope

### In scope for the product

- Consumer accounts, salon owners, salon employees, and platform administrators.
- Salon onboarding and administrative governance.
- Career profiles, recruitment discovery, applications, and hiring decisions.
- Direct employment invitations, requests, leave, and salon transfer workflows.
- Public salon discovery, customer booking, employee assignment, and verified salon reviews.
- Operational controls required to deploy and support the backend safely.

### Current release boundary

The current backend release supports authentication, salon approval, career profiles, hiring, employment, administrative moderation, public service discovery, employee qualification and weekly availability, server-calculated appointment slots, customer-approved rescheduling, appointment lifecycle management, and verified salon reviews. It is not yet ready for production because schedule exceptions, payments, external push delivery, database migrations, durable token revocation, observability, and deployment infrastructure remain planned.

### Explicitly outside the current implementation

- Online payment processing, refunds, invoices, and financial reconciliation.
- Holidays, employee time off, automated reminders, and recurring appointments.
- Real-time chat, social feeds, employee and service ratings, and content moderation.
- Native mobile push notifications and production email templates.
- Multi-tenant billing, subscription plans, and salon analytics.

## Product principles

- **Server-owned identity:** clients never choose the authenticated user, owner, or salon relationship for protected workflows.
- **Reversible administration:** moderation preserves recoverable business state unless an explicit deletion policy applies.
- **One user profile:** career information extends the registered profile instead of creating a second identity.
- **Transparent ranking:** hiring labels and scores come from documented, reproducible rules.
- **Permanent application history:** a user applies once to a hiring post and historical decisions remain traceable.
- **Qualified assignment:** an assigned employee must be active in the salon, qualified for every selected service, and available for the complete appointment interval.
- **Verified feedback:** salon ratings originate only from completed bookings owned by the reviewing customer.
- **Safe evolution:** implemented behavior, planned behavior, and known production gaps remain visibly separated.

### Salon location and media delivery

Salon creation and editing accept an optional full HTTPS Google Maps URL together with latitude and longitude. The backend requires the three values together, accepts Google Maps hosts only, and validates the coordinates against Jordan's geographic bounds. Public salon pages present the city as a separate, readable link that opens the saved map location without allowing it to collide with the salon name.

Salon cover images and employee portfolio media use a backend `MediaStorageService` abstraction implemented with Cloudinary's authenticated upload API. Cloudinary credentials remain backend environment variables. React never receives the API secret. Device files are selected through native image/video inputs, previewed locally, validated again by the server, uploaded to Cloudinary, and persisted as HTTPS delivery URLs plus provider public IDs. Public image delivery adds automatic format, quality, crop, and size transformations. Replacing a salon cover removes the previous managed Cloudinary asset; deleting employee media requires that the authenticated employee owns the database record.

The public employee directory uses a dedicated privacy-safe DTO. It may show name, specialty, career skills, experience, professional biography, and assigned public services. It does not return employee email, phone number, or private schedule data. Active employees may publish photos or supported videos only for their current accepted salon. Public gallery reads are paginated and ordered newest first.

| Method | Endpoint | Authentication | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/media/salon-cover` | Authenticated salon owner/request owner | Upload and replace the owned salon cover from a multipart device file |
| `POST` | `/api/media/salon-cover/drive` | Authenticated salon owner/request owner | Import a publicly shared Google Drive media URL as the owned salon cover |
| `POST` | `/api/media/work` | Active employee | Publish a multipart work photo or video to the employee's current salon |
| `POST` | `/api/media/work/drive` | Active employee | Import publicly shared Drive work media into Cloudinary |
| `GET` | `/api/media/salons/{salonId}/work` | Public | Return the accepted salon's paginated public work gallery |
| `DELETE` | `/api/media/work/{mediaId}` | Publishing employee | Remove an owned gallery record and its managed Cloudinary asset |

## Product goals

- Give users a secure account and a clear role in the platform.
- Allow eligible users to request ownership of a salon profile.
- Require administrative review before a salon becomes active.
- Help salon owners recruit and manage employees.
- Allow users to request employment or respond to salon invitations.
- Preserve employment state when an employee transfers between salons.
- Publish structured hiring posts and route applications to the correct salon owner.
- Let customers request appointments and track owner decisions.
- Help owners assign qualified available employees and complete appointments.
- Publish trustworthy salon feedback based on completed visits.
- Build a reliable foundation for payments, notifications, and deployment.

## Roles and responsibilities

| Role | Purpose | Current permissions |
| --- | --- | --- |
| USER | Default registered account and potential employee | Manage one profile, discover salons and services, request appointments, track or cancel owned bookings, review completed visits, enable career mode, and apply to hiring posts |
| OWNER | User approved as the owner of a salon | Manage the owned salon, services, employee capabilities and schedules, booking decisions and assignments, invitations, employment requests, leave requests, and hiring posts |
| Employee | User currently assigned to a salon | View assigned bookings, participate in employment and leave workflows, and transfer after the required workflow |
| ADMIN | Platform administrator | Sign in to the dashboard, review salon requests, accept or reject salon requests, manage user directories, and reversibly block or unblock non-admin users and salons |

Role names currently follow the source enum exactly: `USER`, `OWNER`, `ADMIN`, and `Employee`. A future normalization may standardize the casing, but that change requires migration planning.

### Stakeholder responsibilities

| Stakeholder | Primary concern | Decision responsibility |
| --- | --- | --- |
| Product owner | Correct business behavior and release priority | Approves workflows, edge cases, and scope changes |
| Backend engineering | Reliable domain logic and API contracts | Implements reviewed behavior and technical controls |
| Frontend engineering | Clear user journeys and valid client integration | Uses documented DTOs, authentication, and route contracts |
| Quality assurance | Repeatable evidence and regression protection | Verifies acceptance criteria, authorization, and failure paths |
| Operations | Deployability, observability, recovery, and secrets | Owns environment configuration and production runbooks |

## Functional requirements

| ID | Requirement | Current state |
| --- | --- | --- |
| FR-01 | A visitor can register a unique validated account, verify email ownership, recover a forgotten password, and authenticate securely | Implemented |
| FR-02 | A user has one profile that can be upgraded with career information | Implemented |
| FR-03 | A user can request a salon and an administrator governs activation | Implemented |
| FR-04 | Owners and users can create and decide employment relationships through controlled workflows | Implemented |
| FR-05 | Owners can publish vacancies and receive every application ranked by transparent fit rules | Implemented |
| FR-06 | Administrators can reversibly restrict non-admin users and salons | Implemented |
| FR-07 | Customers can discover active salon services | Implemented |
| FR-08 | Customers can request future appointments for active services within salon and employee availability | Implemented |
| FR-09 | Owners can decide bookings, assign qualified employees, and complete accepted appointments | Implemented |
| FR-10 | Customers can publish a verified salon review after a completed owned booking | Implemented |
| FR-11 | The platform can notify participants about time-sensitive workflow changes | Planned |
| FR-12 | Production operators can deploy, monitor, migrate, back up, and recover the system | Planned |

## Technology stack

| Layer | Technology |
| --- | --- |
| Web client | React 19, React Router 7, Vite 8 |
| Client HTTP and feedback | Axios, React Toastify |
| Client styling and icons | Tailwind CSS 4 foundation, project CSS, Lucide React |
| Client tests and quality | Vitest, Testing Library, ESLint, Prettier |
| Language | Java 17 |
| Framework | Spring Boot 4.1.0 |
| HTTP API | Spring Web MVC |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA and Hibernate |
| Production database target | MySQL |
| Local database | Persistent H2 with MySQL compatibility mode |
| Test database | Isolated H2 in-memory database with MySQL compatibility mode |
| Validation | Spring Validation plus service-level business validation |
| Email foundation | Spring Mail with SMTP configuration |
| Build | Maven |
| Utilities | Lombok |

## Application architecture

The product uses a feature-oriented React client connected to a layered Spring API.

```text
React route
  -> feature page
      -> feature hook
          -> API adapter
              -> shared Axios client
                  -> Controller
      -> Service interface
          -> Service implementation
              -> Repository
                  -> MySQL database
```

Controllers define HTTP routes and receive authenticated identity information. DTOs define API input and output contracts. Service implementations contain validation and business rules. Repositories provide database access through Spring Data JPA. Entities represent persisted domain state.

The main Java package is `com.salon.backend` and contains:

- `Config` for Spring Security, password hashing, and JWT filtering.
- `Controllers` for authentication, dashboard, salons, salon services, employees, bookings, reviews, employment, hiring posts, and users.
- `DTOs` for request and response contracts.
- `Entities` for users, salons, services, employee capabilities and schedules, bookings, reviews, employment, leave requests, and hiring posts.
- `Repositories` for JPA database access.
- `Services` for authentication, administration, salon management, salon-service management, employee directories and availability, bookings, reviews, employment, and hiring-post rules.

All service responses use the generic `ApiResponse<T>` envelope containing `success`, `message`, and `data`. The frontend API adapters return the response body, `requireData` accepts successful envelopes and throws the server message for unsuccessful envelopes, and feature hooks convert those outcomes into page state and consistent toast feedback.

### Architectural responsibilities

| Layer | Owns | Must not own |
| --- | --- | --- |
| Controller | Route mapping, request binding, authenticated principal extraction | Domain decisions or direct repository orchestration |
| DTO | External request and response shape | Persistence behavior or service dependencies |
| Service | Validation, authorization, state transitions, transactions | HTTP rendering concerns |
| Repository | Query intent, locking, and persistence access | Product policy |
| Entity | Persisted state and relationships | Client-facing response contracts |

### Key architecture decisions

- JWT subjects use immutable database user IDs; email remains available through the authenticated principal for compatible routes.
- DTOs are required at external boundaries to prevent password and persistence-graph exposure.
- Business outcomes continue through `ApiResponse.success(...)` and `ApiResponse.error(...)` under the approved API convention.
- Pessimistic write locks and entity versioning protect sensitive hiring, booking, transfer, moderation, and one-review-per-booking transitions.
- H2 in MySQL compatibility mode isolates automated tests from developer and production databases.
- Secrets enter through environment variables and never through committed production values.

## Frontend application

The frontend is a React 19 single-page application built by Vite. It is organized by feature so pages, hooks, models, API adapters, and feature-specific components remain close to the workflow they implement. App-level routing and layout compose those features, while `shared` owns the HTTP client, authentication storage, reusable query behavior, notifications, and common components.

### Frontend structure

| Area | Responsibility | Implemented examples |
| --- | --- | --- |
| `app` | Route composition, layout, loading, not-found, and render-error boundaries | Lazy routes, `ProtectedRoute`, navbar, toast container |
| `features/auth` | Registration, email verification, login, logout, password recovery, and session entry | Forms, auth hooks, public recovery routes, `/auth/*` adapters |
| `features/salons` | Public discovery, details, salon request, and owner profile | Home cards, salon details, create salon, my salon |
| `features/services` | Public catalog and owner service management | List, create, edit, deactivate |
| `features/profile` | Unified profile and career-mode upgrade | Career form with resume and portfolio validation |
| `features/hiring` and `applications` | Vacancy discovery, application, owner decision, and applicant history | Ranked posts, apply flow, decision screens |
| `features/employees` | Owner workforce directory and employee setup | Service capabilities and weekly schedules |
| `features/bookings` | Customer request, owner queue, employee view, and status monitoring | Qualified employee filtering, lifecycle actions, 30-second polling |
| `features/reviews` | Verified salon ratings and comments | Review modal, public list, summary, own deletion |
| `features/media` | Reusable device/Drive selection, Cloudinary delivery optimization, salon covers, and employee work gallery | Connected to authenticated backend media routes |
| `shared` | Cross-feature transport, auth storage, feedback, loading, and UI primitives | Axios interceptors, `requireData`, error normalization, toast helpers, persistent loader state, branded overlays |

Dependencies flow from `app` to `features` to `shared`. Pages use hooks rather than importing transport code directly. Pure model functions remain independent of React and HTTP; for example, booking employee filtering checks that one employee supports every selected service.

### Visual design system

The application uses an editorial salon identity built around deep plum, warm gold, soft rose surfaces, and high-contrast neutral text. The same semantic color roles drive the navigation, cards, forms, dashboard, feedback components, and documentation. Components consume theme variables instead of feature-specific hard-coded colors so light and dark modes remain visually consistent.

| Token | Light value | Dark value | Use |
| --- | --- | --- | --- |
| Primary | `#76546F` | `#C58EBA` | Brand actions, active navigation, focus and diagram emphasis |
| Primary strong | `#5E4058` | `#D7A8CE` | Hover emphasis, headings, high-priority controls |
| Primary soft | `#F1E8EF` | Derived dark plum surface | Selected states, subtle panels, loader aura |
| Accent gold | `#D7A86E` | `#E1B978` | Highlights, status accents, premium visual details |
| Page background | `#FAF7F4` | `#141216` | Main application canvas |
| Card surface | `#FFFFFF` | `#211D23` | Cards, dialogs, forms, dashboard panels |
| Main text | `#292329` | `#F8F3F6` | Readable foreground content |
| Muted text | `#746B72` | `#B9AFB6` | Secondary labels and supporting copy |
| Border | `#E8DEDF` | `#423942` | Dividers, input outlines, non-destructive hover feedback |

Hover states preserve the card's dark surface instead of replacing it with a light fill. They communicate interaction through a stronger border, restrained background modulation, shadow, and motion so text remains readable. Normal pages show the Salon Hup wordmark once in the header; repeated full-brand blocks were removed from authentication content and the footer. Startup and route-transition states may show the brand because normal page chrome is not yet available.

### Loading and transition experience

Two coordinated loading patterns clarify what the application is doing:

- **Signature Snip action loader:** used for API operations such as signing in, registering, searching, booking, deciding a request, applying to a post, changing staff or services, and updating notifications. Animated scissors open and close with a small gold sparkle over a softened page backdrop. The message describes the current action instead of displaying a generic spinner.
- **Salon Hup navigation loader:** used for initial application startup, lazy route loading, redirects, link navigation, programmatic navigation, and browser back or forward movement. It uses the primary brand mark and a destination-aware message without duplicating the header wordmark.

The shared loader store counts overlapping operations so one completed request cannot hide another request that is still running. Action overlays appear after 180 ms to avoid flicker and remain visible for at least 320 ms once shown. Route transitions remain visible for approximately 650 ms to make the navigation feel intentional. Background availability refresh and notification polling do not take over the screen. Both loaders respect light/dark tokens and reduced-motion preferences.

### Error presentation

The backend `ApiResponse.error(...)` factory trims supplied messages and guarantees a clear fallback when a caller provides a blank or null reason. On the client, one normalizer prioritizes the backend `message`, validation `errors`, or legacy `error` field and then maps timeouts, network failures, and HTTP status failures to understandable text. Axios interceptors preserve those details; feature hooks render them through the common toast or page-error components. A generic failure is used only when neither the server nor transport provides a useful reason.

### Frontend routes and user journeys

| Browser route | Access | Page purpose | Primary API integration |
| --- | --- | --- | --- |
| `/` | Public | Active salon discovery | `GET /api/salons/active` |
| `/salons/:salonId` | Public | Salon profile, services, ratings, and reviews | Salon, service, and review public endpoints |
| `/login`, `/register` | Public | Account access and verified account creation | Auth login and registration |
| `/verify-email` | Public | Verify a new account or request a replacement code | Email verification and resend endpoints |
| `/forgot-password`, `/verify-reset-code`, `/reset-password` | Public | Request, authorize, and complete password recovery | Password recovery endpoints |
| `/salons/:salonId/book` | Authenticated | Select services, qualified employee, future date, and contact details | Salon, services, employees, `POST /api/bookings` |
| `/my-bookings` | Authenticated customer | Track, cancel, and review appointments; poll decisions every 30 seconds while open | Customer booking and review endpoints |
| `/hiring-posts` | Authenticated | Browse filtered and personalized vacancies | `GET /api/hiring-posts` |
| `/hiring-posts/:postId/apply` | Authenticated career user | Submit the optional application note | Hiring application endpoint |
| `/profile/career` | Authenticated | Enable or update the career section on the unified profile | Profile GET and career PUT |
| `/my-applications` | Authenticated applicant | View permanent application history and decisions | Applicant-history endpoint |
| `/owner/dashboard` | OWNER only | Owner workspace with salon, employee, customer, booking, revenue, rating, and employee-performance summaries | Owner dashboard and scoped salon APIs |
| `/owner/create-salon`, `/owner/my-salon` | Authenticated owner candidate/owner | Request and manage an owned salon | Salon create, mine, edit, delete |
| `/owner/create-hiring-post`, `/owner/applications` | Authenticated owner | Publish vacancies and rank or decide applicants | Hiring owner endpoints |
| `/owner/employees`, `/owner/employees/:employeeId/setup` | Authenticated owner | Manage active staff, capabilities, and weekly working periods | Salon employee endpoints |
| `/owner/services`, `/owner/create-service`, `/owner/services/:serviceId/edit` | Authenticated owner | Manage the salon catalog | Salon service endpoints |
| `/owner/bookings` | Authenticated owner | Decide, assign, reassign with explanation, and complete bookings | Owner booking endpoints |
| `/employee/bookings` | Authenticated employee | View appointments assigned to the signed-in employee | Employee booking endpoint |

`ProtectedRoute` checks for a stored session and redirects unauthenticated visitors to login while preserving the attempted location. It improves navigation, while Spring Security and service authorization remain the authoritative access controls.

### Frontend and backend connection

[[DIAGRAM:frontend-integration]]

The browser uses `VITE_API_BASE_URL`, defaulting to `/api`. During local development, Vite proxies `/api` to `VITE_API_PROXY_TARGET`, defaulting to `http://localhost:8080`. In production, the built client can use the same-origin `/api` reverse proxy or an explicitly configured HTTPS API origin.

| Concern | Frontend behavior | Backend behavior |
| --- | --- | --- |
| Authentication | Keeps verification and recovery calls public, stores the current JWT contract, and adds `Authorization: Bearer <token>` outside auth routes | Verifies account email before login, validates JWT, resolves immutable user ID, checks active state and credential-change time, and applies roles |
| Request lifecycle | Uses a 15-second timeout and abort signals for stale page requests | Binds DTOs, derives caller identity from the security principal |
| Success handling | Reads `ApiResponse.data`; feature hooks update UI and show success feedback | Returns `ApiResponse.success(message, data)` |
| Business failure | Shared normalization preserves `message`, validation `errors`, and legacy `error`; hooks show the result in a toast or page state | Returns `ApiResponse.error(message)` with `data: null`; blank reasons receive a safe fallback |
| Slow foreground action | Begins a contextual Signature Snip loading operation and keeps concurrent work visible | Completes or fails the requested transaction and returns the standard envelope |
| Route transition | Shows the branded navigation overlay for links, redirects, programmatic navigation, and history changes | Protected resources still enforce authentication and role authorization |
| Unauthorized session | Clears matching current credentials on HTTP 401; HTTP 403 does not sign the user out | Security may reject unauthenticated or unauthorized calls before the controller |
| Booking decisions | Polls customer bookings every 30 seconds only while the page is visible and reports accepted or rejected transitions | Persists booking state and owner note; returns scoped booking DTOs |
| Notification inbox | Connected inbox, bell, polling, unread count, and read controls | Persists recipient-scoped booking events and exposes paginated inbox, unread-count, and read-state APIs |
| Mapping differences | Salon and hiring adapters normalize backend field names for views | Keeps stable DTO names and pagination metadata |

### Runtime links and environment

- [Live API Test](http://localhost:8080/swagger-ui/index.html) - local Swagger UI address. It becomes usable when the backend is running and the Springdoc/OpenAPI dependency is enabled. The route is allowed by security, but the current Maven dependencies do not yet provide Swagger UI.
- Frontend local URL: `http://localhost:5173` when started with Vite's default development server.
- Backend local base URL: `http://localhost:8080/api`.

| Variable | Default | Use |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `/api` | Browser API prefix; safe public configuration, never a secret |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Local Vite proxy target |
| Backend environment variables | Defined in backend `.env.example` | Database, JWT, SMTP, and runtime configuration |

### Integration coverage and gaps

| Capability | Frontend | Backend | Connection state |
| --- | --- | --- | --- |
| Auth and unified career profile | Registration, verification, login, recovery, and profile pages implemented | Verification codes, reset authorization, JWT invalidation, and unified profile implemented | Connected end to end |
| Public salons and service catalogs | Implemented | Implemented | Connected |
| Salon request and owner management | Implemented | Implemented | Connected |
| Hiring discovery, application, and decisions | Implemented | Implemented | Connected for current UI actions; edit, reopen, and cancel-join APIs have no current page control |
| Employee directory, capabilities, and schedules | Implemented | Implemented | Connected |
| Booking creation and customer, owner, employee views | Implemented | Implemented | Connected |
| Booking assignment explanation | Implemented | Implemented | Connected; explanation is required when replacing an assigned employee |
| Persistent notification inbox | Implemented | Implemented | Connected with signed-in polling and refresh when the application becomes active |
| Server-calculated booking availability | Implemented | Implemented | Connected; the booking page renders qualified employees and selectable 15-minute slots |
| Customer-approved booking rescheduling | Implemented | Implemented | Connected; owners propose server-available slots and customers accept or keep the original appointment |
| Verified salon reviews | Implemented | Implemented | Connected |
| Direct employment, leave, transfer | No dedicated current pages | Implemented | Backend ready; frontend journey pending |
| Administration dashboard | No dedicated current pages | Implemented | Backend ready; frontend journey pending |
| Salon media | Device/Drive picker, previews, owner cover editor, public gallery, employee publisher | Cloudinary storage abstraction, signed server uploads, asset metadata and deletion | Connected; requires Cloudinary environment credentials |
| Swagger interactive testing | Link and allowed security path | Dependency absent | Enable Springdoc/OpenAPI before using Live API Test |

[[PAGEBREAK]]

## Current application flow

This portrait-oriented flowchart shows the completed customer, salon-owner, career, and employee journeys in the web application. Cross-lane arrows show the handoffs between a customer and salon staff. Breaks are handled as needed rather than scheduled; admin workflows are backend-only in the current frontend.

[[DIAGRAM:application-flow]]

[[PAGEBREAK]]

## System diagrams

The following diagrams present the product from business, runtime, interaction, and value perspectives. Solid flows represent implemented behavior. Labels that include `planned` identify capabilities outside the current release.

### System context

This view shows the people and external systems that exchange data with Salon Hup. The browser application uses the backend API as the single business gateway; the API owns authorization and persistence decisions.

[[DIAGRAM:system-context]]

[[PAGEBREAK]]

### Use case view

The use case view separates customer, career user, salon owner, employee, and administrator responsibilities. A person may move between roles through approved salon and employment workflows.

[[DIAGRAM:use-cases]]

### Software component architecture

Requests cross explicit boundaries from the React feature adapter to Spring Security, thin controllers, domain services, repositories, and the database. DTOs and the `ApiResponse<T>` envelope protect the API boundary.

[[DIAGRAM:components]]

### Frontend action, error, and navigation feedback

The following view shows how routes, API operations, backend response messages, and the two loading presentations cooperate without placing workflow-specific behavior in page components.

[[DIAGRAM:frontend-feedback]]

[[PAGEBREAK]]

### Account verification and password recovery sequence

The authentication sequence shows verified registration, server-generated short-lived codes, resend throttling, one-time reset authorization, BCrypt password replacement, and invalidation of JWTs issued before the credential change. Codes and reset tokens are never persisted in plaintext.

[[DIAGRAM:auth-sequence]]

[[PAGEBREAK]]

### Hiring application sequence

The hiring sequence shows how authenticated profile data replaces repeated applicant entry and how acceptance delegates to the existing employment workflow.

[[DIAGRAM:hiring-sequence]]

[[PAGEBREAK]]

### Salon service sequence

The service sequence shows owner authorization, validation, persistence, public filtering, and reversible disable behavior.

[[DIAGRAM:service-sequence]]

[[PAGEBREAK]]

### Employee qualification and appointment-slot decision

This decision flow shows how the server creates selectable appointment times from active services, total duration, salon hours, qualified employees, weekly schedules, accepted bookings, and the same-day lead rule. The same checks run again when the customer submits the booking.

[[DIAGRAM:availability-flow]]

[[PAGEBREAK]]

### Appointment booking sequence

The booking sequence shows trusted profile data, active-service validation, employee qualification and schedule checks, owner decisions, assignment, and completion.

[[DIAGRAM:booking-sequence]]

[[PAGEBREAK]]

### Booking lifecycle

The booking lifecycle shows the permitted owner and customer transitions and the terminal states that cannot return to an earlier stage.

[[DIAGRAM:booking-lifecycle]]

[[PAGEBREAK]]

### Booking reschedule sequence

The reschedule sequence shows the owner-only availability lookup, durable proposal, customer decision, acceptance-time capacity revalidation, and atomic appointment and notification update. A rejected proposal preserves the original appointment.

[[DIAGRAM:reschedule-sequence]]

[[PAGEBREAK]]

### Booking notification sequence

The notification sequence shows the atomic booking-and-notification write, server-owned recipient selection, idempotent event key, and authenticated polling and read flow. A failed booking transition creates no event, and a notification failure rolls back the enclosing booking transaction.

[[DIAGRAM:notification-sequence]]

[[PAGEBREAK]]

### Verified salon review sequence

The review sequence shows how a completed owned booking authorizes one customer review and how public summaries include reviews from currently active customers.

[[DIAGRAM:review-sequence]]

### Stakeholder benefits

The platform creates value by connecting discovery, operational control, career mobility, and platform governance through one consistent domain model.

[[DIAGRAM:benefits]]

## Core domain model

### User

A user stores identity and account information: first name, last name, email, username, BCrypt password hash, phone number, role, status, and an optional salon assignment. The same entity also stores an optional career section containing career-mode status, date of birth, years of experience, skills, preferred work locations, résumé URL, portfolio URL, and professional biography.

The product exposes one unified profile. Registration fields appear automatically in it. Career mode enriches that profile instead of creating a second account or profile. Career skills and preferred locations use collection tables so users may provide multiple values.

`emailVerified` prevents newly registered accounts from signing in before ownership is confirmed. `credentialsUpdatedAt` records the latest password change and invalidates earlier JWTs while preserving the stateless authentication model. Legacy accounts with a null verification flag remain compatible and are treated as verified.

User status values are `Active`, `Blocked`, and `Deleted`.

### Authentication verification code

The authentication verification record stores the normalized email, purpose, hashed code, expiry, failed-attempt count, consumption time, and optional hashed reset-token authorization. Verification codes expire after 10 minutes, resend is throttled for 60 seconds, five failed attempts consume the code, and reset authorization is one-time and short lived. Raw codes and reset tokens are delivered to the user but are never stored in the database.

### Salon

A salon stores its name, salon phone, owner phone, email, location, description, owner, employees, current and maximum employee counts, creation date, opening and closing times, profile picture URL, approval status, and opening status. Reversible administration also stores the status that existed before a block, independent owner-block and direct admin-block flags, and the owner's role before a direct salon block.

Salon approval status values are `Inactive`, `Requested`, `Accepted`, `Rejected`, and `DELETED`.

### Salon service

A salon service belongs to one salon and stores a customer-facing name, optional description, decimal price in Jordanian dinars, duration in minutes, active state, and creation and update timestamps. The public catalog returns only active services from an accepted, unblocked salon. The owner catalog includes both active and inactive records.

Disabling a service is a soft state change. The row remains available for later reactivation and for the booking history that will reference it. Only the authenticated owner of an accepted, unblocked salon may create, edit, activate, or disable its services.

### Employee service capability

An employee service capability links one active salon employee to one active service in the same salon. The authenticated owner replaces the employee's capability set through one request. The service validates ownership, employee membership, service membership, active states, and duplicate IDs before replacing the persisted links.

Capabilities determine which employees appear as suitable for a customer's selected services and which employees an owner may assign to a booking. One assigned employee must support every service item in that booking. Capability links are removed when an accepted leave or transfer removes the employee from the salon.

### Employee schedule and working period

An employee schedule belongs to one employee and contains zero or more weekly working periods. Each period stores a day of week, start time, and end time. Multiple non-overlapping periods can describe split working shifts. These periods describe scheduled work and do not define or schedule personal breaks.

The owner may configure up to 28 periods per employee. Every period must have a valid day and time range, remain inside the salon's opening hours, and avoid overlap with another period on the same day. Adjacent periods are valid. An employee without a saved schedule inherits the salon opening hours for backward compatibility; an explicitly saved empty schedule makes that employee unavailable for the entire week. Schedule records are removed when an accepted leave or transfer removes the employee from the salon.

### Booking and service snapshot

A booking links the authenticated customer to one accepted, unblocked salon, an optional assigned employee, one or more service-item snapshots, and its reschedule-proposal history. It stores the current date and time, customer contact and location, customer note, owner note, total price, total duration, status, decision time, completion time, cancellation time, and audit timestamps.

Each booking service item copies the source service ID, name, price, and duration at booking creation. Later salon-service edits therefore do not rewrite historical booking totals or labels. Booking status values are `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED`, and `COMPLETED`.

An employee cannot hold two overlapping `ACCEPTED` bookings on the same date. Acceptance and accepted-booking reassignment lock the selected employee, compare the complete appointment interval against existing accepted appointments, and allow directly adjacent appointments. Pending requests remain visible and do not reserve capacity before the owner accepts them.

### Booking reschedule proposal

A reschedule proposal belongs to one booking and stores the proposing owner, required proposed date and time, optional note, status, creation time, and response time. Status values are `PENDING`, `ACCEPTED`, `REJECTED`, and `CANCELLED`. Only one pending proposal may exist per booking, while all decided proposals remain visible as history.

The current appointment and booking status remain unchanged while a proposal is pending. Accepting the proposal revalidates the complete appointment against current salon hours, employee capability and schedule, accepted-booking conflicts, the 15-minute grid, and the 30-minute same-day notice before changing the booking date and time. Rejecting keeps the original appointment. A terminal booking transition cancels any pending proposal.

### User notification

A user notification belongs to exactly one recipient and one booking. It stores the event type, message, unique event key, salon ID and name snapshots, booking date and time, optional proposed date and time, booking-status snapshot, frontend target path, creation time, and optional read time. Supported booking event types include acceptance, rejection, reschedule proposal and decision, employee reassignment, and cancellation.

The recipient is resolved from trusted booking relationships rather than accepted from the client. Acceptance, rejection, suggested-time, and reassignment events go to the customer. A customer cancellation notifies the salon owner and the assigned employee when present. Every publisher method requires an active transaction, so the booking transition and its notifications commit or roll back together. Deterministic event keys plus a database uniqueness constraint make retried actions idempotent.

### Salon review

A salon review links exactly one completed booking to its salon and customer. It stores an integer rating from one through five, an optional comment of up to 2000 characters, and its creation time. A database uniqueness constraint on the booking relationship enforces one review per booking in addition to service validation.

Public review lists and aggregate summaries are available only for accepted, unblocked salons and include reviews whose customer is currently active. Blocking a customer hides that customer's reviews without deleting them; unblocking restores their visibility. A customer may list and delete only their own reviews.

### Employment request

An employment request links a salon, sender, receiver, optional hiring post, status, request type, source, creation time, and optional rejection reason.

Request types are `Owner_Invite` and `User_Request`. Sources are `SALON_PROFILE` and `HIRING_POST`.

Employment request statuses are `Requested`, `Accepted_Pending_Leave`, `Accepted`, `Rejected`, and `Cancelled`.

### Leave request

A leave request records the employee, current salon, optional target salon, optional employment request, status, reason, and creation time. It supports both a normal departure and a transfer connected to an accepted employment request.

Leave request statuses are `Requested`, `Accepted`, and `Cancelled`.

### Hiring post

A hiring post belongs to one salon and stores:

- Title and detailed description.
- Required skills in a related collection table.
- Minimum and maximum applicant age.
- Required employee location.
- Minimum years of experience.
- Number of available positions.
- `Open`, `Closed`, or `Expired` status.
- A closure reason that distinguishes manual closure from closure caused by filled positions.
- Creation and expiration timestamps.

The backend obtains salon and owner details from the authenticated owner and database. The client cannot choose another owner or salon while creating a post. A version field and transactional write locks protect concurrent lifecycle and position updates.

### Hiring application

A hiring application links one applicant, hiring post, and employment request. It stores a submission-time snapshot of the applicant's date of birth, preferred work locations, years of experience, skills, optional résumé URL, optional portfolio URL, application note, and creation time.

Known identity fields including name, username, email, phone number, role, and user ID come from the authenticated user and are not accepted from the client. The linked employment request provides the application status and allows acceptance to reuse the existing employment and transfer workflows.

## Domain state model

### Primary state transitions

| Aggregate | From | Trigger | To | Restorable |
| --- | --- | --- | --- | --- |
| User | Active | Administrator blocks a non-admin account | Blocked | Yes, to Active |
| Salon | Requested | Administrator accepts request | Accepted | No automatic rollback |
| Salon | Requested | Administrator rejects with reason | Rejected | Yes, through correction and resubmission |
| Salon | Accepted, Requested, or Rejected | Direct administrator salon block | Inactive | Yes, to exact prior state |
| Salon service | Active | Owner disables service | Inactive | Yes, through owner edit |
| Booking | PENDING | Owner accepts valid request | ACCEPTED | No; customer may cancel |
| Booking | PENDING | Owner rejects with required note | REJECTED | No |
| Booking | PENDING or ACCEPTED | Customer cancels owned booking | CANCELLED | No |
| Booking | ACCEPTED | Owner completes appointment | COMPLETED | No |
| Hiring post | Open | Positions filled or owner closes post | Closed | Conditional reopen |
| Hiring post | Open | Expiration time passes | Expired | Yes, with future date and valid positions |
| Employment request | Requested | Receiver accepts available applicant | Accepted | Employment ends through leave workflow |
| Employment request | Requested | Receiver accepts applicant employed elsewhere | Accepted_Pending_Leave | Can cancel pending join |
| Leave request | Requested | Current owner accepts | Accepted | No automatic rollback |

### Invariants

- A user may apply to a specific hiring post only once.
- A hiring application never changes its career-data snapshot after submission.
- A salon must be `Accepted` for active employment and hiring operations.
- A public salon service must be active and belong to an accepted, unblocked salon.
- Only the authenticated salon owner may mutate that salon's service catalog.
- Disabling a salon service preserves its database identity for reactivation and future booking history.
- A booking contains at least one and at most ten unique active services from its salon.
- A requested appointment must be in the future and its complete duration must fit inside salon opening hours.
- An assigned employee must be active in the booking salon, support every selected service, and work for the complete appointment interval.
- An employee cannot be assigned to overlapping accepted appointment intervals; directly adjacent appointments remain valid.
- Replacing an already assigned employee requires a nonblank owner explanation of at most 1000 characters.
- Notification recipients always come from authenticated or booking-owned server data; inbox reads and read-state updates are restricted to that recipient.
- A logical booking event has one unique notification event key, and its persistence shares the booking transaction.
- A missing employee schedule inherits salon hours; a saved empty schedule represents no availability.
- Only a pending booking can be accepted or rejected, only a pending or accepted owned booking can be cancelled, and only an accepted booking can be completed.
- Booking service names, prices, and durations are immutable snapshots of the selected services at request time.
- Only the customer who owns a completed booking may create its single salon review.
- Review ratings are integers from one through five; comments are optional and limited to 2000 characters.
- A hiring post must be open, unexpired, and have a remaining position before application or acceptance.
- Employee counts and reserved positions change in the same transaction as the related employment decision.
- A direct salon block and an owner-derived block are independent; both must be removed before restoration.
- Direct salon unblocking restores the saved owner role and exact prior salon status.
- Deleted entities are not restored through block and unblock operations.

## Implemented business workflows

### Registration and login

Registration validates names, username format, supported email domains, Jordanian phone formats, password strength, and uniqueness of username, email, and phone number. New accounts receive the `USER` role and `Active` status, are marked unverified, and receive a six-digit email code. Passwords are stored as BCrypt hashes. The registration response tells the frontend that verification is required, that the code lasts 10 minutes, and that resend becomes available after 60 seconds.

Email verification compares a secret-backed hash, limits failed attempts, consumes the code after success, and marks the account verified. Login rejects unverified accounts, verifies account status and password, then returns a bearer JWT valid for 24 hours. Logout blacklists the presented token in application memory until its expiration time.

Forgot-password requests return the same success response whether or not an account exists, preventing account enumeration. A valid six-digit recovery code produces a one-time reset token. Reset requires matching new and confirmation passwords, applies the shared password policy, replaces the BCrypt hash, consumes the authorization, and records `credentialsUpdatedAt` so older JWTs stop working immediately.

### End-to-end user journey

1. A visitor registers with first name, last name, username, email, phone number, and password.
2. Registration validates identity fields, uniqueness, supported formats, and password strength, then creates an active but email-unverified `USER` account and sends a short-lived verification code.
3. The user verifies the registered email, then logs in and receives a JWT whose subject is the immutable database user ID. The authenticated principal also exposes email and role without accepting identity from the client.
4. `GET /api/profile` returns one unified profile populated with the identity information already collected during registration.
5. The user may keep the normal profile or select the client action represented by "Make your profile career."
6. `PUT /api/profile/career` validates and stores the career section, then enables career mode on the same profile.
7. The hiring feed loads open, unexpired posts from accepted salons with available positions. Career users receive personalized labels and ordering; normal-profile users receive the standard filtered feed plus a prompt to enable career mode.
8. A career user applies with only an optional application note. The backend obtains identity and career data from the database and stores an application snapshot.

### Owner dashboard journey

An authenticated owner opens `/owner/dashboard`. The route guard requires an active session and the `OWNER` role before rendering the page; backend authorization remains authoritative for every requested resource. The dashboard resolves the owner's salon from authenticated identity and composes scoped salon data, employees and their service capabilities, customers with completed bookings, booking revenue values, verified salon ratings, and operational totals. Employee performance combines completed booking count, services delivered, and received ratings so the owner can identify strong performance without exposing another salon's data. Non-owners are redirected away from the workspace and cannot obtain its protected data through direct API calls.

### Shared interaction-feedback journey

When a user starts a foreground operation, the relevant feature hook supplies an action-specific message to the shared loading service. The Signature Snip overlay appears only when the operation lasts beyond the short delay, keeps the underlying page visually present but de-emphasized, and remains active until all overlapping operations finish. On success, the page updates and a success message may be shown. On failure, the client displays the exact useful backend reason from `ApiResponse`; transport-only failures receive a clear network, timeout, authentication, authorization, not-found, or server message. Navigation uses the separate Salon Hup brand loader and covers declarative links, redirects, programmatic navigation, and browser history movement.
9. The salon owner reviews every application ordered by match level and score, then accepts or rejects it.
10. Acceptance reuses employment rules: an available applicant joins immediately, while an existing employee enters the transfer and leave workflow.

### End-to-end appointment journey

1. A customer opens an accepted salon and receives its active services and active employees with their service capabilities.
2. The customer selects one to ten unique services, a future date and time, and optionally an employee qualified for every selected service.
3. The backend calculates immutable price and duration totals, checks salon hours, and checks the selected employee's complete working interval.
4. The booking is stored as `PENDING`, and the customer sees it in personal booking history.
5. The salon owner receives the request in the pending queue, may assign a qualified available employee, and accepts or rejects the request.
6. Acceptance repeats capability and schedule validation. Rejection stores a required owner note and may include a suggested time.
7. The assigned employee sees the booking in the employee-scoped list. The customer may cancel while it remains pending or accepted.
8. After the appointment, the owner marks an accepted booking `COMPLETED`.
9. The customer may create one verified salon review linked to that completed booking.
10. Public salon pages may display visible reviews and the aggregate average while preserving administrative block and restore behavior.

### Unified profile and career mode

Every authenticated user has one profile. Basic identity fields come from registration and are never re-entered to activate career mode. Career activation requires date of birth, nonnegative years of experience, at least one skill, and at least one preferred work location. Skills and locations are trimmed and deduplicated case-insensitively.

Résumé, portfolio, and professional biography are optional. When supplied, a résumé must be an absolute HTTP or HTTPS URL whose path ends in `.pdf`, while a portfolio must be an absolute HTTP or HTTPS URL with a valid host. The service stores links only and does not fetch remote content. Profile completion assigns 80 percent to the four required career sections and the remaining 20 percent to résumé, portfolio, and biography.

The same career endpoint activates a normal profile and updates an existing career profile. Career mode becomes active only after all required career information passes validation.

### Salon onboarding

1. An authenticated user submits salon information.
2. The service validates the salon name, Jordanian city, description, email, phone number, opening hours, and employee capacity.
3. The salon is created with `Requested` status.
4. An administrator reviews the request.
5. Acceptance changes the salon to `Accepted` and promotes its user to `OWNER`.
6. Rejection requires a reason, stores it on the salon, and changes the salon to `Rejected`.
7. The owner can view the reason, correct the rejected salon details, and explicitly resubmit the same request.
8. Each resubmission clears the previous reason and returns the salon to `Requested`. A salon may be resubmitted at most three times.

Only the salon owner can update or resubmit the owned salon through the service ownership check. Editing alone does not resubmit a rejected salon. A rejected owner cannot create a second salon request to bypass the resubmission limit. Acceptance, rejection, and resubmission use transactional write locks.

### Reversible user and salon blocking

Dashboard administrators can block or unblock non-admin users and salons. Administrators cannot block an account whose role is `ADMIN`; approval by the future application owner is planned before administrator-to-administrator blocking is introduced. Deleted users and salons are outside this workflow.

Blocking a normal user or employee changes the account status from `Active` to `Blocked`. The JWT filter reloads account state for every authenticated request, so both new login attempts and already-issued tokens stop granting access while the account remains blocked. Unblocking returns the account to `Active` without deleting profile, application, or employment history.

Blocking a salon directly is different from blocking its owner. The owner account remains active, but the owner's current role is saved and temporarily changed to `USER`. The salon's current approval status is saved and changed to `Inactive`. Hiring posts, applications, employment links, and other related records remain persisted but become unavailable through the accepted-salon business checks. Unblocking restores the exact previous salon status and owner role, making records that were valid before the block usable again.

Blocking a salon owner blocks the owner account and makes the owned salon inactive while preserving the owner's role. Direct salon blocks and owner-derived blocks are tracked independently. If both restrictions apply, removing only one keeps the salon inactive; the salon is restored only after both restrictions are removed. Transactional pessimistic locks protect these state transitions.

### Salon service catalog

An authenticated owner whose salon is accepted and unblocked can create a service with a required name, positive price, and positive duration. Description is optional. Names are trimmed to 100 characters, descriptions to 1000 characters, and prices use at most two decimal places within the persisted precision.

The owner management endpoint returns active and inactive services so disabled entries remain editable and may be reactivated. Update requests can change the name, description, price, duration, and active state. The delete route performs a soft disable and returns the updated DTO rather than deleting the row.

Public clients can list the active services of a salon without authentication. The service layer first verifies that the salon is accepted and unaffected by either administrator or owner-derived blocking, then queries only active services ordered by name. Response DTOs expose the service contract without serializing the salon entity graph.

### Employee directory and capabilities

Authenticated clients can list active employees for an accepted, unblocked salon. The salon owner can list the same scoped team through an owner route. Employee responses include identity details appropriate for discovery plus the service IDs and names the employee can perform.

The salon owner replaces an employee's service capabilities using service IDs. All selected services must be active and belong to the owner's salon. These capabilities are enforced again by booking creation, acceptance, and assignment, so a manipulated client cannot select or assign an unqualified employee.

### Weekly employee schedules and ad hoc absences

The owner can read or replace a current employee's weekly schedule. A schedule accepts multiple working periods per day. Gaps between periods are outside scheduled working hours; they are not configured break times. An employee may also become unavailable unexpectedly, for example to eat, smoke, or leave. The owner can assign the affected appointment to another qualified employee and add an update for the customer. Validation rejects missing values, reversed ranges, periods outside salon opening hours, overlaps, and more than 28 periods.

When no employee schedule has ever been configured, booking validation treats the salon's opening and closing times as the employee's working period. Once an empty schedule is explicitly saved, the employee is unavailable all week. This distinction preserves existing employee behavior while allowing owners to disable appointment availability intentionally.

### Appointment availability, request, and decision

An authenticated customer first requests availability using a salon ID, one to ten unique service IDs, a date, and an optional employee ID. The backend calculates the total duration, finds employees who support every selected service, loads their weekly schedules and accepted bookings in batches, and returns selectable times at 15-minute intervals. A same-day slot must start at least 30 minutes after the current server time. Pending requests do not reserve capacity.

When no employee is selected, a slot is returned when at least one fully qualified employee is free, and the response lists every eligible employee for that time. When an employee is selected, only that employee's availability is considered. An employee without a configured schedule inherits the salon hours; an explicitly empty schedule means unavailable all week.

The customer creates the booking using one returned time plus the salon, services, location, optional phone override, optional note, and optional employee ID. The backend loads the customer and salon, verifies every service is active and belongs to that salon, defaults the phone number from the profile when omitted, calculates price and duration, and saves service snapshots. It revalidates the exact slot before persistence so elapsed time, a newly accepted appointment, or a manipulated request cannot create an unavailable booking.

If the customer selects an employee, that employee must be active in the salon, support every selected service, and work for the complete appointment interval. The complete interval must also fit within salon opening hours. The owner may accept a pending request, reject it with a required note, or assign a qualified available employee to a pending or accepted booking. Replacing an employee who is already assigned requires an owner explanation, which is stored as the customer-visible owner note. Acceptance and assignment repeat capability and schedule validation because availability may have changed since creation.

Customers receive their own booking history and can cancel an owned pending or accepted booking. Salon owners receive all owned-salon bookings or a pending-only queue. Employees receive only bookings assigned to them. The owner completes an accepted booking after the visit. Business outcomes continue through the shared `ApiResponse` envelope.

### Customer-approved booking rescheduling

The salon owner can propose a new date and time for a future `PENDING` or `ACCEPTED` booking. Reschedule availability derives the salon, service snapshots, assigned employee, duration, and booking to exclude from trusted persisted data; the client submits only the date for availability and the selected date, time, and optional note for the proposal.

The proposal does not reserve the new time, and the original booking remains unchanged. The customer may accept or reject only a proposal attached to the customer's booking. Acceptance locks both proposal and booking, revalidates availability while excluding the booking's current accepted interval, updates the date and time, preserves the booking status, and notifies the salon owner plus an assigned employee for an accepted booking. Rejection preserves the original appointment and notifies the owner. Retried identical decisions return the existing outcome without duplicate notifications.

### Verified salon reviews

After a booking reaches `COMPLETED`, its customer may submit one salon rating from one through five and an optional comment. The service locks and reloads the booking, verifies active customer identity, booking ownership, completed status, active salon state, and absence of an existing review before saving it.

Anyone may read the review list and aggregate average for an active salon. Authenticated customers may list their own reviews and delete only a review they own. Public queries filter by the review customer's current active status, so administration blocking temporarily removes that content and unblocking restores it without data loss.

### Direct employment request

A `USER` may request to join an accepted salon with available capacity. The system prevents duplicate pending requests for the same user and salon. The salon owner becomes the receiver of the request.

When the owner accepts and the user has no current salon, the user is assigned to the salon, the role becomes `Employee`, the salon employee count increases, and the request becomes `Accepted`.

### Owner invitation

The owner of an accepted salon may invite another user when the salon has capacity. The system prevents self-invitations and duplicate pending invitations or requests. The invited user may accept or reject the invitation, and the owner may cancel an invitation that the owner sent.

### Leave and transfer

An employee can create a leave request for the current salon. The current salon owner accepts the request. For a normal leave, the employee is removed from the salon, the role returns to `USER`, and the salon employee count decreases.

If a user who already works at a salon accepts another employment opportunity, the employment request becomes `Accepted_Pending_Leave` and a linked leave request is created. Approval by the current salon owner removes the employee from the current salon and adds the employee to the target salon in one transaction.

### Hiring-post creation

Only an authenticated `OWNER` can create a hiring post. The owner must have an associated salon with `Accepted` status. The service validates all owner-entered requirements, removes blank and duplicate skills, assigns the current time, sets the status to `Open`, and stores the post for the owner's salon.

The response automatically includes the salon ID, salon name, salon location, owner ID, and owner name from persisted data.

### Hiring-post discovery and personalization

Authenticated users browse open posts through a paginated endpoint. The query may filter by keyword, required employee location, required skill, and maximum required experience. Page size is limited to 50. Supported secondary orders are newest, expiring soon, and required experience ascending.

The feed excludes closed or expired posts, posts without remaining positions, and posts belonging to salons that are not accepted. For a user with career mode enabled, the backend calculates a transparent match for every eligible candidate before pagination and orders results by match level, score, and the requested secondary order:

1. `SUGGESTED` means every requirement matches and the user has both more experience than required and at least one additional skill.
2. `MATCHED` means every required skill, experience, age, and location condition matches.
3. `MISSING_REQUIREMENTS` remains visible and describes every missing skill or unmet condition.

Required skills contribute up to 50 points, experience 25, age 10, preferred-location compatibility 10, additional skills up to 5, and an optional résumé 5, capped at 100. A location matches when the post's required employee location equals any saved preferred work location, case-insensitively.

### Hiring application and ranking

Any authenticated `USER` or `Employee` with a complete career profile can apply to an open, unexpired hiring post unless already employed by that salon or already registered as an applicant for that post. Applications are never rejected because the applicant does not match the post requirements.

The client submits only the application-specific note. Date of birth, years of experience, skills, preferred work locations, résumé URL, portfolio URL, and identity fields are loaded from the authenticated user's saved profile. The application stores a snapshot so later profile edits do not rewrite historical submissions.

The owner receives every application ordered by match level and score:

1. `SUGGESTED` applications meet every requirement, exceed the experience requirement, and provide additional skills.
2. `MATCHED` applications meet every required skill, experience, age, and location condition.
3. `MISSING_REQUIREMENTS` applications remain visible and include exact missing skills and unmet conditions.

The match score is transparent and reproducible. Required skills contribute up to 50 points, experience up to 25, age 10, location 10, additional skills up to 5, and an optional résumé adds a 5-point bonus. The final score is capped at 100. The résumé is encouraged but may be omitted or stored as `null` without preventing submission.

### Hiring application decision

Only the owner of the salon connected to a hiring post can accept or reject its applications. Rejection requires a reason and reuses the existing employment-request rejection workflow.

Acceptance reuses the employment-request acceptance workflow. An applicant without a current salon joins immediately. An applicant already employed elsewhere enters `Accepted_Pending_Leave`, and the existing transfer leave request is created. Acceptance reserves one advertised position. A post closes when no positions remain. If a pending transfer is cancelled, the reserved position is restored and an unexpired post reopens when appropriate.

### Hiring-post lifecycle management

The salon owner can list all owned hiring posts, inspect one post with application counts, edit allowed fields, close a post manually, and reopen it. Requirements remain editable until the first application exists. After an application exists, the owner may change only the title, description, expiration date, and number of positions so the requirements used for ranking historical applicants remain stable.

Reopening requires a future expiration date. A closed or expired post can reopen with its restored positions; when no position is available, the owner must add at least one. Manual closure remains closed when a position is later restored. A post closed because all positions were filled automatically reopens after a pending hiring transfer is cancelled when it is still unexpired.

A scheduled service marks open posts as expired when their expiration time passes. Request-time checks also expire stale posts before an operation continues, so correctness does not depend only on the scheduler.

Applicants can view their permanent application history. An application itself cannot be deleted or cancelled and a rejected or cancelled applicant cannot apply again to the same post. When an accepted application is waiting for the applicant to leave a current salon, the applicant may cancel the linked join request. This cancels the employment transfer and restores the reserved position. Fully accepted employment uses the normal leave workflow instead.

### Authenticated employment identity

Employment and leave endpoints never accept a current-user ID from request parameters. The JWT subject stores the immutable user ID, the authentication filter loads that user by primary key, and controllers receive a typed authenticated principal. Service authorization compares the authenticated ID with the request sender, receiver, employee, or salon owner as required.

Employment and leave endpoints return dedicated response DTOs instead of exposing JPA entities. The DTOs contain the request, salon, participant, hiring-post, status, reason, and timestamp fields needed by clients without exposing entity graphs or account secrets.

## Hiring-post creation contract

### Owner-provided fields

| Field | Rule |
| --- | --- |
| title | Required, at least 3 characters |
| description | Required, at least 10 characters |
| requiredSkills | At least one nonblank skill; duplicate values are removed |
| minimumAge | Required and at least 18 |
| maximumAge | Required and not lower than minimum age |
| employeeLocation | Required |
| minimumYearsOfExperience | Required and zero or greater |
| numOfPositions | Required and greater than zero |
| expiresAt | Required and later than the current time |

### Automatically generated fields

| Field | Source |
| --- | --- |
| salon | Loaded using the authenticated owner's email |
| owner identity | Loaded from the authenticated account and salon relationship |
| status | Set to `Open` |
| createdAt | Set by the backend at creation time |

## Hiring application contract

### Applicant-provided field

| Field | Rule |
| --- | --- |
| applicationNote | Optional, maximum 2000 characters |

### Profile-provided application snapshot

| Field | Rule |
| --- | --- |
| dateOfBirth | Loaded from the career profile; user must be at least 18 |
| preferredWorkLocations | Loaded from the career profile; at least one is required |
| yearsOfExperience | Loaded from the career profile; zero or greater |
| skills | Loaded from the career profile; at least one is required |
| resumeUrl | Optional HTTP/HTTPS PDF URL; adds a ranking bonus |
| portfolioUrl | Optional valid HTTP/HTTPS URL |

### Ranking output

| Field | Purpose |
| --- | --- |
| matchLevel | Classifies the application as `SUGGESTED`, `MATCHED`, or `MISSING_REQUIREMENTS` |
| matchScore | Orders applications within each level from highest to lowest |
| matchedSkills | Required skills supplied by the applicant |
| additionalSkills | Applicant skills beyond the post requirements |
| missingSkills | Required skills absent from the application |
| missingRequirements | Human-readable explanation of every unmet condition |
| resumeUploaded | Shows whether the optional résumé bonus applies |

[[PAGEBREAK]]

## Employee availability contract

### Service capability update

| Field | Rule |
| --- | --- |
| employeeId | Path value; employee must be active and assigned to the authenticated owner's salon |
| serviceIds | Required collection of unique IDs; an empty list clears capabilities |
| each service | Must be active and belong to the same salon as the employee |

### Weekly schedule update

| Field | Rule |
| --- | --- |
| workingPeriods | Required collection; empty means unavailable all week; maximum 28 periods |
| dayOfWeek | Required Java day-of-week value |
| startTime | Required and earlier than end time |
| endTime | Required and no later than salon closing time |
| same-day periods | Must not overlap; adjacent periods are accepted; gaps are outside scheduled working periods |

## Booking contract

### Customer-provided booking fields

| Field | Rule |
| --- | --- |
| salonId | Required; salon must be accepted and unblocked |
| serviceIds | One to ten unique active services belonging to the selected salon |
| assignedEmployeeId | Optional; employee must support every selected service and be available for the full interval |
| bookingDate and bookingTime | Required server-offered appointment start; 15-minute grid, full duration inside salon hours, and at least 30 minutes of notice on the same day |
| customerPhoneNumber | Optional override; otherwise loaded from the authenticated profile; maximum 30 characters |
| customerLocation | Required; maximum 255 characters |
| note | Optional; maximum 1000 characters |

### Backend-generated booking fields

| Field | Source |
| --- | --- |
| customer | Authenticated user ID from the JWT principal |
| service snapshots | Active service records loaded from the selected salon |
| totalPrice | Sum of selected service prices |
| totalDurationMinutes | Sum of selected service durations |
| status | Set to `PENDING` at creation |
| timestamps | Set during creation and every accepted state transition |

### Availability request and response

| Field | Rule |
| --- | --- |
| salonId | Required; salon must be accepted, open, and unblocked |
| serviceIds | One to ten unique active services from the selected salon |
| bookingDate | Required and not before the current server date |
| employeeId | Optional; when supplied, the employee must be active in the salon and capable of every selected service |
| totalDurationMinutes | Calculated from current service durations |
| slotIntervalMinutes | Server-owned interval; currently 15 minutes |
| minimumLeadMinutes | Server-owned same-day notice; currently 30 minutes |
| slots[].time | A start time whose complete duration fits salon and employee availability |
| slots[].eligibleEmployees | Qualified active employees without an accepted overlap at that time |

### Owner booking action fields

| Field | Rule |
| --- | --- |
| assignedEmployeeId | Optional during acceptance and a required path value during explicit assignment; employee must pass salon, capability, and schedule checks |
| ownerNote | Optional for first assignment and acceptance; required when replacing an already assigned employee; maximum 1000 characters |

### Reschedule proposal contract

| Field | Rule |
| --- | --- |
| bookingDate | Availability request date; required and not in the past |
| proposedDate | Required and must produce a future server-available slot |
| proposedTime | Required 15-minute slot returned by the reschedule availability endpoint |
| note | Optional customer message; maximum 1000 characters |
| current appointment | Remains unchanged while the proposal is pending or after rejection |
| acceptance | Revalidates capacity and updates the appointment while preserving booking status |
| history | Every proposal remains in the booking response with status and timestamps |

## Salon review contract

| Field | Rule |
| --- | --- |
| bookingId | Required; booking must belong to the authenticated customer and be `COMPLETED` |
| rating | Required integer from 1 through 5 |
| comment | Optional trimmed text; maximum 2000 characters |
| uniqueness | One salon review per booking, protected by service validation and a database constraint |
| visibility | Public only while the salon is active and the reviewing customer is active |

## API catalog

All routes except explicitly public authentication routes require a valid bearer token under the current security configuration.

### Response convention

Every service response uses the existing envelope:

```text
{
  "success": true or false,
  "message": "Human-readable outcome",
  "data": object, array, scalar, or null
}
```

The current product decision is to represent business outcomes through `ApiResponse.success(...)` and `ApiResponse.error(...)`. New work must preserve this contract and must not introduce a competing centralized exception handler or independent HTTP-status strategy without a separately approved migration.

### Access matrix

| API group | Public | Authenticated user | OWNER | Employee | ADMIN |
| --- | --- | --- | --- | --- | --- |
| Registration and login | Yes | Yes | Yes | Yes | Dashboard login only |
| Unified profile | No | Own profile | Own profile | Own profile | Own profile |
| Active salon and service discovery | Yes | Yes | Yes | Yes | Yes |
| Salon ownership actions | No | Request only | Owned salon | No | Approval and moderation |
| Salon service management | No | No | Accepted owned salon | No | No special access |
| Employee discovery | No | Active salon team | Owned salon team | Active salon team | Active salon team |
| Employee capabilities and schedules | No | No | Owned salon employees | No | No special access |
| Booking | No | Own bookings and actions | Owned salon decisions | Assigned bookings | No special access |
| Notification inbox | No | Own notifications only | Own notifications only | Own notifications only | Own notifications only |
| Salon reviews | Public salon list and summary | Own creation, list, and deletion | Public read | Public read | Public read |
| Employment and leave | No | Applicable requests | Owned salon decisions | Applicable requests | No special access |
| Hiring discovery and application | No | Yes | Owner cannot apply as owner | Yes | No special access |
| Hiring-post management | No | No | Owned salon only | No | No special access |
| User directory and dashboard moderation | No | No | No | No | Yes |

### Authentication

| Method | Route | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | Create a user account |
| POST | `/api/auth/rigester` | Backward-compatible alias for the original registration route |
| POST | `/api/auth/login` | Authenticate and receive a JWT |
| POST | `/api/auth/email/verify` | Verify a registered email with its current six-digit code |
| POST | `/api/auth/email/resend` | Invalidate the previous code and send a replacement after the cooldown |
| POST | `/api/auth/password/forgot` | Request a password-recovery code without exposing account existence |
| POST | `/api/auth/password/verify-code` | Exchange a valid recovery code for a short-lived one-time reset token |
| POST | `/api/auth/password/reset` | Replace the password with a valid reset token and invalidate older JWTs |
| POST | `/api/auth/logout` | Blacklist the current JWT |

### Unified user profile

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/api/profile` | Return registration identity and the optional career section for the authenticated user |
| PUT | `/api/profile/career` | Enable career mode or update career information on the same profile |

### Salon management

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/api/salons` | Return all salons to an administrator |
| GET | `/api/salons/active` | Return accepted salons |
| GET | `/api/salons/{salonId}` | Return one salon profile |
| GET | `/api/salons/mine` | Return the authenticated owner's salon, rejection reason, and resubmission counters |
| POST | `/api/salons/create` | Request creation of a salon |
| PUT | `/api/salons/{salonId}/edit` | Update an owned salon |
| POST | `/api/salons/{salonId}/resubmit` | Resubmit a rejected owned salon, up to three times |
| DELETE | `/api/salons/{salonId}` | Soft-delete the authenticated owner's salon |

### Salon services

| Method | Route | Purpose | Access |
| --- | --- | --- | --- |
| POST | `/api/salon-services` | Create an active service for the authenticated owner's salon | Accepted salon owner |
| GET | `/api/salon-services/salon/{salonId}` | List active services for an accepted, unblocked salon | Public |
| GET | `/api/salon-services/my-salon` | List active and inactive services for management | Accepted salon owner |
| PUT | `/api/salon-services/{serviceId}` | Update and optionally reactivate an owned service | Accepted salon owner |
| DELETE | `/api/salon-services/{serviceId}` | Disable an owned service without deleting history | Accepted salon owner |

### Salon employees, capabilities, and schedules

| Method | Route | Purpose | Access |
| --- | --- | --- | --- |
| GET | `/api/salon-employees/salon/{salonId}` | List privacy-safe active employee profiles and service capabilities for an accepted, unblocked salon | Public |
| GET | `/api/salon-employees/my-salon` | List the authenticated owner's active employees | Accepted salon owner |
| GET | `/api/salon-employees/{employeeId}/services` | Read one owned employee's current service capabilities | Accepted salon owner |
| PUT | `/api/salon-employees/{employeeId}/services` | Replace one owned employee's validated service-capability set | Accepted salon owner |
| GET | `/api/salon-employees/{employeeId}/schedule` | Read configured weekly periods or inherited salon hours | Accepted salon owner |
| PUT | `/api/salon-employees/{employeeId}/schedule` | Replace one owned employee's weekly working periods | Accepted salon owner |

### Appointment bookings

| Method | Route | Purpose | Access |
| --- | --- | --- | --- |
| POST | `/api/bookings/availability` | Calculate available 15-minute appointment starts and eligible employees for selected services and date | Authenticated user |
| POST | `/api/bookings` | Create a validated future appointment request with service snapshots | Authenticated customer |
| GET | `/api/bookings/my` | Return the authenticated customer's booking history | Booking customer |
| GET | `/api/bookings/my-salon` | Return all bookings for the authenticated owner's salon | Accepted salon owner |
| GET | `/api/bookings/my-salon/pending` | Return the owner's pending queue ordered by appointment time | Accepted salon owner |
| GET | `/api/bookings/employee/my` | Return bookings assigned to the authenticated employee | Active employee |
| PUT | `/api/bookings/{bookingId}/accept` | Accept a pending booking after current capability and availability checks | Booking salon owner |
| PUT | `/api/bookings/{bookingId}/reject` | Reject a pending booking with a required owner note | Booking salon owner |
| PUT | `/api/bookings/{bookingId}/cancel` | Cancel an owned pending or accepted booking | Booking customer |
| PUT | `/api/bookings/{bookingId}/complete` | Complete an accepted appointment | Booking salon owner |
| PUT | `/api/bookings/{bookingId}/assign-employee/{employeeId}` | Assign a qualified available employee; reassignment requires an explanatory `ownerNote` body | Booking salon owner |
| POST | `/api/bookings/{bookingId}/reschedule-availability` | Calculate trusted availability for moving this booking while excluding its current interval | Booking salon owner |
| POST | `/api/bookings/{bookingId}/reschedule-proposals` | Create one pending date-and-time proposal | Booking salon owner |
| PUT | `/api/bookings/{bookingId}/reschedule-proposals/{proposalId}/accept` | Accept an owned proposal after current availability revalidation | Booking customer |
| PUT | `/api/bookings/{bookingId}/reschedule-proposals/{proposalId}/reject` | Reject an owned proposal and preserve the original appointment | Booking customer |

### Notification inbox

| Method | Route | Purpose | Access |
| --- | --- | --- | --- |
| GET | `/api/notifications` | List the authenticated user's notifications newest first with pagination | Authenticated recipient |
| GET | `/api/notifications/unread-count` | Return the authenticated user's unread notification count | Authenticated recipient |
| PUT | `/api/notifications/{notificationId}/read` | Mark one owned notification as read idempotently | Notification recipient |
| PUT | `/api/notifications/read-all` | Mark all unread notifications owned by the authenticated user as read | Authenticated recipient |

### Verified salon reviews

| Method | Route | Purpose | Access |
| --- | --- | --- | --- |
| POST | `/api/reviews` | Create one salon review for an owned completed booking | Booking customer |
| GET | `/api/reviews/salon/{salonId}` | List visible reviews newest first for an active salon | Public |
| GET | `/api/reviews/salon/{salonId}/summary` | Return average rating and total visible reviews | Public |
| GET | `/api/reviews/my` | Return the authenticated customer's reviews | Review owner |
| DELETE | `/api/reviews/{reviewId}` | Delete a review owned by the authenticated customer | Review owner |

### Employment and leave

| Method | Route | Purpose |
| --- | --- | --- |
| POST | `/api/employment/join` | Request employment at a salon |
| POST | `/api/employment/invite/{salonId}` | Invite a user to a salon |
| POST | `/api/employment/accept` | Accept an employment request or invitation |
| POST | `/api/employment/reject` | Reject a request or invitation |
| POST | `/api/employment/cancel` | Cancel a sent employment request |
| POST | `/api/employment/leave/request` | Request departure from the current salon |
| POST | `/api/employment/leave/accept/{requestId}` | Accept a leave request |
| POST | `/api/employment/leave/cancel/{requestId}` | Cancel a leave request |
| POST | `/api/employment/invitation/accept-and-leave` | Accept an invitation and start a transfer |

The authenticated user for every route above comes from the JWT principal. No route accepts `currentUserId` from the client.

### Hiring

| Method | Route | Purpose | Status |
| --- | --- | --- | --- |
| POST | `/api/hiring-posts` | Create a hiring post for the authenticated owner's salon | Implemented |
| GET | `/api/hiring-posts` | Browse and filter open posts, with personalized ranking for career profiles | Implemented |
| POST | `/api/hiring-posts/{id}/applications` | Apply to a hiring post | Implemented |
| GET | `/api/hiring-posts/{id}/applications` | Owner reviews ranked post applications | Implemented |
| POST | `/api/hiring-posts/applications/{id}/accept` | Accept an applicant and execute employment rules | Implemented |
| POST | `/api/hiring-posts/applications/{id}/reject` | Reject an applicant with a reason | Implemented |
| GET | `/api/hiring-posts/owner` | List the authenticated owner's posts, optionally filtered by status | Implemented |
| GET | `/api/hiring-posts/owner/{id}` | Inspect an owned post and its application counts | Implemented |
| PUT | `/api/hiring-posts/owner/{id}` | Edit an owned post under lifecycle locking rules | Implemented |
| POST | `/api/hiring-posts/owner/{id}/close` | Close an owned post manually | Implemented |
| POST | `/api/hiring-posts/owner/{id}/reopen` | Reopen a closed or expired post with a future expiration | Implemented |
| GET | `/api/hiring-posts/applications/me` | Return the authenticated applicant's permanent history | Implemented |
| POST | `/api/hiring-posts/applications/{id}/cancel-join` | Cancel a linked pending transfer and restore its reserved position | Implemented |

### Administration and users

| Method | Route | Purpose |
| --- | --- | --- |
| POST | `/api/dashboard/login` | Authenticate an administrator |
| GET | `/api/dashboard/salons/requested` | Review requested salons |
| POST | `/api/dashboard/salons/requested/accept` | Accept a salon request by salon ID |
| POST | `/api/dashboard/salons/requested/reject` | Reject a salon request with a required persisted reason |
| POST | `/api/dashboard/users/{userId}/block` | Block a non-admin user and inactivate an owned salon |
| POST | `/api/dashboard/users/{userId}/unblock` | Restore a blocked user and remove the owner-derived salon block |
| POST | `/api/dashboard/salons/{salonId}/block` | Inactivate a salon and temporarily return its active owner role to `USER` |
| POST | `/api/dashboard/salons/{salonId}/unblock` | Remove the direct salon block and restore its prior status and owner role |
| GET | `/api/users` | Return users to an administrator |
| GET | `/api/users/active` | Return active users to an administrator |
| GET | `/api/users/blocked` | Return blocked users to an administrator |

## Complete API contract reference

This reference lists every implemented controller route. `Body` names JSON fields, `path` and `query` identify URL inputs, and `none` means the caller sends no application data beyond authentication. Every success value below is placed in `ApiResponse.data`.

For controller and business validation failures, the API uses `{ "success": false, "message": "...", "data": null }`. The frontend reads this envelope with `requireData` and displays the server `message` in an error toast or page error. Success uses `{ "success": true, "message": "...", "data": ... }`. Spring Security failures before the controller use the same error envelope and `application/json`: unauthenticated protected requests return HTTP 401 with `Your session is missing or expired. Please sign in and try again.`, and authenticated requests lacking the required role return HTTP 403 with `Your account does not have permission to perform this action.` The registered `ApiAuthenticationEntryPoint` and `ApiAccessDeniedHandler` serialize `ApiResponse.error(...)` using the application's Jackson mapper. Endpoint permissions and roles remain unchanged. Axios displays the server message; only a 401 matching the current token clears the saved session, while a 403 preserves it.

### Authentication and profile contracts

| Endpoint | Access | Request data | Success data | Main errors shown in `message` |
| --- | --- | --- | --- | --- |
| `POST /api/auth/register` | Public | Body: `firstName`, `lastName`, `userName`, `email`, `phoneNumber`, `password` | Registered user identity plus `emailVerificationRequired`, `verificationCodeExpiresInSeconds`, and `resendAvailableInSeconds` | Required or duplicate identity data, invalid values |
| `POST /api/auth/rigester` | Public legacy alias | Same registration body | Same registration response | Same registration errors |
| `POST /api/auth/login` | Public | Body: `email`, `password` | `id`, `username`, `token`, `userRole`, `email` | Invalid credentials, unverified email, or blocked account |
| `POST /api/auth/email/verify` | Public | Body: `email`, six-digit `code` | Verification confirmation | Missing/invalid input, expired or consumed code, attempt limit reached |
| `POST /api/auth/email/resend` | Public | Body: `email` | Generic delivery confirmation | Missing email or active 60-second cooldown |
| `POST /api/auth/password/forgot` | Public | Body: `email` | Generic response regardless of account existence | Missing/invalid email or active cooldown for an existing account |
| `POST /api/auth/password/verify-code` | Public | Body: `email`, six-digit `code` | `resetToken`, `expiresInSeconds` | Invalid, expired, consumed, or attempt-limited code |
| `POST /api/auth/password/reset` | Public | Body: `resetToken`, `newPassword`, `confirmPassword` | Password-reset confirmation | Invalid/expired/used token, mismatched passwords, or password-policy failure |
| `POST /api/auth/logout` | Authenticated | Bearer token; body is ignored | `null` with logout message | Invalid/expired token is still treated as logout |
| `GET /api/profile` | Authenticated user | None | Unified identity plus career-mode profile DTO | User/profile not found or blocked session |
| `PUT /api/profile/career` | Authenticated user | Body: `dateOfBirth`, `yearsOfExperience`, `skills[]`, `preferredWorkLocations[]`, optional PDF `resumeUrl`, optional HTTP(S) `portfolioUrl`, `professionalBio` | Updated unified profile DTO with career mode enabled | Invalid age, experience, URLs, resume type, or profile data |

### Salon and administration contracts

| Endpoint | Access | Request data | Success data | Main errors shown in `message` |
| --- | --- | --- | --- | --- |
| `GET /api/salons` | ADMIN | None | List of all salon DTOs | Administrator authorization failure |
| `GET /api/salons/active` | Public | None | Accepted and active salon DTO list | Empty list is valid |
| `GET /api/salons/{salonId}` | Public | Path: `salonId` | Salon DTO | Salon missing or unavailable |
| `GET /api/salons/mine` | Authenticated owner | None | Owned salon, status, rejection reason, resubmission count | No owned salon |
| `POST /api/salons/create` | Authenticated user | Body: salon name, description, phone, email, city, employee limit, opening/closing time, profile picture URL; legacy `ownerId` is ignored and the JWT principal is authoritative | Requested salon DTO | Existing ownership/request, invalid hours or fields |
| `PUT /api/salons/{salonId}/edit` | Salon owner | Path: `salonId`; body: editable salon fields | Updated salon DTO | Not owner, salon missing, invalid fields/status |
| `POST /api/salons/{salonId}/resubmit` | Rejected salon owner | Path: `salonId` | Requested salon DTO with updated counters | Not rejected, not owner, or three-resubmission limit reached |
| `DELETE /api/salons/{salonId}` | Salon owner | Path: `salonId` | Confirmation string/null | Not owner or salon missing |
| `POST /api/dashboard/login` | Public admin login | Body: `adminEmail`, `adminPassword` | JWT token | Invalid credentials, non-admin, or blocked admin |
| `GET /api/dashboard/salons/requested` | ADMIN | None | Requested salon DTO list | Administrator authorization failure |
| `POST /api/dashboard/salons/requested/accept` | ADMIN | Body: `salonId` | Accepted salon DTO | Missing salon or invalid current status |
| `POST /api/dashboard/salons/requested/reject` | ADMIN | Body: `salonId`, `rejectionReason` | Rejected salon DTO | Missing reason, salon, or invalid status |
| `POST /api/dashboard/users/{userId}/block` | ADMIN | Path: `userId` | Confirmation string | User missing, already blocked, or target is an admin |
| `POST /api/dashboard/users/{userId}/unblock` | ADMIN | Path: `userId` | Confirmation string | User missing or not blocked |
| `POST /api/dashboard/salons/{salonId}/block` | ADMIN | Path: `salonId` | Confirmation string | Salon missing or already blocked |
| `POST /api/dashboard/salons/{salonId}/unblock` | ADMIN | Path: `salonId` | Confirmation string | Salon missing or no direct block to restore |
| `GET /api/users` | ADMIN | None | All user DTOs | Administrator authorization failure |
| `GET /api/users/active` | ADMIN | None | Active user DTOs | Administrator authorization failure |
| `GET /api/users/blocked` | ADMIN | None | Blocked user DTOs | Administrator authorization failure |

### Service and employee contracts

| Endpoint | Access | Request data | Success data | Main errors shown in `message` |
| --- | --- | --- | --- | --- |
| `POST /api/salon-services` | Accepted salon owner | Body: `name`, optional `description`, `price`, `durationMinutes` | Service DTO with ID and active state | No accepted salon, duplicate/invalid fields |
| `GET /api/salon-services/salon/{salonId}` | Public | Path: `salonId` | Active service DTO list | Salon missing, blocked, or not accepted |
| `GET /api/salon-services/my-salon` | Accepted salon owner | None | Owned service DTO list including inactive items | No accepted owned salon |
| `PUT /api/salon-services/{serviceId}` | Accepted salon owner | Path: `serviceId`; body: name, description, price, duration, optional `isActive` | Updated service DTO | Not owner, missing service, invalid values |
| `DELETE /api/salon-services/{serviceId}` | Accepted salon owner | Path: `serviceId` | Disabled service DTO/message | Not owner, missing service, already inactive |
| `GET /api/salon-employees/salon/{salonId}` | Public | Path: `salonId` | Privacy-safe employee DTO list with career and service details | Salon unavailable |
| `GET /api/salon-employees/my-salon` | Accepted salon owner | None | Owned active employee DTO list | No accepted owned salon |
| `GET /api/salon-employees/{employeeId}/services` | Accepted salon owner | Path: `employeeId` | Employee capability DTO | Employee not in owned salon |
| `PUT /api/salon-employees/{employeeId}/services` | Accepted salon owner | Path: `employeeId`; body: `serviceIds[]` | Replaced capability DTO | Employee/service mismatch, inactive service, duplicates |
| `GET /api/salon-employees/{employeeId}/schedule` | Accepted salon owner | Path: `employeeId` | Weekly periods plus inherited-hours indicator | Employee not in owned salon |
| `PUT /api/salon-employees/{employeeId}/schedule` | Accepted salon owner | Path: `employeeId`; body: `periods[{dayOfWeek,startTime,endTime}]`; multiple periods can describe split working shifts | Validated weekly schedule DTO | More than 28 periods, overlap, invalid interval, outside salon hours |

### Booking, notification, and review contracts

| Endpoint | Access | Request data | Success data | Main errors shown in `message` |
| --- | --- | --- | --- | --- |
| `POST /api/bookings/availability` | Authenticated user | Body: `salonId`, `serviceIds[]`, `bookingDate`, optional `employeeId` | Total duration, 15-minute interval, 30-minute lead, and ordered slots with eligible employee summaries | Inactive salon/service, duplicate or excessive services, past date, selected employee not fully qualified |
| `POST /api/bookings` | Authenticated customer | Body: `salonId`, `serviceIds[]`, optional `assignedEmployeeId`, `customerPhoneNumber`, `customerLocation`, `bookingDate`, `bookingTime`, optional `note` | Pending booking DTO with service price/duration snapshots | Past or off-grid time, stale/unavailable slot, unavailable salon/employee, unsupported service, schedule conflict |
| `GET /api/bookings/my` | Booking customer | None | Customer booking DTO list | Authentication failure; empty list is valid |
| `GET /api/bookings/my-salon` | Accepted salon owner | None | All owned-salon booking DTOs | No accepted owned salon |
| `GET /api/bookings/my-salon/pending` | Accepted salon owner | None | Pending booking DTOs ordered by appointment | No accepted owned salon |
| `GET /api/bookings/employee/my` | Active employee | None | Assigned booking DTO list | Caller is not an active employee |
| `PUT /api/bookings/{bookingId}/accept` | Booking salon owner | Path: `bookingId`; optional body: `ownerNote`, `assignedEmployeeId` | Accepted booking DTO; customer notification committed atomically | Not owner, wrong state, pending reschedule decision, expired time, employee no longer eligible, overlapping accepted appointment |
| `PUT /api/bookings/{bookingId}/reject` | Booking salon owner | Path: `bookingId`; body: required `ownerNote` | Rejected booking DTO | Blank/long note, not owner, wrong state |
| `PUT /api/bookings/{bookingId}/cancel` | Booking customer | Path: `bookingId` | Cancelled booking DTO | Not customer or state cannot be cancelled |
| `PUT /api/bookings/{bookingId}/complete` | Booking salon owner | Path: `bookingId` | Completed booking DTO | Not owner, not accepted, or appointment not eligible for completion |
| `PUT /api/bookings/{bookingId}/assign-employee/{employeeId}` | Booking salon owner | Paths: booking and employee IDs; optional body `ownerNote`, required when replacing an assignment, max 1000 chars | Booking DTO with assigned employee and explanation | Not owner, employee unqualified/unavailable, conflict, missing reassignment explanation |
| `POST /api/bookings/{bookingId}/reschedule-availability` | Booking salon owner | Path: `bookingId`; body: required `bookingDate` | Duration, slot interval, lead time, and available slots derived from the booking | Not owner, terminal/past booking, invalid date, employee no longer qualified |
| `POST /api/bookings/{bookingId}/reschedule-proposals` | Booking salon owner | Path: `bookingId`; body: `proposedDate`, `proposedTime`, optional `note` | Pending proposal DTO; customer notification committed atomically | Not owner, duplicate pending proposal, unchanged or unavailable time, terminal/past booking |
| `PUT /api/bookings/{bookingId}/reschedule-proposals/{proposalId}/accept` | Booking customer | Booking and proposal path IDs | Accepted proposal DTO and updated booking appointment; owner/employee notifications | Not customer, wrong proposal state, booking no longer eligible, proposed slot no longer available |
| `PUT /api/bookings/{bookingId}/reschedule-proposals/{proposalId}/reject` | Booking customer | Booking and proposal path IDs | Rejected proposal DTO; original appointment unchanged | Not customer or proposal no longer pending |
| `GET /api/notifications` | Authenticated recipient | Query: optional zero-based `page` (default 0), `size` (default 20, maximum 100) | `content[]`, page, size, total elements/pages, first/last; each item contains IDs, event type, message, booking/salon snapshot, target path, timestamps, and `read` | Negative page, size outside 1-100, authentication failure |
| `GET /api/notifications/unread-count` | Authenticated recipient | None | `unreadCount` for the current user | Authentication failure |
| `PUT /api/notifications/{notificationId}/read` | Notification recipient | Path: `notificationId` | Updated notification DTO with `readAt` and `read=true` | Notification missing or owned by another user; the same non-revealing error is returned |
| `PUT /api/notifications/read-all` | Authenticated recipient | None | `unreadCount: 0` after the scoped bulk update | Authentication failure |
| `POST /api/reviews` | Completed-booking customer | Body: `bookingId`, integer `rating` 1-5, optional `comment` up to 2000 chars | Verified salon review DTO | Booking not owned/completed, duplicate review, invalid rating/comment |
| `GET /api/reviews/salon/{salonId}` | Public | Path: `salonId` | Visible reviews newest first | Salon unavailable; empty list is valid |
| `GET /api/reviews/salon/{salonId}/summary` | Public | Path: `salonId` | `averageRating`, `totalReviews` | Salon unavailable |
| `GET /api/reviews/my` | Authenticated customer | None | Customer's review DTO list | Authentication failure |
| `DELETE /api/reviews/{reviewId}` | Review owner | Path: `reviewId` | Confirmation string/null | Review missing or caller is not its owner |

### Employment and hiring contracts

| Endpoint | Access | Request data | Success data | Main errors shown in `message` |
| --- | --- | --- | --- | --- |
| `POST /api/employment/join` | Authenticated user | Body: `salonId` | Employment request DTO | Already employed/pending, salon unavailable/capacity reached |
| `POST /api/employment/invite/{salonId}` | Salon owner | Path: `salonId`; body: `userId` | Invitation DTO | Not owner, user unavailable, duplicate request, capacity issue |
| `POST /api/employment/accept` | Applicable receiver | Body: `requestId` | Accepted employment DTO | Wrong receiver/state, existing employment, capacity issue |
| `POST /api/employment/reject` | Applicable receiver | Body: request ID and rejection reason | Rejected employment DTO | Wrong receiver/state or blank reason |
| `POST /api/employment/cancel` | Request sender | Body: `requestId` | Cancelled employment DTO | Not sender or request no longer cancellable |
| `POST /api/employment/leave/request` | Active employee | Body: optional reason | Leave request DTO | No active employment or existing leave request |
| `POST /api/employment/leave/accept/{requestId}` | Salon owner | Path: `requestId` | Accepted leave DTO and ended employment | Not owner, missing request, wrong state |
| `POST /api/employment/leave/cancel/{requestId}` | Leave requester | Path: `requestId` | Cancelled leave DTO | Not requester or wrong state |
| `POST /api/employment/invitation/accept-and-leave` | Employed invitee | Body: `requestId`; optional query `reason` | Pending transfer employment DTO | Wrong invitee/state or missing current employment |
| `GET /api/hiring-posts` | Authenticated | Query: keyword, location, skill, maximum required experience, paging/sorting | Page containing post, match score/level, pagination, personalization message | Invalid paging/filter values |
| `POST /api/hiring-posts` | Accepted salon owner | Body: title, description, skills, age range, employee location, minimum experience, positions, future expiry | Hiring-post DTO | Not eligible owner, invalid range/date/positions/fields |
| `GET /api/hiring-posts/owner` | Accepted salon owner | Optional query: `status` | Owner post DTO list with application counts | No owned salon or invalid status |
| `GET /api/hiring-posts/owner/{hiringPostId}` | Post owner | Path: `hiringPostId` | Owned post plus application counts | Post missing or not owner |
| `PUT /api/hiring-posts/owner/{hiringPostId}` | Post owner | Path; body: title, description, expiry, `additionalPositions` | Updated hiring-post DTO | Locked field/state, invalid future date/positions, not owner |
| `POST /api/hiring-posts/owner/{hiringPostId}/close` | Post owner | Path: `hiringPostId` | Closed hiring-post DTO | Already closed or not owner |
| `POST /api/hiring-posts/owner/{hiringPostId}/reopen` | Post owner | Path; body: future `expiresAt` | Reopened hiring-post DTO | Not reopenable, no free position, invalid date |
| `POST /api/hiring-posts/{hiringPostId}/applications` | Authenticated career user | Path; body: optional `applicationNote` | Application snapshot with match analysis | Career mode incomplete, duplicate application, own post, closed/expired/full post |
| `GET /api/hiring-posts/{hiringPostId}/applications` | Post owner | Path: `hiringPostId` | All applications ranked suggested/matched/missing | Not owner or post missing |
| `POST /api/hiring-posts/applications/{applicationId}/accept` | Post owner | Path: `applicationId` | Accepted or pending-transfer application DTO | Wrong state, capacity/position issue, employment conflict |
| `POST /api/hiring-posts/applications/{applicationId}/reject` | Post owner | Path; body: `rejectionReason` | Rejected application DTO | Blank reason, wrong state, not owner |
| `GET /api/hiring-posts/applications/me` | Applicant | None | Permanent application-history DTO list | Authentication failure |
| `POST /api/hiring-posts/applications/{applicationId}/cancel-join` | Applicant | Path: `applicationId` | Cancelled linked join/transfer DTO and restored position | Not applicant, no cancellable linked request, wrong state |

### API usage example

```text
POST /api/reviews
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "bookingId": 42,
  "rating": 5,
  "comment": "Excellent service."
}
```

Successful response body:

```text
{
  "success": true,
  "message": "Review created successfully",
  "data": {
    "id": 9,
    "bookingId": 42,
    "salonId": 3,
    "rating": 5,
    "comment": "Excellent service."
  }
}
```

Business-error response body:

```text
{
  "success": false,
  "message": "Only completed bookings can be reviewed",
  "data": null
}
```

The frontend presents the error `message` to the user through the shared notification component. It must not replace useful server validation text with a generic failure unless no message exists.

## Security model

Spring Security runs without server sessions. A JWT uses the immutable database user ID as its subject and carries email as a claim. The filter validates the token signature and expiration, loads the user by primary key, verifies that the account is still active, rejects tokens issued before `credentialsUpdatedAt`, and places a typed `AuthenticatedUser` principal and its role in the security context. `getName()` remains the account email for existing controllers, while security-sensitive employment endpoints use the principal ID directly. Passwords use BCrypt.

The authentication filter does not log authorization headers or bearer-token values. Employment and leave controllers derive caller identity from `@AuthenticationPrincipal` and return response DTOs.

Public routes include login, registration, email verification and resend, the three password-recovery operations, dashboard login, API documentation routes, and selected search routes. The frontend never attaches a stored JWT to public authentication requests. Dashboard salon and user routes and every user-directory route require the `ADMIN` role. Blocked administrators cannot obtain a dashboard token. Other routes require authentication unless explicitly allowed.

Verification codes are six digits, stored only as a secret-backed hash, expire after 10 minutes, allow at most five failures, and are replaced on resend. Reset tokens are high-entropy, stored only as a hash, short lived, and consumed in the same transaction as the password change. Forgot-password responses are deliberately non-enumerating. SMTP sender identity uses `MAIL_FROM`; verification hashing uses the separately configurable `AUTH_CODE_SECRET`.

Database, SMTP, and JWT secrets are supplied through environment variables and are absent from the current source configuration. The repository includes a safe `.env.example` contract, while real `.env` variants are ignored by Git. Credentials that existed in earlier repository history must still be rotated before deployment.

The JWT signing secret must be Base64 encoded and contain at least 32 random bytes before encoding. JWT lifetime is configurable through `JWT_EXPIRATION_MS` and defaults to 24 hours.

Local development can load an ignored `.env` file and use a persistent H2 database in MySQL compatibility mode. Deployment environment variables override local values and may point the same application to MySQL. Automated tests use a separate H2 in-memory database with test-only mail and JWT values, so tests do not require developer MySQL or SMTP credentials and do not write to local or production data.

## Data protection and governance

### Data classification

| Classification | Examples | Required handling |
| --- | --- | --- |
| Authentication secret | Password hashes, JWT signing secret, SMTP password | Never returned by APIs or logged; inject secrets at runtime |
| Personal information | Name, email, username, phone, date of birth | Return only through approved DTOs and authorized routes |
| Career information | Skills, experience, résumé URL, portfolio URL, biography | User-controlled profile data; snapshot only when applying |
| Business information | Salon profile, location, hours, hiring posts | Publish only for accepted and active salons |
| Workflow history | Applications, employment requests, leave decisions, rejection reasons | Preserve for product history and future audit requirements |

The current implementation does not yet define retention periods, user data export, consent records, or permanent erasure rules. Those policies must be agreed before production because they affect database relationships, audit history, and regulatory obligations.

### Logging rules

- Never log passwords, JWTs, authorization headers, signing secrets, SMTP credentials, or full résumé content.
- Avoid logging complete personal profiles; use immutable IDs and workflow IDs for correlation.
- Production logs should be structured and include request correlation IDs, severity, event name, and safe entity identifiers.
- Security and moderation events should become durable audit records before launch.

## Non-functional requirements

> **Status:** The targets below define production acceptance criteria. They are requirements for release planning and are not claims about the current runtime.

| Category | Production target | Current assessment |
| --- | --- | --- |
| Availability | Define an uptime objective and maintenance policy before launch | Not defined |
| Performance | Establish p95 latency targets per API class and test realistic data volumes | Not measured |
| Scalability | Support stateless horizontal API scaling with shared revocation and persistence services | Blocked by in-memory token blacklist |
| Security | TLS, managed secrets, least privilege, dependency scanning, and authorization tests | Partial |
| Reliability | Transactional state changes, idempotency where required, and recovery procedures | Partial |
| Data integrity | Versioned migrations, constraints, locking, and production backup validation | Partial |
| Observability | Structured logs, metrics, traces or correlation IDs, dashboards, and alerts | Planned |
| Accessibility | Frontend journeys should target WCAG 2.2 AA | Frontend responsibility; not yet verified |
| Localization | Define supported languages, locale-sensitive dates, and message strategy | Planned |

## Environment and configuration model

| Environment | Purpose | Database | Secrets | Schema strategy |
| --- | --- | --- | --- | --- |
| Local development | Developer implementation and manual testing | Persistent H2 in MySQL compatibility mode by default; MySQL can be configured | IDE or shell environment | Hibernate update currently used |
| Automated test | Repeatable unit and Spring context verification | Ephemeral H2 in MySQL mode | Test-only values | Create and drop |
| Staging | Production-like validation and release acceptance | Managed isolated MySQL | Deployment secret manager | Planned versioned migrations |
| Production | Customer traffic and durable business data | Managed MySQL with backup policy | Deployment secret manager | Required versioned migrations |

Required runtime variables are `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `JWT_SECRET`. `DB_URL`, `DB_USERNAME`, `MAIL_HOST`, `MAIL_PORT`, and `JWT_EXPIRATION_MS` have development defaults and remain overrideable. Spring Boot does not automatically import `.env`; the launcher, IDE, container platform, or hosting platform must inject the values.

## Deployment architecture

### Target production topology - planned

```text
Internet client
  -> TLS load balancer / reverse proxy
      -> Static React application
      -> /api to Salon Hup API instances
          -> Managed MySQL
          -> Durable token/session revocation store
          -> SMTP or transactional email provider
          -> Object storage for managed résumé and media uploads
          -> Metrics, logs, alerts, and error tracking
```

The frontend currently builds into a static `dist/` bundle and the backend packages as a Spring Boot service. Production must rewrite client routes to `index.html`, proxy `/api` to the backend or configure an approved API origin and CORS policy, and serve both over HTTPS. Container definitions, environment-specific configuration, database migrations, shared token revocation, health/readiness policy, and the hosting pipeline still need implementation.

### Deployment sequence - planned

1. Build and test an immutable application artifact.
2. Scan dependencies and the container image for known vulnerabilities.
3. Back up the production database and verify migration compatibility.
4. Apply versioned database migrations through a controlled release job.
5. Deploy to staging and run smoke, authorization, and critical workflow tests.
6. Deploy gradually to production with health checks and rollback criteria.
7. Verify logs, metrics, email connectivity, database health, and core user journeys.

## Testing and quality strategy

### Current automated evidence

The backend suite covers Spring application startup, immutable JWT identity, blocked-account authentication, verified-registration behavior, verification expiry and attempt limits, non-enumerating password recovery, one-time reset authorization, old-JWT invalidation, dashboard moderation logic, user-directory mapping, employment authorization, hiring lifecycle and expiration, salon resubmission rules, salon-service ownership and visibility, employee capability replacement, employee schedule validation and availability, booking lifecycle, overlap prevention, reassignment rules, notification ownership and read state, notification deduplication and booking-event creation, verified salon reviews, URL validation, and non-empty `ApiResponse.error` reasons. It runs against an isolated H2 test database. On 31 October 2026 the complete backend suite passed 123 tests, including nine new security-filter integration tests. Those tests use the production `SecurityConfig`, JWT filter, JWT service, blacklist, Jackson mapper, and response handlers with a mocked user lookup and probe controllers, isolating filter authorization from controller business logic. They verify missing, malformed, blacklisted, and blocked-account authentication; anonymous admin requests; authenticated non-admin denial; permitted user/admin requests; and public endpoint access. The frontend includes Vitest and Testing Library coverage for all active routes including the four new public auth routes, login redirects, authentication subscriptions, public-auth JWT exclusion, auth action loading messages, 401/403 behavior, stale request cancellation, response-envelope handling, salon payload contracts, action-loading state, and route/navigation loading. The 31 October frontend run passed all 66 tests, lint, and the production build without frontend source changes. A combined staging acceptance run with real SMTP remains required.

### Required test layers

| Layer | Purpose | Release expectation |
| --- | --- | --- |
| Unit | Validate scoring, validation, and isolated state rules | Required for complex branching rules |
| Service integration | Validate transactions, repositories, locks, and mappings | Required for critical workflows |
| Authorization | Verify every role and ownership boundary | Required before production |
| API contract | Protect request, response, and `ApiResponse` compatibility | Required before frontend release |
| Concurrency | Prove capacity, position, transfer, and block transitions under contention | Required before production |
| End-to-end | Validate registration through business outcome in staging | Required for release candidates |
| Performance | Establish capacity, latency, and database-query baselines | Required before launch |

## Observability and operations

### Required operational signals - planned

- Health and readiness checks for the API, database, and required external services.
- Request volume, error rate, latency percentiles, JVM health, connection-pool pressure, and database saturation.
- Business metrics for registration, salon approvals, applications, hiring acceptance, bookings, completion, cancellation, and verified reviews.
- Alerts tied to user impact, with an owner, severity, runbook, and escalation path.
- Centralized structured logs with retention and access controls.

### Incident response outline - planned

1. Detect and classify the incident from an alert or user report.
2. Protect users and data by disabling the affected path or rolling back when necessary.
3. Preserve safe logs, deployment metadata, and database evidence.
4. Restore service using the documented rollback or recovery process.
5. Communicate status and impact through the agreed channel.
6. Complete a blameless review with corrective actions and owners.

## Release readiness checklist

| Gate | Required evidence | Current state |
| --- | --- | --- |
| Product scope | Approved release journeys and acceptance criteria | Partial |
| Security | Threat review, authorization matrix tests, secret rotation, TLS | Partial |
| Data | Versioned migrations, backup and restore test, retention policy | Planned |
| Quality | Passing unit, integration, contract, concurrency, and staging E2E suites | Partial |
| Operations | Dashboards, alerts, runbooks, health checks, and ownership | Planned |
| Deployment | Reproducible artifact, staging promotion, rollback procedure | Planned |
| Legal and privacy | Privacy notice, terms, consent and deletion decisions | Planned |
| Customer support | Support channel, issue triage, and escalation process | Planned |

## Current implementation status

| Area | Status | Notes |
| --- | --- | --- |
| Registration and login | Implemented | Core validation, BCrypt, verified email, resend cooldown, login gating, JWT, and immutable-ID authenticated principal available |
| Password recovery | Implemented | Non-enumerating request, short-lived verification code, one-time reset token, shared password policy, and old-JWT invalidation are connected to the React recovery pages |
| React application shell | Implemented | Lazy routes, shared layout, protected navigation, branded route-transition loading, not-found/render-error fallbacks, light/dark themes, and responsive feature pages are available |
| Frontend API integration | Implemented for core journeys | Shared Axios client, JWT attachment, timeout, stale-request cancellation, `ApiResponse` parsing, field normalization, clear error reasons, contextual loading, and toast feedback connect active screens to the API |
| Owner dashboard | Implemented | OWNER-only route and backend-scoped data present salon, staff, customers, bookings, revenue, ratings, and employee-performance insights |
| Design system and branding | Implemented | Plum/gold semantic tokens, accessible dark surfaces, restrained hover treatment, one normal-page header brand, and theme-aware feedback components are shared across the application |
| Interactive prototype | Implemented | `/prototype` demonstrates customer, owner, employee, hiring, booking, invitation, status, schedule, and dashboard states with interactive sample data |
| Unified user profile | Implemented | Registration data, career activation/update, completion score, and URL validation available |
| Logout | Implemented | In-memory blacklist; not durable across restarts |
| Salon onboarding | Implemented | Admin approval, persisted rejection reason, correction, and three explicit resubmissions available |
| Salon profile update | Implemented | Ownership check and rejected-request correction exist |
| User and salon response DTOs | Implemented | Listing endpoints omit passwords and persistence entity graphs |
| Administrative blocking | Implemented | Non-admin user and salon blocking is reversible; prior salon status and owner role are restored after all applicable blocks are removed |
| Direct employment | Implemented | Request, invite, accept, reject, cancel, authenticated identity, and response DTOs available |
| Leave and transfer | Implemented | Normal departure, cross-salon transfer, secure cancellation, and position restoration available |
| Hiring-post creation | Implemented | DTO, repository, service, controller, and validation compile successfully |
| Hiring-post discovery | Implemented | Pagination, filters, controlled sorting, personalized labels, and ranked results available |
| Hiring applications | Implemented | Profile autofill, immutable application snapshot, transparent ranking, owner review, acceptance, and rejection available |
| Hiring-post management | Implemented | Owner list/detail/edit/close/reopen, scheduled expiration, application history, and locked position updates available |
| Salon service catalog | Implemented | Owner create/list/update/disable/reactivate and public active-service discovery are available through DTO-based APIs |
| Employee directory and capabilities | Implemented | Authenticated discovery and owner directories include capability data; owners replace validated service assignments |
| Employee weekly schedules | Implemented | Owners configure working periods; ad hoc absences are handled through reassignment; validation enforces salon hours, non-overlap, and explicit empty availability |
| Customer booking | Implemented | Server-calculated slots, creation revalidation, proposal-based rescheduling, customer and owner history, pending queue, employee view, acceptance, rejection, cancellation, assignment, accepted-interval conflict prevention, completion, and service snapshots are available |
| Notification inbox | Implemented | Booking events persist atomically with recipient and booking snapshots; the frontend polls the authenticated inbox and supports unread count and owned read operations |
| Verified salon reviews | Implemented | Completed-booking ownership, one-review constraint, public list and summary, customer list, deletion, and blocked-user filtering are available |
| Salon media | Partial | Frontend page and API adapter exist; backend entity, persistence, service, and controller are not implemented |
| Interactive API documentation | Partial | The `Live API Test` address and security allowlist exist; Springdoc/OpenAPI dependency and generated runtime UI are not yet enabled |
| Automated tests | Active | Backend suite has 109 passing tests and focused auth verification has nine; frontend has 63 passing tests plus clean lint and production build. Broader staging, SMTP, contention, accessibility, and performance coverage remains required |
| Secret configuration | Implemented | Database, SMTP, sender, auth-code hashing, and JWT secrets use environment variables with a safe example contract |
| Deployment configuration | Planned | Production profiles, migrations, packaging, monitoring, and hosting remain |

## Known technical risks

### Risk register

| ID | Risk | Impact | Likelihood | Treatment |
| --- | --- | --- | --- | --- |
| R-01 | In-memory logout blacklist is lost on restart and cannot coordinate multiple instances | High | High | Add durable shared revocation or refresh-token design |
| R-02 | Automatic schema update can create uncontrolled production changes | High | High | Adopt Flyway or Liquibase before production |
| R-03 | Earlier credentials remain in Git history | High | Medium | Rotate every exposed value and use deployment secrets |
| R-04 | Authorization coverage is incomplete across the full API surface | High | Medium | Add route and service authorization tests |
| R-05 | Employee capacity and notification idempotency need database-backed contention verification | High | Medium | Add MySQL contention tests around accepted-booking locks and duplicate event keys |
| R-06 | Missing retention and deletion policy creates privacy ambiguity | Medium | High | Approve data-governance policy before launch |
| R-07 | Production logs and monitoring are not yet designed | High | High | Implement observability requirements and runbooks |
| R-08 | Holidays and employee time-off exceptions are not implemented | Medium | High | Add dated salon closures and employee absence records to the availability calculation |
| R-09 | Payment processing and reconciliation are not implemented | High | Certain | Approve payment methods and provider before defining secure payment APIs |
| R-10 | Employee and service ratings are not implemented | Low | High | Approve per-booking rating dimensions and edit policy before extending verified reviews |
| R-11 | Frontend salon-media calls have no matching backend routes | Medium | Certain | Define media ownership, storage, moderation, DTOs, and persistence before enabling the page |
| R-12 | Swagger UI security paths exist without the Springdoc runtime dependency | Low | Certain | Add and configure a compatible Springdoc/OpenAPI dependency before publishing Live API Test |
| R-13 | Resolved: Spring Security 401/403 responses use the application `ApiResponse` envelope | Medium | Resolved | Custom authentication entry point and access-denied handler preserve status codes and clear messages; nine filter-chain integration tests cover rejected and permitted requests |
| R-14 | Dashboard financial values are operational summaries rather than settled payment records | High | High | Label them as booking revenue estimates until payment capture, refunds, fees, and reconciliation are implemented |

### Entity exposure

Employment, leave, user-listing, salon-listing, and requested-salon routes return dedicated DTOs. Remaining controllers should be audited for direct JPA entity responses before deployment.

### Secret management

Current source configuration obtains database credentials, SMTP credentials, and the JWT signing key from environment variables. Previously committed credentials remain in Git history and must be rotated before deployment. Production infrastructure should inject the replacement values through its secret manager.

### Token revocation

The logout blacklist is stored in application memory. It disappears after a restart and does not work consistently across multiple server instances. A durable revocation or refresh-token design is required for production.

### Data integrity and concurrency

Hiring-post position reservation, post lifecycle changes, linked transfer cancellation, booking decisions, accepted-appointment overlap checks, notification event keys, and one-review-per-booking creation use transactional locks or database constraints. Employee-capacity mutations, notification retries, and review creation should receive database-backed concurrency tests before deployment.

### Error and validation consistency

The approved API contract represents business outcomes through the existing `ApiResponse<T>` envelope and its `success` and `error` factories. `ApiResponse.error` now prevents blank reasons, and the frontend preserves useful response and transport details through one shared normalizer. Future validation work must keep this response structure and must not introduce a new HTTP-status strategy or centralized exception handler. Remaining consistency work should standardize field validation and security-filter responses within this contract.

### Schema lifecycle

Hibernate currently uses automatic schema update. Production environments should use versioned database migrations such as Flyway or Liquibase.

## Development roadmap

### Priority 1 Complete hiring management - completed

- Allow owners to edit, close, and inspect their posts.
- Add scheduled expiration handling.
- Let applicants list and cancel their pending applications.
- Prevent repeat application after rejection or cancellation.

### Priority 2 Secure existing workflows - in progress

- Replace request-supplied current user IDs with authenticated identities. Completed for employment and leave.
- Introduce response DTOs for users, salons, employment requests, and leave requests. Completed for these current API surfaces; remaining controllers still require an exposure audit.
- Add role checks at route and service levels.
- Correct the admin salon lookup and owner-blocking behavior. Completed with reversible user and salon blocking, independent overlapping restrictions, role restoration, and blocked-token enforcement.
- Correct the salon-rejection request contract. Completed with persisted reasons and controlled resubmission.

### Priority 3 Hiring workflow hardening

- Expand ranking tests to cover boundary scores, pagination, filters, and incomplete profiles.
- Protect position reservation with database locking or optimistic versioning.
- Add application history and audit timestamps.
- Add managed résumé upload storage instead of relying only on a URL.

### Priority 4 Booking foundation

- Define salon services, prices, and durations. Completed with owner management, public active-service discovery, validation, DTOs, ownership checks, and reversible disable.
- Define which employees can perform each service. Completed with owner-managed capabilities and full-service-set enforcement.
- Define weekly employee working periods. Completed with validated periods, inherited salon hours, and explicit empty availability. Personal breaks remain ad hoc; owners handle conflicts by assigning another qualified employee and informing the customer.
- Create booking request, confirmation, rejection, cancellation, assignment, completion, customer history, owner queue, and employee views. Completed.
- Preserve service price, duration, and name snapshots in booking history. Completed.
- Add booking-verified salon ratings, public review lists, and aggregate summaries. Completed.
- Reject overlapping accepted bookings. Completed with employee write locking, complete-interval comparison, and adjacent-appointment support.
- Calculate server-owned available appointment slots. Completed with 15-minute generation, qualified-employee details, accepted-conflict filtering, 30-minute same-day notice, creation-time revalidation, and connected frontend selection.
- Connect the frontend notification inbox with 30-60 second signed-in polling and refresh on application activation. Completed.
- Add holidays, employee time off, reminders, and salon calendar views. Customer-approved rescheduling is completed with proposal history, server availability, acceptance-time revalidation, and durable events.
- Approve and implement payment methods, provider integration, refunds, and reconciliation.
- Extend reviews with approved employee and service rating rules.

### Priority 5 Production readiness

- Add meaningful unit, integration, authorization, and concurrency tests.
- Introduce database migrations and environment-specific profiles.
- Rotate the credentials that existed in repository history and store their replacements in the deployment secret manager.
- Add structured logging, health checks, metrics, and error tracking.
- Create Docker and deployment configuration.
- Configure production CORS, TLS, backups, and recovery procedures.
- Perform final security and performance reviews.

## Glossary

| Term | Meaning |
| --- | --- |
| Career profile | The optional professional section of the single user profile |
| Direct salon block | An administrator restriction placed on the salon itself; owner stays active as `USER` |
| Owner-derived salon block | Salon inactivity caused by blocking the salon owner account |
| Hiring application snapshot | Applicant career data copied at submission time for historical consistency |
| Suggested | A complete match that exceeds experience requirements and contains additional skills |
| Matched | An application or post where every required condition is satisfied |
| Missing requirements | A visible candidate with one or more unmet post conditions |
| Pending leave | A transfer stage in which a new opportunity is accepted but the current salon must release the employee |
| Reserved position | A hiring-post position consumed while an accepted transfer is waiting for leave completion |
| Salon service | A priced and timed offering owned by one salon and shown publicly only while active |
| Employee capability | A persisted relationship showing that one salon employee can perform one active salon service |
| Working period | A weekly day and time interval during which an employee can receive a complete appointment |
| Inherited schedule | Availability derived from salon hours when no employee schedule has been configured |
| Availability slot | A server-calculated appointment start where at least one fully qualified employee can cover the complete selected-service duration |
| Reschedule proposal | An owner-created date-and-time offer that changes the appointment only after the booking customer accepts it |
| Booking service snapshot | The service name, price, duration, and source ID copied when a booking is created |
| Notification event key | A deterministic unique value that prevents the same logical booking event from producing duplicate inbox records |
| Notification snapshot | Salon and booking display data copied into an inbox item so the frontend can render a useful durable message |
| Verified salon review | A one-to-one review created by the customer from an owned completed booking |
| Soft disable | A reversible state change that hides a service without deleting its identity or future booking references |
| DTO | A request or response object that defines an external API contract without exposing persistence entities |

## Quality and review policy

Every feature should move through a small reviewable slice:

1. Confirm business rules with the product owner.
2. Add or update entities only when persistence changes are required.
3. Create request and response DTOs.
4. Add repository methods with clear names.
5. Define the service interface and implement business validation.
6. Add a thin controller that uses authenticated identity.
7. Compile and run relevant tests.
8. Review the code and API behavior before moving to the next feature.
9. Update this document after the change is approved.

## Documentation maintenance

The editable source is `documentation/Salon-Hup-Project-Documentation.md`. The published PDF is `output/pdf/Salon-Hup-Project-Documentation.pdf`. The document is maintained with the backend repository because it describes the shared product, but every approved frontend or backend change must update the relevant product, integration, API, testing, and diagram sections.

Approved feature batches should update the document version, last-updated date, implementation-status table, relevant workflow, API catalog, domain model, and roadmap. Draft or unapproved behavior should remain marked as planned and should not be described as implemented.

Document versions use three numeric positions: `major.minor.patch`.

- Increase `patch` for a small correction or routine documentation update: `0.0.1` through `0.0.9`.
- Increase `minor` for a large feature or substantial section update and reset `patch` to zero: `0.1.0`.
- Increase `major` for a complete document restructuring or redesign and reset the other positions: `1.0.0`.
- Each position rolls over after nine. For example, the update after `0.0.9` is `0.1.0`.

## Revision history

| Version | Date | Summary |
| --- | --- | --- |
| 1.4.1 | 31 October 2026 | Standardized security-filter 401/403 responses through `ApiResponse.error`, preserved endpoint permissions and frontend session behavior, resolved R-13, and added nine filter-chain integration tests plus current verification evidence |
| 1.4.0 | 27 October 2026 | Added verified registration, email-code lifecycle and resend cooldown, non-enumerating password recovery, one-time reset authorization, credential-change JWT invalidation, four connected React auth pages, action and navigation loading integration, public-auth token policy, complete API contracts, security model, auth sequence diagram, and final backend/frontend verification evidence |
| 1.3.0 | 26 October 2026 | Rethemed the full publication to the Salon Hup plum-and-gold design system; documented the owner dashboard, interactive prototype, light/dark behavior, accessible hover treatment, single-header branding, Signature Snip action loading, branded route-transition loading, concurrent loader lifecycle, centralized frontend error display, backend error-message fallback, and new frontend feedback diagram and verification evidence |
| 1.2.4 | 18 October 2026 | Added customer-approved booking rescheduling, trusted reschedule availability, proposal history and decisions, acceptance-time revalidation, concurrency locks, durable proposal notifications, connected owner and customer interfaces, a reschedule sequence diagram, four API contracts, and 98-test verification evidence |
| 1.2.3 | 18 October 2026 | Added server-calculated 15-minute booking availability, qualified-employee slot details, 30-minute same-day notice, accepted-conflict filtering, booking-creation revalidation, connected frontend slot selection, updated booking diagrams and API contracts, and 89-test verification evidence |
| 1.2.2 | 13 October 2026 | Added persistent recipient-scoped booking notifications, authenticated paginated inbox and read APIs, atomic and idempotent event creation, frontend polling contract, accepted-booking overlap prevention, notification sequence diagram, API tables, authorization rules, and 83-test verification evidence |
| 1.2.1 | 13 October 2026 | Added a portrait application-flow chart for completed customer, owner, career, and employee journeys; clarified that employee breaks are ad hoc and unavailable appointments are handled by qualified reassignment with a customer update |
| 1.2.0 | 12 October 2026 | Expanded the specification to the complete React and Spring product: frontend architecture, routes and journeys, shared HTTP/error behavior, environment and deployment connection, integration coverage and gaps, Live API Test link, frontend-to-backend diagram, and complete request/response/error tables for every implemented backend endpoint |
| 1.1.1 | 12 October 2026 | Added employee directories, service capabilities, weekly schedules and split working periods, schedule-aware appointment booking, qualified assignment and reassignment explanation, booking state transitions and views, verified salon reviews, five public and customer review routes, updated test evidence, risks, roadmap, availability decision, booking lifecycle, booking sequence, and review sequence diagrams |
| 1.1.0 | 10 October 2026 | Added the salon-service domain and API contract, local persistent development database model, current test evidence, and professional system context, use case, component, sequence, and stakeholder-benefit diagrams |
| 1.0.0 | 29 September 2026 | Rebuilt the complete document as a production product and engineering specification with document control, linked contents, product scope and principles, functional requirements, domain transitions, API conventions, governance, non-functional requirements, environment and deployment models, testing strategy, operations, release gates, risk register, and glossary |
| 0.7.0 | 29 September 2026 | Added reversible non-admin user and salon blocking, independent overlapping block state, exact salon-status and owner-role restoration, administrator-only user directories, and immediate blocked-token enforcement |
| 0.6.0 | 29 September 2026 | Added safe user and salon listing DTOs, administrator-only full salon listing, persisted salon rejection reasons, owner correction, and a three-resubmission limit |
| 0.5.0 | 29 September 2026 | Externalized database, SMTP, and JWT secrets; added a safe environment contract, configurable JWT lifetime, and isolated H2 test configuration |
| 0.4.0 | 29 September 2026 | Added owner hiring-post lifecycle management, scheduled expiration, applicant history, protected position restoration, employment and leave DTOs, and immutable user-ID JWT principals |
| 0.3.0 | 26 September 2026 | Added the unified normal/career profile, multiple preferred locations, personalized hiring discovery, profile-based application snapshots, URL validation, and ranking tests |
| 0.2.0 | 26 September 2026 | Added ranked hiring applications, optional résumé scoring, owner decisions, position reservation, and transfer integration |
| 0.1.0 | 26 September 2026 | Initial professional project specification covering the existing backend and hiring-post creation workflow |
