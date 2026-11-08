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
- Remote URL input restrictions were tightened on November 4 (see below). Provider-side
  redirect behavior and imported asset metadata/limits still require acceptance before
  treating Drive imports as production-ready.
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

## Verification â€” November 2, 2026

- `mvn.cmd verify`: 135 tests passed, zero failures/errors/skips; executable JAR packaged.
- `npm.cmd run check`: ESLint passed, 75 tests passed, production build passed.
- Backend has no configured lint command. `git diff --check` passed.
- Sandbox Maven/Vitest launches stalled and were cancelled; the successful runs used
  elevated execution. No tests or assertions were weakened.
- Changed source and documentation were reviewed for secrets and generated artifacts;
  verification logs and build outputs are excluded from commits.

## November 3 continuation â€” provider deletion correctness

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

## November 4 continuation — canonical Drive file imports

The provider accepts only HTTPS `drive.google.com` and `docs.google.com` file links:
`/file/d/{id}` (optional view/preview/edit suffix), `/open?id={id}`, or `/uc?id={id}`.
Only the default HTTPS port or explicit 443 is accepted. Every accepted input becomes
`https://drive.google.com/uc?export=download&id={id}` with an ASCII file identifier.
Caller-supplied sharing parameters and redirect targets are discarded. Arbitrary paths,
Google subdomains, raw googleusercontent URLs, embedded credentials, fragments, encoded
path separators and duplicate query IDs are rejected through the existing ApiResponse
error convention. Device uploads, authorization and schema are unchanged.

This limits the caller-controlled fetch destination; it does not establish provider-side
redirect or content validation. Cloudinary performs the remote fetch, and its actual
Drive redirect behavior remains unverified without credentials. Imported format, byte,
dimension and duration limits and durable rejected-asset cleanup remain open.

Changed files:

- `src/main/java/com/salon/backend/Services/Media/CloudinaryMediaStorageService.java`
- `src/test/java/com/salon/backend/Services/Media/CloudinaryMediaStorageServiceTest.java`
- `documentation/Daily-Work-Plan.md`
- This status document.

The 27 new parameterized cases assert exact normalized multipart input for supported
links and zero provider requests for rejected links.

Implementation commit: `8e7e76c` (`fix(media): canonicalize and restrict Google Drive import URLs`).
Verification: `mvn.cmd verify` passed 177 tests with zero failures/errors/skips and
packaged the executable JAR. Frontend `npm.cmd run check` passed ESLint with zero
warnings, 82 tests and the production build. No backend lint command is configured.
`git diff --check` passed. Reviewed the exact source/test diff and credential fixtures;
no real credentials, local configuration, logs or generated outputs are committed.

Both origins were fetched. The backend persistent daily branch was clean and already
aligned; it was checked out in `C:/My Work/SalonBookingv2/daily-backend` using a linked
worktree. The frontend daily branch was also aligned. Original backend/master and
frontend/main checkouts contain pending owner-dashboard/navigation/auth work and remain
untouched. No new branch, default-branch merge, reset, rebase or history rewriting.
Frontend validation includes the pending user edits and cannot establish committed-branch
reproducibility. No frontend source or configuration is included in automation commits.

No process Cloudinary variables or matching assignments in the original backend local
configuration were found; no live upload was attempted. No Google credentials were added
or verified; the committed frontend now includes the user's Maps picker, whose live
credential setup remains unverified. No official Drive picker was added.

Review/setup: configure the three backend Cloudinary variables in the deployment
environment and restart. In staging, test a public Drive file share URL, `open?id` and
`uc?id`; confirm successful imports retain a Cloudinary delivery URL. Try a subdomain,
nonstandard port or Google redirect path and confirm an ApiResponse error before any
provider call. Review remaining gaps above before accepting the coordinated feature.
Keep the existing My Salon v0 prompt for user review before final redesign integration.


## November 6 continuation — validate captions before upload

Device and Drive employee-work creation now normalize captions and enforce the existing
300-character limit after employee authorization but before calling media storage. Previously,
a rejected caption uploaded an asset first and then attempted deletion without preserving
a failed cleanup. The new path creates no remote asset and writes no gallery record for
an oversized caption. Blank captions still store null; surrounding whitespace is trimmed;
300 normalized characters remain valid. Existing ApiResponse messages are preserved.
No endpoint, schema, provider configuration, or frontend contract changes are needed.

Regression coverage exercises both sources, no provider/database interactions for rejected
captions, the trimmed 300-character boundary, null/blank captions, normalization, and
preserved authorization precedence. Durable replacement/rejected-cover cleanup and database
transaction orphan recovery remain open and are not solved by this prevention fix.

The public-directory privacy gap remains open: customer booking uses the same public
employee response IDs for eligibility and assignment. The gallery compares employee IDs
for delete controls. Remove employee identifiers only with coordinated booking and ownership
contracts and regression coverage; removing the field alone would break those workflows.

Checkout note superseding previous pending-edit constraints: both original application
checkouts were clean at inspection (backend master e297c3c; frontend main 92ab732).
Both origins were fetched. Automation reused the clean backend linked worktree and selected
the existing frontend daily branch, with safe ff-only updates. No new branch, default-branch
merge or history rewriting occurred. Frontend daily source remains unchanged.

No Cloudinary environment variables or matching local backend assignments were found; no
live upload was attempted. No Google credentials were added or verified. Drive share-link
and external Maps fallbacks remain; official Drive/embedded Maps credential acceptance is
still unverified. The existing My Salon v0 prompt remains available for review.

Changed files: SalonMediaServiceImpl.java, SalonMediaServiceImplTest.java,
Daily-Work-Plan.md and this status document.

Review/setup: configure CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET
only in the backend environment and restart. As an active employee in staging, submit
301-character captions to POST /api/media/work and POST /api/media/work/drive. Both should
return the existing caption error without making a Cloudinary request or saving a gallery
record. Submit a trimmed 300-character caption and confirm publication succeeds. Full live
provider and coordinated feature acceptance still require the remaining gaps listed above.


Verification: mvn.cmd -B verify passed all 185 tests with zero failures, errors or skips,
and packaged the executable JAR. The eight new caption regressions passed. Frontend
npm.cmd run check on the clean, unchanged daily branch passed ESLint with zero warnings,
75 tests and the production build. No backend lint command is configured. git diff --check
passed; exact changes were reviewed for secrets and generated artifacts. An initial sandbox
Maven launch stalled and was cancelled; full verification used elevated execution.
`nImplementation commit: `b8e972d` â€” fix(media): validate work captions before uploading assets.

## November 7 continuation - My Salon cover upload lifecycle

The existing My Salon cover editor now reports device-to-backend byte transfer progress,
then waits for the backend's successful response before updating the salon. Drive imports
stay indeterminate: browser request progress cannot measure the provider download. Both
sources retain authentication, use a two-minute request timeout and avoid the global loading
overlay so the local cancellation control remains reachable. No backend endpoint or schema
change is needed.

Source changes and closing the editor are disabled while uploading; a cleared selection
cannot be submitted. Cancel upload aborts the browser request, resets local busy state and
explains that the server may still finish saving. Refresh before retrying. Navigation aborts
the pending request. Late progress, success and error events cannot overwrite newer state or
notify after navigation. Configuration/network failures retain the selection and show a
recoverable inline error using existing toast conventions.

This closes the My Salon editing gap only. Creation-form and employee-gallery progress and
cancellation still need implementation. Durable provider cleanup, transaction orphans,
remote content validation, image/video limits and metadata, public employee privacy and
booking contracts, employee error states and exact Jordan boundary checks remain open.
The coordinated approved feature remains in progress. The saved v0 prompt is retained.

Changed frontend files: mediaApi.js, useSalonCoverEditor.js, SalonCoverEditor.jsx,
owner-salon.css, useSalonCoverEditor.test.jsx, mediaApi.test.js and README.md. Backend
changes are limited to this status document and Daily-Work-Plan.md. Existing daily branches
were fetched and safely checked with ff-only updates; both were already up to date. The
original backend master checkout and parent repository artifacts remain untouched.

No Cloudinary variables were found in the process or matching assignments in the original
backend local configuration. No live provider call or successful upload is claimed. No Google
credentials were added or verified; existing Drive link/Maps fallback limitations remain.

Verification: mvn.cmd -B verify passed 185 tests with zero failures/errors/skips and packaged
the executable JAR. npm.cmd run check passed ESLint with zero warnings, all 83 tests and the
production build. Eight new regressions cover lifecycle races, cancellation/navigation,
configuration/network recovery, selection clearing and authenticated adapter config forwarding.
The initial lint run rejected an API import in the component-directory integration test;
the test was relocated beside the media hook to follow existing architecture rules. No rules
or assertions weakened. No backend lint command configured. git diff --check passed.

Review/setup: configure CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET
only in the backend deployment environment and restart. Sign in as an owner and open My
Salon > Change picture. Throttle the connection and verify Device progress, server-confirmation
waiting, cancellation and navigation cleanup; test Drive import's indeterminate state. Restore
configuration after an error and retry the preserved selection. Refresh the salon after
cancelling to reconcile a possible completed server save before retrying.

Frontend implementation commit: ca3dc09 - feat(media): add cover upload progress and cancellation.

## November 8 continuation - Current-salon work deletion authorization

Work-media deletion now reuses the active-employee eligibility check used for publication
before looking up any media. The entry must belong to the authenticated employee AND their
current salon. Transferred/departed, blocked/deleted and non-employee accounts cannot delete
old salon assets. Deleted, unaccepted or blocked salons cannot be managed through this route.
Rejected requests do not call Cloudinary or delete database records. Owner access remains
view-only. Successful deletion still waits for provider confirmation; provider errors retain
the record for retry. Authorization uses authenticated identity and database membership,
never a caller-supplied provider public ID. No schema, DTO or frontend contract change.

Fourteen additional regression cases cover successful video deletion ordering, transferred
and missing membership, blocked/deleted/missing accounts, wrong role, ineligible salons,
and missing principal/media IDs. Existing cross-owner and provider-failure tests now supply
valid membership so they continue testing their intended failure paths.

Changed files: Services/Media/SalonMediaServiceImpl.java and its service test, plus
Daily-Work-Plan.md and this status document. Backend mvn.cmd -B verify passed 199 tests,
zero failures/errors/skips, and packaged the executable JAR. No backend lint is configured.

Review: in staging, publish work as an active employee in salon A, retain the media record
ID, transfer the employee to accepted salon B, and call DELETE /api/media/work/{mediaId}
with that employee's authenticated session. Expect success:false and no asset/record deletion.
Repeat for a departed/blocked account and for a different employee. Current-salon authors
should still delete their own work, with provider failure preserving the record for retry.
Configure CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET only in the
backend environment, restart, and verify the allowed live deletion with a disposable asset.
No Cloudinary credentials were found in the process or original backend local configuration;
no live upload/deletion is claimed. No Google credentials were added or verified; Drive
share-link and external Maps fallbacks retain their existing limitations.

Overall approved item 2 remains IN PROGRESS. Durable cleanup/transaction-orphan recovery,
remote metadata and format/size/dimension/duration enforcement, full cover/video metadata,
public employee privacy and booking/ownership contracts, remaining creation/gallery upload
progress/cancellation, employee recoverable errors, and exact Jordan boundaries remain open.
Original backend and frontend contain pending community work; all preserved. Backend work
uses the existing daily-backend worktree on daily-work-automated. No new branch or default
branch merge. The saved My Salon v0 prompt is retained for user review.

Implementation commit: dcf5aaec0f6136bc58ad0af79b5342ed1f18e68b -
fix(media): require current salon membership for work deletion.
Frontend verification initially hit a timeout in the successful-login destination test
(82 passed, one failed). The unchanged route suite then passed all 26 tests in isolation,
and the unchanged full rerun passed all 83 tests and ESLint with zero warnings. Frontend
checks include preserved pending community edits, not a clean-commit reproducibility claim.
No tests or timeouts were weakened. git diff --check passed; staged changes were reviewed
for secrets and generated artifacts. Verification used elevated execution after fetch
sandbox restrictions; both origins fetched and ff-only daily updates were already current.
Frontend production build passed. No frontend automation commit or push was necessary.
