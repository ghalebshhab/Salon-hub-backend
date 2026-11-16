# SalonHub Approved Daily Work Plan

**Plan status:** Approved  
**Approved on:** 28 October 2026  
**Execution branch:** `daily-work-automated`  
**Next execution date:** Continue approved item 2; see its remaining work and checkout blocker

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
- **Next action at completion of item 1:** No further item had been approved at that time. The subsequent user instruction approves item 2 below.

## Approved item 2 — Public salon profile, owner media and Jordan location

**Status:** In progress — user-approved November 1 priority; continued November 7, 2026.

The automation instruction approves the coordinated public employees, Cloudinary storage,
owner cover selection, owner-specific join action, Jordan Maps location, employee portfolio,
and My Salon v0 prompt work. It supersedes the previous exhausted-plan status.

Much of the initial implementation already exists in backend commits `2b973d3`, `f27a41c`,
`3a1c81c`, `684a02a` and frontend commits `dec1eeb`, `3f07a06`. Do not duplicate those
commits or treat their existence as complete acceptance of this priority.

November 2 continuation fixes public gallery membership filtering before pagination and
adds real database regression coverage. Inactive, departed, transferred, and non-employee
publishers are excluded. Equal creation times use descending record ID for stable ordering.

The frontend checkout contains pending user changes to salon form/location controls and
shared styles. Preserve these; do not stage or rewrite them as automation work. Backend
gallery filtering is independent and does not require changing the frontend API contract.

See [the acceptance status and remaining work](Salon-Public-Profile-Media-Status.md).

November 3 continuation adds mocked HTTP coverage for the Cloudinary provider's signed
upload and destroy requests. Deletion now requires configuration, a stored identifier,
a supported resource type, and a confirmed provider result. Failed deletion preserves
employee gallery records for retry. Replacement cleanup and the other listed acceptance
gaps remain open; this continuation does not mark the coordinated feature complete.

Keep this priority in progress until its remaining acceptance criteria are verified; only
then return to another uncompleted user-approved plan item.

November 4 continuation restricts remote imports to recognized HTTPS Drive file share
links and canonicalizes them to a fixed download endpoint. Arbitrary Google-host paths,
subdomains, ports, redirect targets and ambiguous IDs no longer reach Cloudinary.
Pending dashboard edits now exist in both original checkouts on their default branches.
They remain untouched; backend automation uses the existing `daily-work-automated`
branch in an isolated worktree, with no new branch or default-branch merge.


November 6 continuation moves work-media caption normalization and the existing 300-character
limit before either device upload or Drive import, after employee authorization. Rejected
captions no longer consume provider uploads or depend on best-effort deletion. No schema
or HTTP contract change is required. Replacement cleanup and transaction-orphan recovery
remain open; this is not completion of the coordinated priority.

Both original application checkouts are now clean. Automation reuses the backend daily
worktree and selected the existing frontend daily branch with an ff-only update. The
public employee ID gap needs coordinated booking changes: the public employee endpoint
also feeds customer employee selection, so simply deleting IDs breaks assignment. Gallery
ownership controls also compare employee IDs. Preserve both behaviors when replacing
these contracts; do not treat a React key-only change as a privacy fix.

November 7 continuation adds measured device transfer progress, indeterminate Drive import,
browser cancellation, server-confirmation waiting, and retryable inline errors to the My
Salon cover editor. Obsolete responses are ignored on cancellation/navigation. The global
loading overlay is disabled only for these requests so the cancel control remains usable.
Cancellation does not roll back backend/provider operations. Creation-form and employee
gallery upload progress/cancellation remain open alongside the other acceptance gaps.

November 8 continuation closes the work-media deletion membership gap: only an active
employee may delete their own entry in their current accepted, unblocked salon. Transferred,
departed, blocked/deleted or non-employee callers cannot delete old assets. Rejected requests
never contact the media provider. Provider failures still preserve records for retry.
Fourteen new regression cases; backend verify passes all 199 tests and packages the JAR.
Original backend/frontend community edits remain untouched. Coordinated item 2 remains
IN PROGRESS; see the status document for remaining acceptance gaps and staging steps.

November 9 continuation closes the public team loading/recovery gap. Frontend 4e115d0
loads salon and employees independently; team failures show a local retryable alert,
loading is announced, and successful empty responses retain the empty state. Retry
fetches only employees; navigation/unmount ignore stale results. Six new regressions;
frontend check passes lint, 97 tests and production build. Backend verify passes 199 tests.
No runtime backend/schema changes. Prior pending community work is now user-committed;
both daily checkouts began clean. Approved item 2 remains IN PROGRESS with the remaining
media/privacy/location gaps documented in Salon-Public-Profile-Media-Status.md.

November 10 continuation validates Cloudinary upload delivery identity before cover/gallery
persistence: explicit image/video type, usable stored public ID, and HTTPS shared-CDN upload
URL for the configured cloud and matching resource type. Reject malformed/foreign/local
URLs using ApiResponse errors without returning provider details. Thirty new HTTP-mocked
regressions; backend verify passed 229 tests and packaged the JAR. This does not close content
limits or durable rejected-asset reconciliation. See the status document for review/setup,
verification and remaining approved work. Item 2 stays IN PROGRESS.

November 11 continuation closes incomplete local PNG/GIF/WebP signature checks before
Cloudinary uploads (implementation c2c38ba). Forty-one new regressions; focused provider
suite 112 passed; backend verify 270 passed and packaged JAR; frontend lint, 98 tests
and production build passed. No live credentials or provider calls. Frontend local commits
remain preserved and unpushed. This is a signature gate, not full media content validation.
Item 2 remains IN PROGRESS; see the status document for files, review/setup and remaining
cleanup/content/privacy/location/upload-state acceptance work. Do not repeat this fix.

November 14 continuation persists salon-cover upload metadata on the salon itself: Cloudinary
resource type, intrinsic width, height and byte count now accompany the already persisted HTTPS
delivery URL and public ID. The fields are nullable for existing salons and are filled only by
the authenticated owner cover-upload flow. Hibernate schema update creates the new nullable
columns during deployment; no standalone migration framework exists in this repository. This
does not claim video-duration support, remote-content validation or replacement-orphan recovery.
Full backend verification passed (270 tests, zero failures/errors/skips, executable JAR
packaged); see the status document for verification and deployment review. Item 2 remains IN
PROGRESS.

November 16 continuation persists Cloudinary video duration metadata for employee work media.
The storage adapter reads a finite, non-negative numeric `duration` only from video upload
responses; it ignores malformed or negative values. The service saves that value with the
employee-owned gallery entry and returns it in the public gallery DTO so clients can present
video context without probing the provider. Existing rows remain nullable, and Hibernate
schema update adds the nullable column at deployment. This records provider metadata only;
it does not enforce a maximum video duration or close remote-content validation, replacement
cleanup, or transaction-orphan gaps. Item 2 remains IN PROGRESS.
Focused provider/service tests passed 143 cases; full `mvnw.cmd -B verify` passed 273 tests
with zero failures, errors, or skips and packaged the executable JAR. No live Cloudinary
credentials were available, so all provider checks used mocked HTTP responses.
