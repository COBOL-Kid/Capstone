# Playwright Regression Test Coverage Gaps

Reviewed against `PLAYWRIGHT_REGRESSION_TEST_SCENARIOS.md` and the current specs under `e2e/tests/` (107 passing scenarios as of the E2E gap-coverage work).

## Review summary

- Public page/navigation scenarios `PUB-001` through `PUB-008` are implemented.
- Core garage, auth, account-change, vehicle onboarding, vehicle detail, maintenance, recall, and resilience scenarios through P0/P1 are implemented, including destructive flows (`ACCT-009`, `AUTH-010`, `ACCT-004`–`007`, `ACCT-011`/`012`).
- Dev E2E email stub (`E2E_STUB_EMAIL`, `E2E_FIXED_VERIFICATION_CODE`) enables deterministic verification codes for signup and account-change specs when the backend runs with those env vars (see `e2e/README.md`).
- `VEH-003` (not-found vehicle) and `VEH-004` (dashboard load failure) are implemented and passing in `e2e/tests/vehicle-detail/`.
- Remaining gaps are mostly P2 (rate limits, a11y smoke, mobile layout, multi-tab session) plus a few optional assertion expansions.

## Remaining missing scenarios

### Authentication, session, and CSRF

| ID | Priority | Scenario | Notes |
| --- | --- | --- | --- |
| `AUTH-015` | P2 | Login rate-limit message. | Keep isolated/outside default suite; avoid poisoning shared IP rate-limit state. |
| `AUTH-016` | P2 | Expired/invalid session handling. | Simulate invalid/corrupt cookie or expired session; assert redirect to sign-in. |

### Account drawer and account management

| ID | Priority | Scenario | Notes |
| --- | --- | --- | --- |
| `ACCT-014` | P2 | Account deletion flow. | Backend endpoint exists; implement after UI is available. |

### Vehicle onboarding

| ID | Priority | Scenario | Notes |
| --- | --- | --- | --- |
| `VIN-012` | P2 | VIN input normalization. | Define expected lowercase/whitespace behavior, then assert UI/backend result. |

### Vehicle detail page

| ID | Priority | Scenario | Notes |
| --- | --- | --- | --- |
| `VEH-017` | P2 | No photos available state. | Route-mock `availableImageUrls: []`. |
| `VEH-018` | P2 | Loading skeleton accessibility. | Slow dashboard mock; assert loading status then final content. |

### Maintenance and recall tracking

| ID | Priority | Scenario | Notes |
| --- | --- | --- | --- |
| `MAIN-010` | P2 | Prevent modal close while maintenance mutation is submitting. | Slow POST/PATCH; assert Escape/backdrop/close blocked while pending. |
| `REC-010` | P2 | Prevent modal close while recall mutation is submitting. | Same pattern as `MAIN-010`. |

### Error states and resilience

| ID | Priority | Scenario | Notes |
| --- | --- | --- | --- |
| `ERR-007` | P2 | Direct API list normalization. | `GAR-008` covers `/api/vin` 204; add focused coverage for other list endpoints if needed. |

### Responsive and accessibility smoke checks

None of the dedicated `A11Y-*` scenarios are implemented yet.

| ID | Priority | Scenario | Notes |
| --- | --- | --- | --- |
| `A11Y-001` | P1 | Keyboard-only auth modal. | Focus trap, labels, Escape/close when not submitting. |
| `A11Y-002` | P1 | Keyboard-only account drawer. | Keyboard path to Log out/change actions. |
| `A11Y-003` | P1 | Dialog close-blocking while submitting. | Parameterize add vehicle, mileage, maintenance, recall, account-change modals. |
| `A11Y-004` | P2 | Mobile landing/navigation. | Mobile viewport smoke. |
| `A11Y-005` | P2 | Mobile garage cards and detail layout. | No horizontal scroll; modals fit viewport. |
| `A11Y-006` | P2 | Toast announcements. | Success toasts after mutations. |

## Optional assertion expansions (lower priority)

These IDs have passing tests; the scenario document lists additional assertions that could still be added.

| ID | Possible expansion |
| --- | --- |
| `VEH-006` | `window.opener` / safe external URL check on owner-manual popup |
| `VEH-008` | Formatted warranty expiration/status in modal |
| `VEH-015` | Selected photo state + garage card image update |
| `MAIN-003` / `MAIN-004` | Explicit upcoming/completed count transitions |
| `REC-003` | Full recall detail fields (date, component, consequence) |
| `REC-004` / `REC-005` | Open/completed count updates |

## Additional beneficial scenarios (not in scenario document)

| Proposed ID | Priority | Status |
| --- | --- | --- |
| `ADD-001` | P0 | Implemented (`session.spec.ts`) |
| `ADD-002` | P1 | Implemented (`auth-routing.spec.ts`) |
| `ADD-003` | P1 | Implemented (`session.spec.ts`) |
| `ADD-004` | P1 | Implemented (`unverified.spec.ts`) |
| `ADD-005` | P1 | Implemented (`login.spec.ts`) |
| `ADD-006` | P2 | Not implemented — browser back/forward with auth modals |
| `ADD-007` | P2 | Not implemented — bootstrap splash replaced after init |
| `ADD-008` | P2 | Not implemented — multi-tab logout/password-change smoke |
