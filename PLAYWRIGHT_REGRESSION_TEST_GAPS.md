# Playwright Regression Test Coverage Gaps

Reviewed against `PLAYWRIGHT_REGRESSION_TEST_SCENARIOS.md` and the current specs under `e2e/tests/`.

## Review summary

- Public page/navigation scenarios `PUB-001` through `PUB-008` are implemented.
- The main happy-path garage scenarios `GAR-001` through `GAR-010` are implemented, with a few assertion-expansion gaps noted below.
- Many P0/P1 user journeys are now covered, but account-change workflows, several add-vehicle edge cases, unavailable vehicle-detail states, validation/close-blocking paths, responsive/a11y smoke checks, and a few resilience cases remain.
- `VEH-003` and `VEH-004` exist as `test.fixme(...)`; they should be treated as not implemented in the passing regression suite until the app bug noted in `e2e/README.md` is fixed.

## Missing scenarios from `PLAYWRIGHT_REGRESSION_TEST_SCENARIOS.md`

### Authentication, session, and CSRF

| ID | Priority | Scenario | Implementation notes |
| --- | --- | --- | --- |
| `AUTH-010` | P1 | Successful sign-up enters unverified account flow. | Needs deterministic email strategy/test hook or a throwaway user fixture. Assert `/home`, verification banner, and Add Vehicle disabled. |
| `AUTH-011` | P1 | Unverified login requiring email verification challenge. | Seeded unverified login currently uses refresh-token fixture; add UI login challenge coverage with Back to sign in and Resend code behavior. |
| `AUTH-014` | P1 | Resend email verification disables controls while pending. | Route-mock slow/success/failure resend response and verify no double-submit. |
| `AUTH-015` | P2 | Login rate-limit message. | Keep isolated/outside default suite to avoid poisoning other login tests. |
| `AUTH-016` | P2 | Expired/invalid session handling. | Simulate invalid/corrupt cookie or expired session and assert redirect to sign-in plus signed-out account state. |

### Account drawer and account management

| ID | Priority | Scenario | Implementation notes |
| --- | --- | --- | --- |
| `ACCT-004` | P1 | Change SMS happy path. | Needs deterministic account-change verification code. |
| `ACCT-005` | P1 | Remove SMS number. | Needs deterministic verification code; verify drawer refreshes to `Not provided`. |
| `ACCT-007` | P1 | Change email happy path. | Use throwaway account and deterministic verification code; assert updated email and intended session behavior. |
| `ACCT-009` | P0 | Change password revokes session after verification. | Project-critical requirement: password change must revoke all refresh tokens and clear the session. Use `password.change@example.com` or another destructive fixture. |
| `ACCT-011` | P1 | Pending account change resumes verification step. | Start change, close/reopen drawer or reload, assert pending verification step resumes from `/api/account/change-requests/pending`. |
| `ACCT-012` | P1 | Resend account-change code. | Route-mock slow/failure states and assert resend button is disabled while pending. |
| `ACCT-014` | P2 | Account deletion flow, if/when UI is added. | Backend endpoint exists, but the scenario document notes no current UI. Implement after UI is available. |

### Vehicle onboarding/add vehicle

| ID | Priority | Scenario | Implementation notes |
| --- | --- | --- | --- |
| `VIN-004` | P1 | Add vehicle duplicate association. | Confirm intended backend/UI behavior, then assert friendly error or no duplicate card. |
| `VIN-008` | P1 | Trim options loading failure. | Route-mock trim-options failure; assert load error and disabled/prevented confirm. |
| `VIN-009` | P1 | Limited-data warning flow. | `VIN-007` clicks `Continue anyways`, but there is no dedicated assertion that the limited-data warning state appears and resumes onboarding correctly. |
| `VIN-010` | P1 | Cannot close add modal while submitting/loading trims. | Route-mock slow `POST /api/vin` or trim-options request; assert Escape/backdrop/close are blocked only while pending. |
| `VIN-012` | P2 | VIN input normalization. | Define expected behavior for lowercase/whitespace VIN input, then assert UI/backend result. |

### Vehicle detail page

| ID | Priority | Scenario | Implementation notes |
| --- | --- | --- | --- |
| `VEH-003` | P0 | Not-found vehicle detail. | Present as `test.fixme(...)`; not part of passing suite until the loading-skeleton error-state bug is fixed. |
| `VEH-004` | P1 | Vehicle detail load failure. | Present as `test.fixme(...)`; not part of passing suite until the loading-skeleton error-state bug is fixed. |
| `VEH-007` | P1 | Owner manual unavailable state. | Needs fixture/route mock with no manual or unsafe manual URL. |
| `VEH-009` | P1 | Warranty unavailable state. | Needs fixture/route mock with `vehicleWarranty: null`. |
| `VEH-011` | P1 | Maintenance costs unavailable state. | Needs fixture/route mock with no cost estimates. |
| `VEH-013` | P1 | Update mileage validation/failure. | Add negative/invalid mileage and route-mocked server failure assertions. |
| `VEH-014` | P1 | Cannot close mileage modal while saving. | Route-mock slow mileage PATCH; assert Escape/backdrop/close blocked while saving. |
| `VEH-016` | P1 | Vehicle photo server failure. | Route-mock photo PATCH failure; assert modal remains open, error alert appears, previous image remains selected. |
| `VEH-017` | P2 | No photos available state. | Needs fixture/route mock with empty `availableImageUrls`. |
| `VEH-018` | P2 | Loading skeleton is accessible. | Route-mock slow dashboard response; assert loading status/skeleton then final content. |

### Maintenance tracking

| ID | Priority | Scenario | Implementation notes |
| --- | --- | --- | --- |
| `MAIN-006` | P1 | Maintenance complete validation. | Assert required date/mileage and invalid cost validation/server error while modal remains open. |
| `MAIN-008` | P1 | Empty upcoming state. | Needs fixture/route mock where all maintenance is completed; assert `All caught up`. |
| `MAIN-009` | P1 | Empty completed state. | Needs fixture/route mock with no completed maintenance; assert `Nothing logged yet`. |
| `MAIN-010` | P2 | Prevent modal close while maintenance mutation is submitting. | Route-mock slow mutation and assert close/backdrop/Escape blocked while pending. |

### Recall tracking

| ID | Priority | Scenario | Implementation notes |
| --- | --- | --- | --- |
| `REC-006` | P1 | Recall complete validation. | Assert missing date or invalid cost keeps modal open with validation/server error. |
| `REC-008` | P1 | Empty open recalls state. | Needs fixture/route mock without open recalls; assert `In the clear`. |
| `REC-009` | P1 | Empty completed recalls state. | Needs fixture/route mock without completed recalls; assert `Nothing logged yet`. |
| `REC-010` | P2 | Prevent modal close while recall mutation is submitting. | Route-mock slow mutation and assert close/backdrop/Escape blocked while pending. |

### Error states, resilience, and security-focused UI regressions

| ID | Priority | Scenario | Implementation notes |
| --- | --- | --- | --- |
| `ERR-005` | P1 | Network offline/server unavailable during page load. | Route abort/fail key page-load requests; assert app shell/navbar remain usable and visible load error appears. |
| `ERR-007` | P2 | Direct API data shape no-content/list normalization. | `GAR-008` covers `/api/vin` `204`; add focused coverage for empty array/populated array and other list endpoints if applicable. |

### Responsive and accessibility smoke checks

None of the dedicated `A11Y-*` smoke scenarios are implemented yet.

| ID | Priority | Scenario | Implementation notes |
| --- | --- | --- | --- |
| `A11Y-001` | P1 | Keyboard-only auth modal. | Assert focus trap/tab order, accessible labels, and Escape/close behavior when not submitting. |
| `A11Y-002` | P1 | Keyboard-only account drawer. | Assert keyboard path to Log out/change actions and close returns usable focus. |
| `A11Y-003` | P1 | Dialog close-blocking while submitting. | Parameterize for add vehicle, mileage, maintenance, recall, and account-change modals. |
| `A11Y-004` | P2 | Mobile landing/navigation. | Use mobile viewport; assert landing CTAs/navbar/account remain visible and tappable. |
| `A11Y-005` | P2 | Mobile garage cards and detail layout. | Assert no horizontal scroll and key controls/modals fit viewport. |
| `A11Y-006` | P2 | Toast announcements. | Assert success toasts appear after mutations and do not obscure required controls. |

## Partially implemented scenarios to expand

These IDs have tests, but the assertions do not yet cover the full expected assertions from the scenario document.

| ID | Current coverage | Missing/weak assertions |
| --- | --- | --- |
| `ACCT-001` | Signed-in drawer opens and shows name/email/actions. | Add explicit SMS/`Not provided` assertion if the drawer exposes it. |
| `ACCT-003` | Close button closes drawer. | Add backdrop close coverage from the original scenario. |
| `GAR-001` | Seeded cards, heading, add button, and mileage are visible. | Add trim and image/placeholder-state assertions. |
| `GAR-004` | Clicking card reaches the expected VIN URL. | Assert the matching vehicle detail title/content after navigation. |
| `GAR-006` | Delete removes card and shows toast. | Add assertion that dialog closes and deleted detail URL shows not found or redirects as expected. |
| `GAR-007` | Delete failure keeps dialog/card. | Add visible error assertion. |
| `VEH-001` | Detail title, VIN, mileage, trim label, and style render. | Add body, engine, transmission, and drive assertions where seeded. |
| `VEH-006` | Owner manual opens Toyota URL in a popup. | Add `window.opener`/safe external URL assertion if Playwright can inspect it reliably. |
| `VEH-008` | Warranty modal opens, shows `Basic`, and closes. | Add formatted coverage expiration/status assertions. |
| `VEH-010` | Maintenance costs modal opens and shows one item plus independent-shop label. | Add dealer/independent ranges and close-hidden assertion. |
| `VEH-012` | Mileage updates and is reset. | Add explicit page data refresh assertion if the UI has secondary mileage displays. |
| `VEH-015` | Photo modal saves alternate photo and resets. | Add selected/pressed state assertion and verify garage card image updates. |
| `MAIN-001` | Upcoming maintenance names render. | Add count, mileage group, and parts/labor/total summary assertions. |
| `MAIN-003` | Mark complete succeeds and completed section is visible. | Assert item moves from upcoming to completed and count updates. |
| `MAIN-004` | Mark incomplete succeeds and item is visible again. | Assert item is removed from completed and count updates. |
| `MAIN-007` | POST failure is covered. | Add DELETE/uncomplete failure coverage. |
| `REC-003` | Open recall modal shows campaign, remedy text, and Mark complete. | Add reported date, component, summary, consequence, and full detail assertions. |
| `REC-004` | Mark complete succeeds and completed section contains item. | Assert open/completed counts update. |
| `REC-005` | Mark incomplete succeeds and open recalls section is visible. | Assert item moves out of completed and open count updates. |
| `REC-007` | POST failure is covered. | Add DELETE/uncomplete failure coverage. |
| `ERR-003` | Test observes CSRF and logout requests. | Current assertion compares request URL strings, not actual request chronology. Record request event order/timestamps to prove CSRF happened before mutation. |
| `ERR-006` | Refresh on `/home` rehydrates. | Add refresh/cold-load coverage for `/vehicles/:vin`. |

## Additional beneficial regression scenarios not currently in the scenario document

These are recommended additions after the original plan is completed or while adding nearby tests.

| Proposed ID | Priority | Scenario | Why it is valuable |
| --- | --- | --- | --- |
| `ADD-001` | P0 | Auth tokens are not stored in `localStorage`/`sessionStorage` after login. | Protects the cookie-based auth model and guards against regressions back to bearer-token storage. |
| `ADD-002` | P1 | Signed-in user visits `/sign-in` or `/sign-up`. | Assert the app does not show an auth modal over an authenticated session and routes/returns to the intended signed-in state. |
| `ADD-003` | P1 | Direct cold deep link to `/vehicles/:vin` with a valid cookie. | Complements `/home` hydration tests and verifies shared/bookmarked vehicle links work without first visiting the garage. |
| `ADD-004` | P1 | Unverified home does not issue `GET /api/vin`. | `GAR-010` checks UI state; this network-level assertion protects the project-specific 403/unverified behavior. |
| `ADD-005` | P1 | Duplicate-submit prevention for high-risk forms. | Route-mock slow sign-in, sign-up, add-vehicle, and account-change requests; assert only one request is sent and controls show pending state. |
| `ADD-006` | P2 | Browser back/forward behavior with routed auth modals. | Verify `/sign-in` ↔ `/sign-up` modal history and closing/back navigation keep the landing page usable. |
| `ADD-007` | P2 | Bootstrap splash is replaced after app initialization. | Protects the static first-paint loading screen from getting stuck after JS/app-initializer requests complete. |
| `ADD-008` | P2 | Multi-tab logout/session revocation smoke. | Login in two contexts/tabs, log out or change password in one, and assert the other cannot continue protected mutations after refresh/hydration. |
