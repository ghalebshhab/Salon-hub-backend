# Public salon profile and media acceptance status

The November 1 user-approved coordinated priority remains **in progress**. This November 2
inspection found substantial implementation already committed in both repositories.
It does not establish live-provider or complete production acceptance.

## Completed in this continuation

- Public work gallery filters at the database level for the publishing user's current
  salon, `Employee` role and `Active` status, before limiting results or counting pages.
- Records from former, transferred, blocked, deleted, and non-employee publishers are
  excluded. Historical assets are retained; visibility changes do not delete them.
- Results use creation time descending followed by ID descending to resolve equal times.
- Database tests exercise membership/moderation transitions, isolation between salons,
  page counts, and stable ordering; service tests cover pagination bounds and blocked salons.
- No schema change is needed for this query-only correction.

## Existing implementation found

- Dedicated public employee DTO and UI section; owner join-action suppression.
- `MediaStorageService`, authenticated Cloudinary HTTP upload/deletion implementation,
  device signature/MIME and byte-size validation, owner cover and employee media APIs.
- Work media entity with provider URL/public ID, owner associations and image metadata.
- Device/Drive selection, previews, cover editing, public image optimization and gallery UI.
- Maps URL/coordinate fields and Jordan bounding-box validation.
- [Detailed My Salon v0 prompt](My-Salon-v0-Design-Prompt.md), for user review before integration.

## Acceptance gaps observed in the inspected code

These remain part of the approved priority, not a claim that the feature is complete:

- Public employee DTOs and gallery responses still contain internal employee IDs; remove
  these from public contracts and adapt rendering keys without exposing private data.
- Cloudinary HTTP request signing and response/error handling now have mocked-provider
  coverage (November 3). Live-provider acceptance still requires real backend credentials.
- The provider enforces byte limits and signatures but lacks image dimension and video
  duration limits. Remote imports bypass local byte/signature validation and need equivalent
  verified metadata/format limits with cleanup for rejected assets.
- Remote URL validation allows broad Google-host subdomains and arbitrary ports. Restrict
  inputs and verify SSRF/redirect handling before treating Drive imports as production-ready.
- Cover replacement ignores failure when removing the previous asset. Durable cleanup and
  transaction-failure/orphan handling remain necessary. Unconfigured or unconfirmed deletion
  now reports an error; explicit gallery deletion retains the record for retry. Replacement
  currently ignores that error, so durable replacement cleanup is still an open gap.
- Cover persistence stores URL and public ID only; complete the required cover metadata and
  video duration persistence. No versioned migration framework currently exists; the project
  uses Hibernate schema update, so document schema changes and deployment requirements.
- Cover upload hooks currently expose a busy flag without byte progress or cancellation.
- Public employee loading shares a combined request with salon details; verify an explicit
  recoverable employee-error state. Verify all gallery states and responsive accessibility.
- Jordan validation is a bounding box, not an exact country boundary. Full links currently
  require separate coordinates; validate normalized links and coordinate consistency.

## Checkout and credential constraints

Frontend pending user edits overlap `SalonFormFields.jsx`, shared styles and a new Google
Maps picker/model/loader. Preserve every pending file, including `.env.example`; those edits
must be completed or safely isolated before coordinated form/location changes are made.
Both existing daily branches were fetched and selected without resets, rebases, merges of
the default branch, or new branches. The frontend switch was between identical commits.

No Cloudinary credential variables were present in the process environment or as matching
assignments in the inspected local backend configuration files. No live upload was attempted.
Configure `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET` in the
backend environment and restart it for live acceptance. Keep the secret outside the browser,
Git, logs and API responses. Do not put it in any `VITE_*` variable.

No official Google Drive picker or embedded Maps integration was added or credential-tested
in this continuation. Keep the public Drive-share-link and external Maps fallback; do not
invent keys. The user's pending Maps picker was not changed or included in automation commits.

## Review and setup

1. Review the query and regression tests; verify old work disappears when an employee leaves
   or is blocked, and that remaining page counts contain only visible entries.
2. Complete or isolate the pending frontend edits, then continue the acceptance gaps above
   on `daily-work-automated` in both repositories. Do not repeat completed implementation.
3. Supply real backend Cloudinary configuration and test device image, Drive image, video,
   deletion and replacement against staging; verify that no secrets enter browser traffic.
4. Review the saved My Salon v0 prompt before integrating any final redesign.

## Files changed in this continuation

- `src/main/java/com/salon/backend/Repositories/Media/EmployeeWorkMediaRepo.java`
- `src/main/java/com/salon/backend/Services/Media/SalonMediaServiceImpl.java`
- `src/test/java/com/salon/backend/Repositories/Media/EmployeeWorkMediaRepoTest.java`
- `src/test/java/com/salon/backend/Services/Media/SalonMediaServiceImplTest.java`
- `documentation/Daily-Work-Plan.md`
- `documentation/Salon-Hup-Project-Documentation.md`
- This status document.

No frontend source or example configuration was changed by the automation. Frontend checks
include the user's pending edits and do not establish reproducibility from the committed
frontend branch alone. The existing v0 prompt was inspected and retained.

## Verification — November 2, 2026

- `mvn.cmd verify`: 135 tests passed, zero failures/errors/skips; executable JAR packaged.
- `npm.cmd run check`: ESLint passed, 75 tests passed, production build passed.
- Backend has no configured lint command. `git diff --check` passed.
- Sandbox Maven/Vitest launches stalled and were cancelled; the successful runs used
  elevated execution. No tests or assertions were weakened.
- Changed source and documentation were reviewed for secrets and generated artifacts;
  verification logs and build outputs are excluded from commits.

## November 3 continuation — provider deletion correctness

Implementation commit: `b95d6146f43ed05bfd77036a10b30e4a7a6e6fde`.

The provider no longer reports deletion success when credentials are unavailable, the
stored public ID is missing, the resource type is unsupported, or the provider response
is empty, malformed or unconfirmed. Only `ok` and `not found` destroy results permit
success; the latter makes retries safe if an earlier request deleted the remote asset
but the database record remained. HTTP errors return clear ApiResponse messages without
provider response details. Upload signature inspection now closes the file input stream.
No endpoint permission, schema or frontend contract changes were needed.

The implementation follows the signed destroy/upload request contracts in the
[Cloudinary Upload API reference](https://cloudinary.com/documentation/image_upload_api_reference).
The production constructor retains environment-based configuration. A package-visible
constructor accepts a RestClient builder so tests intercept HTTP without live credentials.

Changed files in this continuation:

- `src/main/java/com/salon/backend/Services/Media/CloudinaryMediaStorageService.java`
- `src/test/java/com/salon/backend/Services/Media/CloudinaryMediaStorageServiceTest.java`
- `src/test/java/com/salon/backend/Services/Media/SalonMediaServiceImplTest.java`
- `documentation/Daily-Work-Plan.md`
- This status document.

Verification: focused provider/service tests passed 20/20; full `mvn.cmd verify` passed
150 tests with no failures/errors/skips and packaged the executable JAR. Provider tests
exercise multipart and form signing, image/video metadata mapping, Drive normalization,
invalid inputs, provider HTTP failures and ambiguous deletion responses. A service test
proves failed deletion retains the database record and public ID for retry.

Frontend `npm.cmd run check` also passed ESLint with zero warnings, all 75 tests, and
the production build. This includes preserved pending user edits, not a clean-commit
reproducibility claim. No backend lint command is configured. `git diff --check` passed;
credential-pattern and artifact review excluded secrets, local configuration and outputs.

Review/setup: configure the three backend Cloudinary variables documented above and
restart the backend. Publish an employee work item in staging, temporarily remove provider
configuration, and confirm deletion fails while preserving the entry. Restore configuration
and retry; confirm the entry and remote asset are removed. Never commit credential values.
No Cloudinary assignments were found in process environment, local `.env`, or local backend
properties during this run, so no live upload or deletion was claimed. No Google credentials
were added or picker integration verified; the existing external Maps/Drive fallback remains.

The user's overlapping frontend edits remain untouched. Continue the remaining acceptance
gaps above, particularly durable replacement cleanup and remote-import validation, on the
same persistent branches. The overall coordinated priority remains in progress.
