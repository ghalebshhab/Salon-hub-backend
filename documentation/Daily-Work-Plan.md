# SalonHub Approved Daily Work Plan

**Plan status:** Approved  
**Approved on:** 28 October 2026  
**Execution branch:** `daily-work-automated`  
**Next execution date:** No remaining approved item; a new ordered plan needs user approval

This file is the ordered source of truth for the Daily SalonHub Improvement automation. The automation must execute only the first uncompleted approved item, complete its verification, update this file, and stop. It must not infer extra work from the broader product roadmap.

## Approved item 1 — Standardize Spring Security 401 and 403 responses

**Status:** Completed — 31 October 2026

### Goal

Ensure authentication and authorization failures produced by the Spring Security filter chain return the existing `ApiResponse.error(...)` JSON envelope instead of framework-default or empty responses.

### Scope

- Add a custom authentication entry point for unauthenticated requests.
- Add a custom access-denied handler for authenticated users who lack the required role.
- Register both handlers in `SecurityConfig`.
- Return JSON with the existing `ApiResponse` fields: `success`, `message`, and `data`.
- Preserve HTTP status `401` for unauthenticated access and `403` for insufficient permissions.
- Use clear, stable messages suitable for the frontend error normalizer.
- Add focused security integration tests covering one protected endpoint and one role-restricted endpoint.
- Update the maintained project documentation and test evidence after implementation.

### Acceptance criteria

- A request without valid authentication to a protected endpoint returns HTTP 401, `application/json`, `success: false`, a nonblank message, and `data: null`.
- An authenticated non-admin request to an admin-only endpoint returns HTTP 403 with the same envelope contract.
- Existing public endpoints remain accessible.
- Existing authentication and authorization behavior remains intact.
- The complete backend test suite passes.
- No frontend change is required unless verification reveals a contract incompatibility.
- The automation creates a focused conventional commit and pushes only after successful verification.

### Out of scope

- Changing the established `ApiResponse` structure.
- Introducing a centralized business-exception strategy.
- Changing endpoint permissions or role definitions.
- Adding refresh tokens or durable JWT revocation.

## Completion record

- **Completed:** 31 October 2026 (Asia/Amman).
- **Implementation commit:** `aa9e6aaffe32392d8f604082de7f8fe79f816ff7` — `fix(security): standardize unauthorized and forbidden API responses`.
- **Implementation:** Registered `ApiAuthenticationEntryPoint` and `ApiAccessDeniedHandler` in the existing `SecurityConfig`. Both serialize `ApiResponse.error(...)` through the configured Jackson mapper with HTTP 401/403 and JSON content type. Messages match the frontend's existing status fallbacks. No endpoint permissions, roles, or business rules changed; no frontend source change was needed.
- **Focused verification:** `mvn.cmd -Dtest=SecurityResponseIntegrationTest test` — 9 tests passed. The MVC slice explicitly imports Spring Boot 4 security auto-configuration and exercises the real filter chain with real JWTs, mocked user lookup, and probe controllers.
- **Backend verification:** `mvn.cmd verify` — 123 tests passed, zero failures/errors/skips; executable Spring Boot JAR packaged successfully. No backend lint command is configured.
- **Frontend verification:** `npm.cmd run check` in `C:\My Work\salon-community-frontend` — ESLint passed with zero warnings, all 66 tests passed, production build passed.
- **Documentation:** Updated the maintained product specification to 1.4.1 with the security error contract, R-13 resolution, and current test evidence.
- **Review focus:** Confirm that unauthenticated protected requests return 401, authenticated non-admin requests to admin routes return 403, both carry `success: false`, the documented stable message, and explicit `data: null`; public routes and permitted authenticated requests remain accessible. Tests isolate filter behavior from controller business logic.
- **Next action:** No further approved item remains. Approve and record a new ordered daily/weekly plan before another implementation run.
