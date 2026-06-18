# Playwright Regression Test Scenarios

This document outlines the browser-level regression coverage that should be implemented for Honest Car using Playwright. The repository already contains a Playwright project in `e2e/`; these scenarios are intended to expand that suite rather than introduce a separate test harness.

## Current Playwright coverage

Existing tests in `e2e/tests/` cover the first smoke layer:

| Area | Existing coverage |
| --- | --- |
| Landing | `/` shows the Sign In entry point. |
| Auth guard | Unauthenticated `/home` redirects to `/sign-in` and opens the auth dialog. |
| Login | UI sign-in with the seeded user reaches `/home`. |
| Garage | Authenticated user sees seeded Toyota Camry and Honda Civic vehicle cards. |

Keep these tests, but expand assertions where noted below.

## Test data and environment assumptions

- Run against the existing local stack described in `e2e/README.md`:
  - Backend: `Server` on `http://localhost:8080` with `SPRING_PROFILES_ACTIVE=dev`.
  - Frontend: `Client` on `http://localhost:4200` with the Angular proxy.
  - E2E database seed: `e2e/scripts/seed-e2e-data.sql`.
- Reuse the seeded verified user where possible:
  - Email: `test.user@example.com`
  - Password: `Password123!`
  - Vehicles:
    - `4T1C11AK5LU123456` — `2020 Toyota Camry`
    - `2HGFC2F59JH543210` — `2018 Honda Civic`
- Add additional deterministic seed fixtures before implementing the full suite:
  - An unverified user with no vehicles.
  - A verified user with an empty garage.
  - A user/vehicle fixture with no warranty, no maintenance-cost estimates, no owner manual, and no photos.
  - Optional throwaway user(s) for destructive account, password, and email-change tests.
- Prefer API helpers for setup/teardown and UI interactions for the behavior under test.
- For provider-dependent vehicle onboarding, avoid live external calls in the default regression suite. Use seeded data or Playwright route mocks unless a separate live-smoke suite is explicitly desired.

## Priority guide

| Priority | Meaning |
| --- | --- |
| P0 | Critical user journeys and security/session behavior; should run in the default regression suite. |
| P1 | Important product workflows that can run in default or nightly suites depending on runtime. |
| P2 | Edge cases, failure states, responsive/accessibility checks, and live-provider smoke tests. |

## Public pages and navigation

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| PUB-001 | P0 | Landing page loads at `/`. | Brand/title text, tagline, Sign Up and Sign In links are visible. | Expand existing `smoke/landing.spec.ts`. |
| PUB-002 | P0 | `/sign-in` deep link opens the sign-in modal. | URL is `/sign-in`; dialog heading is `Sign In`; Email and Password fields are focused/usable. | Useful for shared links and auth redirects. |
| PUB-003 | P0 | `/sign-up` deep link opens the sign-up modal. | URL is `/sign-up`; dialog heading is `Sign Up`; first name, last name, email, and password fields are visible. |  |
| PUB-004 | P1 | Auth modal mode switching works. | From Sign In, switch to Sign Up and back without losing modal accessibility or routing behavior. | Cover both landing CTA links and in-dialog alternate action. |
| PUB-005 | P1 | Closing auth modal from deep-linked auth route returns to landing state. | Close button/backdrop hides dialog; URL returns to `/` or non-modal landing state as implemented. |  |
| PUB-006 | P1 | Our Services page renders public content. | `/our-services` shows Maintenance, Recalls, Warranty cards and support email `support@honest-car.co`. |  |
| PUB-007 | P1 | Navbar public navigation works. | Brand/Home navigates to `/`; Our Services navigates to `/our-services`; active link styling updates. |  |
| PUB-008 | P2 | Unknown route redirects safely. | Visiting an invalid path redirects to `/` and app remains usable. |  |

## Authentication, session, and CSRF

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| AUTH-001 | P0 | Successful sign-in via UI. | Submit valid seeded credentials; redirected to `/home`; `My Vehicles` visible; Account drawer reflects signed-in user. | Existing `auth/login.spec.ts` should be expanded. |
| AUTH-002 | P0 | Invalid sign-in credentials show an error and do not authenticate. | Error alert is visible; user remains on sign-in dialog; no garage content is shown. | Use a non-seeded password. Beware login rate limiting. |
| AUTH-003 | P0 | Auth guard protects `/home`. | Unauthenticated visit redirects to `/sign-in`; sign-in dialog is visible. | Existing smoke test. |
| AUTH-004 | P0 | Auth guard protects `/vehicles/:vin`. | Unauthenticated visit to a seeded VIN redirects to `/sign-in`; after signing in, user can navigate to the vehicle detail page. |  |
| AUTH-005 | P0 | Logout clears the session and garage state. | Click Account → Log out; redirected/returned to public state; `/home` again redirects to `/sign-in`; previously visible vehicle cards disappear. | Confirms cookie-backed logout behavior. |
| AUTH-006 | P0 | Cold-load session hydration works from stored cookies. | Login, create a new browser page/context with storage state, visit `/home`; user remains signed in without `localStorage` token assumptions. | Important because auth uses HttpOnly cookies. |
| AUTH-007 | P0 | Mutating requests include XSRF credentials through Angular `HttpClient`. | UI mutations such as mileage update or logout succeed after bootstrap. | Do not use app `fetch` in tests; verify through behavior. |
| AUTH-008 | P1 | Registration form validation. | Required fields, invalid email, and weak password show inline validation; submit is blocked or shows expected errors. | Playwright should cover one representative UI validation path; keep exhaustive validation in unit tests. |
| AUTH-009 | P1 | Duplicate registration email returns friendly error. | Attempt sign-up with `test.user@example.com`; alert says an account already exists. | Does not require email delivery. |
| AUTH-010 | P1 | Successful sign-up enters unverified account flow. | Register a throwaway user; redirected to `/home`; verification banner appears; Add Vehicle is disabled. | Requires Mailjet/dev email strategy or backend test hook/seed. |
| AUTH-011 | P1 | Unverified login requiring email verification challenge. | Sign in as unverified user; verification code step appears; Back to sign in and Resend code work. | Use seed/test hook for known code if verifying success. |
| AUTH-012 | P1 | Email verification from home banner succeeds. | Unverified user sees banner, opens `Enter code`, submits valid code, banner disappears, Add Vehicle becomes enabled, vehicles API loads. | Needs deterministic verification code setup. |
| AUTH-013 | P1 | Email verification invalid code error. | Submit malformed/wrong code; visible error; user remains unverified; Add Vehicle remains disabled. |  |
| AUTH-014 | P1 | Resend email verification disables controls while pending. | Click resend; button shows pending text and cannot double-submit; error/success state is handled. | Can use route mocking for slow/failure states. |
| AUTH-015 | P2 | Login rate-limit message. | Repeated invalid logins eventually show the expected lockout/rate-limit message. | Keep outside default suite or isolate by IP/user to avoid poisoning other tests. |
| AUTH-016 | P2 | Expired/invalid session handling. | With an invalid/expired cookie, `/home` redirects to sign-in and Account drawer shows unauthenticated state. | Can be simulated by clearing or corrupting storage state. |

## Account drawer and account management

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| ACCT-001 | P0 | Account drawer opens for signed-in user. | Account button opens drawer; name, email, SMS, member since, last updated, Log out, Change Email/SMS/Password are visible. |  |
| ACCT-002 | P1 | Account drawer opens for signed-out user. | Drawer shows sign-in/sign-up actions instead of profile details. |  |
| ACCT-003 | P1 | Account drawer can be closed by close button and backdrop. | Drawer hides and focus/navigation remains usable. |  |
| ACCT-004 | P1 | Change SMS happy path. | Open Change SMS, submit a valid new number, enter verification code, drawer refreshes with new SMS. | Needs deterministic account-change code. |
| ACCT-005 | P1 | Remove SMS number. | Submit blank SMS value, verify code, drawer displays `Not provided`. |  |
| ACCT-006 | P1 | Change SMS validation. | Invalid phone characters or too-long input shows inline validation and does not start verification. |  |
| ACCT-007 | P1 | Change email happy path. | Submit new email, verify code, drawer updates email; session remains valid as designed. | Use throwaway account. |
| ACCT-008 | P1 | Change email duplicate/invalid errors. | Duplicate email and invalid email display friendly errors and remain in the modal. |  |
| ACCT-009 | P0 | Change password revokes session after verification. | Submit current/new password, verify code; user is signed out; `/home` redirects to sign-in; old session storage no longer works. | Project requirement: password change revokes refresh tokens and clears session. |
| ACCT-010 | P1 | Change password validation. | Wrong current password, weak new password, and mismatched confirmation show appropriate errors. |  |
| ACCT-011 | P1 | Pending account change resumes verification step. | Start a change, close/reopen drawer/modal or reload; pending change resumes at code entry. | Uses `/api/account/change-requests/pending`. |
| ACCT-012 | P1 | Resend account-change code. | Resend button disables while pending and displays errors if resend fails. | Can route-mock failure. |
| ACCT-013 | P2 | Account details load failure. | Route-mock `/api/account/me` failure and confirm drawer shows `Unable to load account details. Please try again.` |  |
| ACCT-014 | P2 | Account deletion flow, if/when UI is added. | Confirm password deletes account, clears cookies, and prevents future sign-in. | Backend endpoint exists; no current UI found. |

## Garage/home page

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| GAR-001 | P0 | Authenticated garage shows seeded vehicles. | `My Vehicles`, header add button, Toyota Camry and Honda Civic cards, mileage, trim, and image/placeholder state are visible. | Existing `garage/home.spec.ts`; expand assertions. |
| GAR-002 | P0 | Empty garage state. | Verified empty user sees `No vehicles yet`, descriptive copy, and the primary `Add a vehicle` button. Header `+` is not shown. | Matches project preference to keep empty-state Add button. |
| GAR-003 | P0 | Non-empty garage header add button. | Seeded user sees header `+`/`Add new vehicle`; empty-state add button is absent. |  |
| GAR-004 | P0 | Vehicle card navigation. | Clicking a vehicle card navigates to `/vehicles/:vin` and shows the matching vehicle detail title. |  |
| GAR-005 | P0 | Delete vehicle cancel path. | Click card delete; confirmation dialog opens; Cancel/close leaves vehicle card visible. |  |
| GAR-006 | P0 | Delete vehicle happy path. | Confirm delete; dialog closes; toast says vehicle removed; card disappears; detail URL for deleted VIN shows not found or redirects as expected. | Use throwaway/seed-reset data. |
| GAR-007 | P1 | Delete vehicle server failure. | Route-mock delete failure; dialog stays open and shows error; vehicle remains visible. |  |
| GAR-008 | P1 | Garage vehicles API `204 No Content` normalizes to empty state. | Route-mock `/api/vin` `204`; empty state renders without error. | Mirrors `normalizeListResponse`. |
| GAR-009 | P1 | Garage load failure. | Route-mock `/api/vin` `500`; shows `Failed to load vehicles.` |  |
| GAR-010 | P1 | Unverified user does not fetch/show vehicles. | Home shows verification banner; Add Vehicle disabled; garage does not show vehicle cards. | Confirms 403/unverified flow is not treated like logout. |

## Vehicle onboarding/add vehicle

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| VIN-001 | P0 | Add vehicle modal opens from empty garage and non-empty header. | Dialog heading `Add a vehicle`; VIN and Current Mileage inputs are visible. |  |
| VIN-002 | P0 | Add vehicle form validation. | Missing VIN, invalid VIN length/pattern, missing mileage, and negative mileage show inline errors. | Keep one concise UI validation test. |
| VIN-003 | P0 | Add existing seeded VIN to a verified empty user. | Submit a VIN/mileage; vehicle is added; toast appears; app navigates to `/vehicles/:vin`; garage later shows the card. | Prefer DB seed over live provider. |
| VIN-004 | P1 | Add vehicle duplicate association. | Adding a VIN already in the user’s garage returns a friendly error or no duplicate card, per backend behavior. | Confirm expected behavior before implementing. |
| VIN-005 | P1 | VIN not found/data unavailable error. | Submit unknown VIN; modal shows the friendly unavailable message. | Use route mocking or backend fixture. |
| VIN-006 | P1 | Onboarding backend failure. | Route-mock `POST /api/vin` `500`; modal remains open with `We couldn't load vehicle data...`; controls re-enable. |  |
| VIN-007 | P1 | Trim selection required flow. | `POST /api/vin` returns trim selection context; modal switches to Trim select; load trim options; select trim; Confirm completes add. | Use Playwright route mocks or deterministic backend fixture. |
| VIN-008 | P1 | Trim options loading failure. | Trim selection step shows load error and prevents confirm until options are available. |  |
| VIN-009 | P1 | Limited-data warning flow. | If backend returns limited-data context/state supported by modal, warning appears and `Continue anyways` resumes onboarding. | Template supports this step; verify current backend trigger. |
| VIN-010 | P1 | Cannot close add modal while submitting/loading trims. | During slow request, backdrop/Escape/close do not dismiss; controls show loading state. | Project convention for overlay modals. |
| VIN-011 | P2 | Unverified user cannot add vehicle. | Button disabled; direct route/API-driven attempt shows `Verify your email before adding vehicles...`. |  |
| VIN-012 | P2 | VIN input normalization. | Lowercase VIN input or whitespace is handled according to product expectations. | Confirm backend/frontend desired behavior. |

## Vehicle detail page

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| VEH-001 | P0 | Seeded vehicle detail loads. | `/vehicles/4T1C11AK5LU123456` shows `2020 Toyota Camry`, VIN, mileage, trim, style/body/engine/transmission/drive where seeded. |  |
| VEH-002 | P0 | Back to vehicles link. | Click `← Back to vehicles`; returns to `/home` with vehicle cards visible. |  |
| VEH-003 | P0 | Not-found vehicle detail. | Authenticated visit to valid-format VIN not owned by user shows `Vehicle not found.` |  |
| VEH-004 | P1 | Vehicle detail load failure. | Route-mock dashboard/detail failure; shows `Failed to load vehicle.` |  |
| VEH-005 | P0 | Maintenance/recalls toggle. | Default section is Maintenance; clicking Recalls shows open/completed recalls; toggling back restores Maintenance. |  |
| VEH-006 | P1 | Owner manual opens safe external URL. | Clicking `Owner's manual` opens a new page/tab to the seeded manual URL with no opener access. | Playwright can wait for popup. |
| VEH-007 | P1 | Owner manual unavailable state. | For fixture with no/suspicious manual URL, button is disabled and does not open a popup. | Validates safe URL guard. |
| VEH-008 | P1 | Warranty information modal. | Button opens warranty modal; coverages and formatted expiration/status are visible; close works. | Seeded Camry/Civic have warranty rows. |
| VEH-009 | P1 | Warranty unavailable state. | Fixture without warranty disables button and shows unavailable tooltip/title. |  |
| VEH-010 | P1 | Maintenance costs modal. | Button opens cost estimates; independent/dealer ranges are visible; close works. | Use seeded misc costs. |
| VEH-011 | P1 | Maintenance costs unavailable state. | Fixture without costs disables button and shows unavailable tooltip/title. |  |
| VEH-012 | P1 | Update mileage happy path. | Open Update Mileage; submit new mileage; dialog closes; toast appears; header mileage updates; page data refreshes. | Use throwaway or reset seed after test. |
| VEH-013 | P1 | Update mileage validation/failure. | Negative/invalid mileage or route-mocked server error shows alert and dialog remains open. |  |
| VEH-014 | P1 | Cannot close mileage modal while saving. | Slow `PATCH /api/vin/:vin/mileage`; close/backdrop disabled; after response modal closes. |  |
| VEH-015 | P1 | Vehicle photo modal happy path. | Open photo modal; selected photo has pressed/selected state; selecting another photo saves, modal closes, hero image and garage card update. |  |
| VEH-016 | P1 | Vehicle photo server failure. | Route-mock photo patch failure; modal remains open; error alert appears; previous selected image remains. |  |
| VEH-017 | P2 | No photos available state. | Fixture without photos shows no camera/change button or modal displays `No photos available.` | Depending current UI branch. |
| VEH-018 | P2 | Loading skeleton is accessible. | Slow route-mocked dashboard request shows loading status text and skeleton; final content replaces it. |  |

## Maintenance tracking

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| MAIN-001 | P0 | Upcoming maintenance section renders seeded data. | Shows `Upcoming maintenance`, count, mileage groups, service names, parts/labor/total summaries. | Camry seed includes upcoming 30k/45k after completed 5k/15k. |
| MAIN-002 | P1 | Show inspections toggle. | Inspection items are visible by default; unchecking hides inspection rows; checking shows them again. |  |
| MAIN-003 | P0 | Mark upcoming maintenance complete. | Open `Mark complete`, enter date/mileage/cost/notes, confirm; toast appears; item moves to Completed maintenance; count updates. |  |
| MAIN-004 | P0 | Mark completed maintenance incomplete. | Open Completed maintenance, select item, Mark incomplete; item moves back to Upcoming maintenance where applicable; toast appears. |  |
| MAIN-005 | P1 | Completed maintenance detail displays saved fields. | Completed modal shows description, due mileage, completed date/mileage, cost, and notes. |  |
| MAIN-006 | P1 | Maintenance complete validation. | Required date/mileage and invalid cost show validation or server error; modal remains open. |  |
| MAIN-007 | P1 | Maintenance mutation failure. | Route-mock `POST /api/maintenance/completed` or DELETE failure; alert appears; no list mutation occurs. |  |
| MAIN-008 | P1 | Empty upcoming state. | Fixture with all maintenance completed shows `All caught up`. |  |
| MAIN-009 | P1 | Empty completed state. | Fixture with no completed maintenance shows `Nothing logged yet`. |  |
| MAIN-010 | P2 | Prevent modal close while maintenance mutation is submitting. | Slow mutation disables close/backdrop and submit shows pending text. |  |

## Recall tracking

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| REC-001 | P0 | Open recalls section renders seeded data. | Recalls tab shows `Open recalls`, count, component and NHTSA campaign number. | Camry seed has one open recall and one completed recall. |
| REC-002 | P0 | Completed recalls section renders seeded data. | Expanding Completed recalls shows completed item and date. |  |
| REC-003 | P0 | Open recall detail modal. | Click open recall; modal shows campaign, reported date, component, summary, consequence, remedy, and Mark complete. |  |
| REC-004 | P0 | Mark recall complete. | Enter completed date, optional repair shop/cost/notes; confirm; toast appears; recall moves to Completed recalls; open count updates. |  |
| REC-005 | P0 | Mark completed recall incomplete. | Open completed recall, Mark incomplete; recall moves back to Open recalls; toast appears. |  |
| REC-006 | P1 | Recall complete validation. | Missing date or invalid cost keeps modal open and shows validation/server error. |  |
| REC-007 | P1 | Recall mutation failure. | Route-mock `POST /api/recall/completed` or DELETE failure; alert appears; lists do not change. |  |
| REC-008 | P1 | Empty open recalls state. | Fixture without open recalls shows `In the clear`. |  |
| REC-009 | P1 | Empty completed recalls state. | Fixture with no completed recalls shows `Nothing logged yet`. |  |
| REC-010 | P2 | Prevent modal close while recall mutation is submitting. | Slow mutation disables close/backdrop and submit shows pending text. |  |

## Error states, resilience, and security-focused UI regressions

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| ERR-001 | P0 | 401 from protected API clears session. | Route-mock `/api/account/me` or a protected request as 401; user is signed out and redirected to sign-in. | Aligns with `validateSession()` behavior. |
| ERR-002 | P1 | 403 account hydration does not wipe cookie-backed session. | Route-mock `/api/account/me` 403 after successful login; app does not clear session as if logout occurred. | Project-specific auth behavior. |
| ERR-003 | P1 | Global bootstrap obtains CSRF before mutations. | On first page load, `/api/auth/csrf` is requested before sign-in/logout/add/update mutation; subsequent mutation succeeds. | Can assert network order in a focused test. |
| ERR-004 | P1 | Generic 500 error body is user-friendly. | Route-mock auth or vehicle mutation `500`; UI displays friendly message, not raw stack trace. |  |
| ERR-005 | P1 | Network offline/server unavailable during page load. | Route-mock request failure; appropriate load error is visible and app shell/navbar remain usable. |  |
| ERR-006 | P2 | Browser refresh on protected routes. | Refresh `/home` and `/vehicles/:vin` with valid storage state; page rehydrates and renders content. |  |
| ERR-007 | P2 | Direct API data shape no-content/list normalization. | Route-mock list endpoint with `204`, empty array, and populated array; UI handles all without crash. |  |

## Responsive and accessibility smoke checks

These should be a small smoke suite, not exhaustive visual testing.

| ID | Priority | Scenario | Expected assertions | Notes |
| --- | --- | --- | --- | --- |
| A11Y-001 | P1 | Keyboard-only auth modal. | Tab order stays inside dialog; Escape/backdrop/close behavior works when not submitting; labels are accessible by name. |  |
| A11Y-002 | P1 | Keyboard-only account drawer. | Account button opens drawer; focus can reach Log out and change actions; close returns usable focus. |  |
| A11Y-003 | P1 | Dialog close-blocking while submitting. | For add vehicle, mileage, maintenance, recall, account-change modals: backdrop/Escape/close are blocked only while pending. | Can be parameterized. |
| A11Y-004 | P2 | Mobile landing/navigation. | At a mobile viewport, landing CTAs and navbar links/account button remain visible and tappable. | Current navbar has no separate mobile menu. |
| A11Y-005 | P2 | Mobile garage cards and detail layout. | Vehicle cards, Add button, detail sections, and modals fit viewport without horizontal scroll. |  |
| A11Y-006 | P2 | Toast announcements. | Success toasts appear after add/delete/mileage/maintenance/recall/photo mutations and do not obscure required controls. |  |

## Suggested implementation order

1. **Stabilize fixtures and helpers**
   - Add seed users for verified empty garage, unverified account, and destructive-change tests.
   - Add API helpers for login, CSRF extraction, user creation/reset, and test data cleanup.
   - Keep `storageState` files separate for verified, unverified, and empty-garage users.

2. **Expand P0 default regression suite**
   - Auth guard/login/logout/session hydration.
   - Garage empty and seeded states.
   - Vehicle detail load, navigation, maintenance/recall tab switching.
   - Mileage update, maintenance complete/uncomplete, recall complete/uncomplete.

3. **Add P1 workflow coverage**
   - Account changes with deterministic verification codes.
   - Add-vehicle happy path and key error paths.
   - Warranty, maintenance costs, owner manual, photo selection.
   - Route-mocked backend failures.

4. **Add P2 smoke/nightly coverage**
   - Rate-limit behavior.
   - Responsive smoke tests.
   - Provider/live onboarding smoke tests only when real credentials and stable test VINs are available.

## Out of scope for Playwright default regression

- Exhaustive DTO validation combinations already covered better by Angular unit tests and Spring integration tests.
- Repository query correctness and Flyway schema validation; keep these in Gradle tests.
- Live third-party provider coverage in every regression run; use a separately tagged live suite if needed.
