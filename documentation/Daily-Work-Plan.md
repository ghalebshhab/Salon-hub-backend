# SalonHub Approved Daily Work Plan

**Plan status:** Approved  
**Approved on:** 28 October 2026  
**Execution branch:** `daily-work-automated`  
**Next execution date:** 29 October 2026 at 6:00 PM Asia/Amman

This file is the ordered source of truth for the Daily SalonHub Improvement automation. The automation must execute only the first uncompleted approved item, complete its verification, update this file, and stop. It must not infer extra work from the broader product roadmap.

## Approved item 1 — Standardize Spring Security 401 and 403 responses

**Status:** Approved — not started

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

When the item is completed, change its status to `Completed`, record the completion date, commit hash, tests run, and a short implementation note here. If a documented blocker prevents safe completion, leave the item uncompleted and record the blocker without starting another item.
