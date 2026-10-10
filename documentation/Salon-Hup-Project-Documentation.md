# Salon Hup Project Documentation

**Document type:** Product and engineering specification
**Project:** Salon Hup
**Backend version:** 0.0.1 SNAPSHOT
**Document version:** 1.1.0
**Last updated:** 10 October 2026
**Status:** Active development

## Document purpose

This document is the maintained source of truth for the Salon Hup backend. It explains the product vision, implemented business rules, application structure, data model, API surface, security model, current delivery status, known risks, and planned development. It is intended for developers, reviewers, future contributors, and stakeholders preparing the product for deployment.

The document distinguishes between implemented behavior and planned behavior. A capability is only marked as implemented when corresponding source code exists in the repository. Future changes should update this source after the implementation has been reviewed and approved.

> **Document convention:** `Implemented` means working source code exists in this repository. `Partial` means a foundation exists but production controls are incomplete. `Planned` describes an approved direction that has not yet been implemented. Planned content is never presented as current behavior.

## Executive overview

Salon Hup is a platform for salon discovery, service catalogs, salon ownership, employment, hiring, and eventually customer booking. The current backend establishes the identity, salon onboarding, administration, workforce management, and service-catalog foundation needed before appointment scheduling and production deployment can be completed.

The application currently supports user registration and JWT login, a unified normal/career user profile, personalized hiring-post discovery, salon creation requests with administrative approval, salon profile management, employment requests and owner invitations, employee leave and salon transfers, reversible administrative user and salon blocking, hiring-post creation, ranked applications, owner acceptance and rejection, and an owner-managed public service catalog.

Owner-side hiring-post management, scheduled expiration, applicant history, secure employment identity, protected transfer cancellation, and salon-service management are implemented. Appointment availability, booking state transitions, notifications, reviews, and payments are not yet implemented.

### Product status snapshot

| Capability group | Delivery state | Product meaning |
| --- | --- | --- |
| Identity and access | Implemented | Registration, login, logout, JWT authentication, role authorization, and blocked-account enforcement exist |
| Career and hiring | Implemented | Unified career profiles, discovery ranking, applications, owner decisions, and hiring lifecycle exist |
| Salon administration | Implemented | Request review, rejection correction, resubmission, listing, blocking, and restoration exist |
| Workforce management | Implemented | Join, invitation, leave, transfer, and linked hiring acceptance flows exist |
| Salon service catalog | Implemented | Approved owners manage priced, timed services and public salon pages receive active offerings |
| Customer booking | Partial | Service discovery exists; availability, appointments, customer history, and calendar APIs remain planned |
| Production operations | Partial | Environment secrets and health foundation exist; migrations, containers, durable revocation, monitoring, and hosting remain |

## Product scope

### In scope for the product

- Consumer accounts, salon owners, salon employees, and platform administrators.
- Salon onboarding and administrative governance.
- Career profiles, recruitment discovery, applications, and hiring decisions.
- Direct employment invitations, requests, leave, and salon transfer workflows.
- Public salon discovery and the future customer booking journey.
- Operational controls required to deploy and support the backend safely.

### Current release boundary

The current backend release is a workforce, salon-management, and service-catalog foundation. It can support authentication, salon approval, career profiles, hiring, employment, administrative moderation, and public service discovery. It is not yet a complete customer booking product because staff capabilities, schedules, slot calculation, appointments, payments, notifications, reviews, and production infrastructure remain planned.

### Explicitly outside the current implementation

- Payment collection, refunds, invoices, and financial reconciliation.
- Customer appointment creation, rescheduling, cancellation, and reminders.
- Real-time chat, social feeds, reviews, and content moderation.
- Native mobile push notifications and production email templates.
- Multi-tenant billing, subscription plans, and salon analytics.

## Product principles

- **Server-owned identity:** clients never choose the authenticated user, owner, or salon relationship for protected workflows.
- **Reversible administration:** moderation preserves recoverable business state unless an explicit deletion policy applies.
- **One user profile:** career information extends the registered profile instead of creating a second identity.
- **Transparent ranking:** hiring labels and scores come from documented, reproducible rules.
- **Permanent application history:** a user applies once to a hiring post and historical decisions remain traceable.
- **Safe evolution:** implemented behavior, planned behavior, and known production gaps remain visibly separated.

## Product goals

- Give users a secure account and a clear role in the platform.
- Allow eligible users to request ownership of a salon profile.
- Require administrative review before a salon becomes active.
- Help salon owners recruit and manage employees.
- Allow users to request employment or respond to salon invitations.
- Preserve employment state when an employee transfers between salons.
- Publish structured hiring posts and route applications to the correct salon owner.
- Build a reliable foundation for customer booking and deployment.

## Roles and responsibilities

| Role | Purpose | Current permissions |
| --- | --- | --- |
| USER | Default registered account and potential employee | Sign in, view the unified profile, enable career mode, receive personalized hiring suggestions, request employment, and apply to hiring posts |
| OWNER | User approved as the owner of a salon | Manage the owned salon, invite users, review employment requests, accept leave requests, and create hiring posts |
| Employee | User currently assigned to a salon | Participate in employment and leave workflows and transfer to another salon after the required workflow |
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
| FR-01 | A visitor can register a unique validated account and authenticate securely | Implemented |
| FR-02 | A user has one profile that can be upgraded with career information | Implemented |
| FR-03 | A user can request a salon and an administrator governs activation | Implemented |
| FR-04 | Owners and users can create and decide employment relationships through controlled workflows | Implemented |
| FR-05 | Owners can publish vacancies and receive every application ranked by transparent fit rules | Implemented |
| FR-06 | Administrators can reversibly restrict non-admin users and salons | Implemented |
| FR-07 | Customers can discover active salon services | Implemented |
| FR-08 | Customers can reserve valid appointment slots | Planned |
| FR-09 | The platform can notify participants about time-sensitive workflow changes | Planned |
| FR-10 | Production operators can deploy, monitor, migrate, back up, and recover the system | Planned |

## Technology stack

| Layer | Technology |
| --- | --- |
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

The backend uses a layered Spring architecture.

```text
Client
  -> Controller
      -> Service interface
          -> Service implementation
              -> Repository
                  -> MySQL database
```

Controllers define HTTP routes and receive authenticated identity information. DTOs define API input and output contracts. Service implementations contain validation and business rules. Repositories provide database access through Spring Data JPA. Entities represent persisted domain state.

The main Java package is `com.salon.backend` and contains:

- `Config` for Spring Security, password hashing, and JWT filtering.
- `Controllers` for authentication, dashboard, salons, salon services, employment, hiring posts, and users.
- `DTOs` for request and response contracts.
- `Entities` for users, salons, salon services, employment, leave requests, and hiring posts.
- `Repositories` for JPA database access.
- `Services` for authentication, administration, salon management, salon-service management, employment, and hiring-post rules.

All service responses use the generic `ApiResponse<T>` envelope containing `success`, `message`, and `data`.

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
- Pessimistic write locks and entity versioning protect sensitive hiring, transfer, and moderation transitions.
- H2 in MySQL compatibility mode isolates automated tests from developer and production databases.
- Secrets enter through environment variables and never through committed production values.

## System diagrams

The following diagrams present the same product from business, runtime, interaction, and value perspectives. Solid flows represent implemented behavior. Labels that include `planned` identify capabilities that remain outside the current release.

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

[[PAGEBREAK]]

### Hiring application sequence

The hiring sequence shows how authenticated profile data replaces repeated applicant entry and how acceptance delegates to the existing employment workflow.

[[DIAGRAM:hiring-sequence]]

[[PAGEBREAK]]

### Salon service sequence

The service sequence shows owner authorization, validation, persistence, public filtering, and reversible disable behavior.

[[DIAGRAM:service-sequence]]

### Stakeholder benefits

The platform creates value by connecting discovery, operational control, career mobility, and platform governance through one consistent domain model.

[[DIAGRAM:benefits]]

## Core domain model

### User

A user stores identity and account information: first name, last name, email, username, BCrypt password hash, phone number, role, status, and an optional salon assignment. The same entity also stores an optional career section containing career-mode status, date of birth, years of experience, skills, preferred work locations, résumé URL, portfolio URL, and professional biography.

The product exposes one unified profile. Registration fields appear automatically in it. Career mode enriches that profile instead of creating a second account or profile. Career skills and preferred locations use collection tables so users may provide multiple values.

User status values are `Active`, `Blocked`, and `Deleted`.

### Salon

A salon stores its name, salon phone, owner phone, email, location, description, owner, employees, current and maximum employee counts, creation date, opening and closing times, profile picture URL, approval status, and opening status. Reversible administration also stores the status that existed before a block, independent owner-block and direct admin-block flags, and the owner's role before a direct salon block.

Salon approval status values are `Inactive`, `Requested`, `Accepted`, `Rejected`, and `DELETED`.

### Salon service

A salon service belongs to one salon and stores a customer-facing name, optional description, decimal price in Jordanian dinars, duration in minutes, active state, and creation and update timestamps. The public catalog returns only active services from an accepted, unblocked salon. The owner catalog includes both active and inactive records.

Disabling a service is a soft state change. The row remains available for later reactivation and for the booking history that will reference it. Only the authenticated owner of an accepted, unblocked salon may create, edit, activate, or disable its services.

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
- A hiring post must be open, unexpired, and have a remaining position before application or acceptance.
- Employee counts and reserved positions change in the same transaction as the related employment decision.
- A direct salon block and an owner-derived block are independent; both must be removed before restoration.
- Direct salon unblocking restores the saved owner role and exact prior salon status.
- Deleted entities are not restored through block and unblock operations.

## Implemented business workflows

### Registration and login

Registration validates names, username format, supported email domains, Jordanian phone formats, password strength, and uniqueness of username, email, and phone number. New accounts receive the `USER` role and `Active` status. Passwords are stored as BCrypt hashes.

Login verifies account status and password, then returns a bearer JWT valid for 24 hours. Logout blacklists the presented token in application memory until its expiration time.

### End-to-end user journey

1. A visitor registers with first name, last name, username, email, phone number, and password.
2. Registration validates identity fields, uniqueness, supported formats, and password strength, then creates an active `USER` account.
3. The user logs in and receives a JWT whose subject is the immutable database user ID. The authenticated principal also exposes email and role without accepting identity from the client.
4. `GET /api/profile` returns one unified profile populated with the identity information already collected during registration.
5. The user may keep the normal profile or select the client action represented by "Make your profile career."
6. `PUT /api/profile/career` validates and stores the career section, then enables career mode on the same profile.
7. The hiring feed loads open, unexpired posts from accepted salons with available positions. Career users receive personalized labels and ordering; normal-profile users receive the standard filtered feed plus a prompt to enable career mode.
8. A career user applies with only an optional application note. The backend obtains identity and career data from the database and stores an application snapshot.
9. The salon owner reviews every application ordered by match level and score, then accepts or rejects it.
10. Acceptance reuses employment rules: an available applicant joins immediately, while an existing employee enters the transfer and leave workflow.

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

## Security model

Spring Security runs without server sessions. A JWT uses the immutable database user ID as its subject and carries email as a claim. The filter validates the token signature and expiration, loads the user by primary key, verifies that the account is still active, and places a typed `AuthenticatedUser` principal and its role in the security context. `getName()` remains the account email for existing controllers, while security-sensitive employment endpoints use the principal ID directly. Passwords use BCrypt. Tokens issued before this subject change require a new login.

The authentication filter does not log authorization headers or bearer-token values. Employment and leave controllers derive caller identity from `@AuthenticationPrincipal` and return response DTOs.

Public routes include login, registration, password-recovery placeholders, dashboard login, API documentation routes, and selected search routes. Dashboard salon and user routes and every user-directory route require the `ADMIN` role. Blocked administrators cannot obtain a dashboard token. Other routes require authentication unless explicitly allowed.

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
| Local development | Developer implementation and manual testing | Local MySQL | IDE or shell environment | Hibernate update currently used |
| Automated test | Repeatable unit and Spring context verification | Ephemeral H2 in MySQL mode | Test-only values | Create and drop |
| Staging | Production-like validation and release acceptance | Managed isolated MySQL | Deployment secret manager | Planned versioned migrations |
| Production | Customer traffic and durable business data | Managed MySQL with backup policy | Deployment secret manager | Required versioned migrations |

Required runtime variables are `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `JWT_SECRET`. `DB_URL`, `DB_USERNAME`, `MAIL_HOST`, `MAIL_PORT`, and `JWT_EXPIRATION_MS` have development defaults and remain overrideable. Spring Boot does not automatically import `.env`; the launcher, IDE, container platform, or hosting platform must inject the values.

## Deployment architecture

### Target production topology - planned

```text
Internet client
  -> TLS load balancer / reverse proxy
      -> Salon Hup API instances
          -> Managed MySQL
          -> Durable token/session revocation store
          -> SMTP or transactional email provider
          -> Object storage for managed résumé and media uploads
          -> Metrics, logs, alerts, and error tracking
```

The application is currently packaged as a Spring Boot service. A production container definition, environment-specific configuration, database migrations, shared token revocation, health/readiness policy, and hosting pipeline still need implementation.

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

The current suite covers Spring application startup, immutable JWT identity, blocked-account authentication, dashboard moderation logic, user-directory mapping, employment authorization, hiring lifecycle and expiration, salon resubmission rules, salon-service ownership and visibility, and URL validation. The suite runs against an isolated H2 test database. At document version 1.1.0, the latest verified suite contains 38 passing tests.

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
- Business metrics for registration, salon approvals, applications, hiring acceptance, and future bookings.
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
| Registration and login | Implemented | Core validation, BCrypt, JWT, and immutable-ID authenticated principal available |
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
| Customer booking | Partial | Service foundation exists; availability, appointments, assignment, history, and calendar APIs remain planned |
| Automated tests | Initial | 38 tests cover application startup and selected JWT, moderation, employment, hiring, salon, salon-service, directory, and URL-validation behavior against isolated H2; broader authorization, integration, concurrency, and performance coverage remains required |
| Secret configuration | Implemented | Database, SMTP, and JWT secrets use environment variables with a safe example contract |
| Deployment configuration | Planned | Production profiles, migrations, packaging, monitoring, and hosting remain |

## Known technical risks

### Risk register

| ID | Risk | Impact | Likelihood | Treatment |
| --- | --- | --- | --- | --- |
| R-01 | In-memory logout blacklist is lost on restart and cannot coordinate multiple instances | High | High | Add durable shared revocation or refresh-token design |
| R-02 | Automatic schema update can create uncontrolled production changes | High | High | Adopt Flyway or Liquibase before production |
| R-03 | Earlier credentials remain in Git history | High | Medium | Rotate every exposed value and use deployment secrets |
| R-04 | Authorization coverage is incomplete across the full API surface | High | Medium | Add route and service authorization tests |
| R-05 | Employee capacity changes need wider concurrency verification | High | Medium | Add database-backed contention tests and constraints |
| R-06 | Missing retention and deletion policy creates privacy ambiguity | Medium | High | Approve data-governance policy before launch |
| R-07 | Production logs and monitoring are not yet designed | High | High | Implement observability requirements and runbooks |
| R-08 | Appointment and availability domains are not implemented | High | Certain | Complete booking scheduling after the service-catalog foundation |

### Entity exposure

Employment, leave, user-listing, salon-listing, and requested-salon routes return dedicated DTOs. Remaining controllers should be audited for direct JPA entity responses before deployment.

### Secret management

Current source configuration obtains database credentials, SMTP credentials, and the JWT signing key from environment variables. Previously committed credentials remain in Git history and must be rotated before deployment. Production infrastructure should inject the replacement values through its secret manager.

### Token revocation

The logout blacklist is stored in application memory. It disappears after a restart and does not work consistently across multiple server instances. A durable revocation or refresh-token design is required for production.

### Data integrity and concurrency

Hiring-post position reservation, post lifecycle changes, and linked transfer cancellation now use transactional locks and version fields. Remaining employee-capacity mutations should be audited and covered with database-backed concurrency tests before deployment.

### Error and validation consistency

The approved API contract represents business outcomes through the existing `ApiResponse<T>` envelope and its `success` and `error` factories. Future validation work must keep this response structure and must not introduce a new HTTP-status strategy or centralized exception handler. Consistency work should standardize messages, field validation, and DTO usage within this contract.

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
- Define which employees can perform each service.
- Define weekly working hours, breaks, holidays, and time off.
- Calculate available appointment slots.
- Create booking, confirmation, cancellation, and rescheduling workflows.
- Add customer booking history and salon calendars.

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

The editable source is `documentation/Salon-Hup-Project-Documentation.md`. The published PDF is `output/pdf/Salon-Hup-Project-Documentation.pdf`.

Approved feature batches should update the document version, last-updated date, implementation-status table, relevant workflow, API catalog, domain model, and roadmap. Draft or unapproved behavior should remain marked as planned and should not be described as implemented.

Document versions use three numeric positions: `major.minor.patch`.

- Increase `patch` for a small correction or routine documentation update: `0.0.1` through `0.0.9`.
- Increase `minor` for a large feature or substantial section update and reset `patch` to zero: `0.1.0`.
- Increase `major` for a complete document restructuring or redesign and reset the other positions: `1.0.0`.
- Each position rolls over after nine. For example, the update after `0.0.9` is `0.1.0`.

## Revision history

| Version | Date | Summary |
| --- | --- | --- |
| 1.1.0 | 10 October 2026 | Added the salon-service domain and API contract, local persistent development database model, current test evidence, and professional system context, use case, component, sequence, and stakeholder-benefit diagrams |
| 1.0.0 | 29 September 2026 | Rebuilt the complete document as a production product and engineering specification with document control, linked contents, product scope and principles, functional requirements, domain transitions, API conventions, governance, non-functional requirements, environment and deployment models, testing strategy, operations, release gates, risk register, and glossary |
| 0.7.0 | 29 September 2026 | Added reversible non-admin user and salon blocking, independent overlapping block state, exact salon-status and owner-role restoration, administrator-only user directories, and immediate blocked-token enforcement |
| 0.6.0 | 29 September 2026 | Added safe user and salon listing DTOs, administrator-only full salon listing, persisted salon rejection reasons, owner correction, and a three-resubmission limit |
| 0.5.0 | 29 September 2026 | Externalized database, SMTP, and JWT secrets; added a safe environment contract, configurable JWT lifetime, and isolated H2 test configuration |
| 0.4.0 | 29 September 2026 | Added owner hiring-post lifecycle management, scheduled expiration, applicant history, protected position restoration, employment and leave DTOs, and immutable user-ID JWT principals |
| 0.3.0 | 26 September 2026 | Added the unified normal/career profile, multiple preferred locations, personalized hiring discovery, profile-based application snapshots, URL validation, and ranking tests |
| 0.2.0 | 26 September 2026 | Added ranked hiring applications, optional résumé scoring, owner decisions, position reservation, and transfer integration |
| 0.1.0 | 26 September 2026 | Initial professional project specification covering the existing backend and hiring-post creation workflow |
