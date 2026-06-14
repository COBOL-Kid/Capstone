# Missing Edge-Case Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close test gaps exposed by the registration HTTP 500 bug — especially custom JPA `@Query` / `@Modifying` methods and auth/data-deletion flows that are only covered by mocked unit tests today.

**Architecture:** Add a thin shared integration-test harness (H2 in PostgreSQL mode, valid Base64 JWT secret, stubbed Mailjet), then layer repository integration tests (fast, isolated), service integration tests (multi-repository flows), and a small set of MockMvc / Angular interceptor tests for HTTP edge cases. Finish with a static architecture test that fails CI if someone reintroduces `@Modifying` + `SELECT` or primitive `boolean` + nullable scalar `@Query` patterns.

**Tech Stack:** Spring Boot 4.1 / JUnit 5 / H2 (`MODE=PostgreSQL`) / MockitoBean / MockMvc / Angular 22 + Vitest

---

## Findings (why this plan exists)

The registration bug (`existsByUserEmail` returning `null` for a primitive `boolean`, `deleteByUser` using `SELECT` with `@Modifying`) shipped because:

| Area | Existing tests | Why they missed the bug |
|------|----------------|-------------------------|
| `AuthenticationServiceTest` | Mocks `UserRepositoryJPA` | Never executes real JPQL |
| `EmailVerificationServiceTest` | Mocks `EmailVerificationCodeRepositoryJPA` | Never executes real deletes |
| `AccountChangeServiceTest` | Mocks `changeRequestRepository.deleteByUser` | Never executes real delete |
| `UnverifiedAccountCleanupJobTest` | Mocks `findByEmailVerifiedFalseAndCreatedAtBefore` | Never executes custom list query |
| No `UserDeletionService` test | — | Seven `@Modifying` repository calls untested |
| `Server/src/test/resources/application.properties` | `security.jwt.secret=test-jwt-secret-key-for-unit-tests-only` | Not valid Base64; breaks any integration test that issues real JWTs unless overridden |

**Already added on branch `cursor/fix-register-500-f3ab` (baseline, do not redo):**

- `Server/src/test/java/com/capstone/data/UserRepositoryJPATest.java`
- `Server/src/test/java/com/capstone/data/EmailVerificationCodeRepositoryJPATest.java`
- `Server/src/test/java/com/capstone/data/AccountChangeRequestRepositoryJPATest.java`
- `Server/src/test/java/com/capstone/authentication/RegistrationIntegrationTest.java`

**Still untested high-risk surfaces:**

1. `RefreshTokenRepositoryJPA` — `findByToken`, `deleteByUser`, `deleteByToken`, `deleteByIdReturning`
2. `UserRepositoryJPA.findByEmailVerifiedFalseAndCreatedAtBefore` — drives scheduled cleanup
3. `UserVinRepositoryJPA` — `findVinNumbersForUser`, `deleteForUserVin`, `deleteAllForUserId`
4. `VinRepositoryJPA.deleteOrphanedVins` — must not delete VINs still linked to another user
5. `CompletedMaintenanceRepositoryJPA` / `CompletedRecallRepositoryJPA` — bulk deletes by user / user+vin
6. `UserDeletionService.deleteUserAndRelatedData` — orchestrates all of the above
7. Auth refresh-token rotation with real DB (`AuthenticationService.refreshToken`)
8. HTTP layer: duplicate registration → 409, validation → 400, email delivery failure → 503
9. Angular: `unauthorized.interceptor.ts`, `credentials.interceptor.ts`, registration 500/503 error surfacing
10. Static guard against broken repository query patterns

---

## File map

| File | Responsibility |
|------|----------------|
| `Server/src/test/java/com/capstone/support/IntegrationTestProperties.java` | Shared `@SpringBootTest` property strings (H2 URL factory, JWT secret, mailjet keys) |
| `Server/src/test/java/com/capstone/support/MailjetTestSupport.java` | Reusable Mailjet success response stub for `@MockitoBean MailjetClient` |
| `Server/src/test/java/com/capstone/data/RefreshTokenRepositoryJPATest.java` | Refresh token custom queries |
| `Server/src/test/java/com/capstone/data/UserRepositoryCleanupQueryTest.java` | `findByEmailVerifiedFalseAndCreatedAtBefore` |
| `Server/src/test/java/com/capstone/data/UserVinRepositoryJPATest.java` | UserVin find/delete queries |
| `Server/src/test/java/com/capstone/data/VinRepositoryJPATest.java` | Orphan VIN deletion edge case |
| `Server/src/test/java/com/capstone/data/CompletedMaintenanceRepositoryJPATest.java` | Bulk maintenance delete |
| `Server/src/test/java/com/capstone/data/CompletedRecallRepositoryJPATest.java` | Bulk recall delete |
| `Server/src/test/java/com/capstone/domain/UserDeletionIntegrationTest.java` | Full user teardown |
| `Server/src/test/java/com/capstone/authentication/RefreshTokenIntegrationTest.java` | Token rotation with real repos |
| `Server/src/test/java/com/capstone/authentication/AuthenticationControllerIntegrationTest.java` | MockMvc HTTP status codes |
| `Server/src/test/java/com/capstone/data/RepositoryQueryArchitectureTest.java` | Static anti-pattern guard |
| `Server/src/test/resources/application.properties` | Fix JWT secret to valid Base64 |
| `Client/src/app/core/auth/unauthorized.interceptor.spec.ts` | 401 session clearing rules |
| `Client/src/app/core/http/credentials.interceptor.spec.ts` | `withCredentials` on same-origin API calls |
| `Client/src/app/core/auth/auth.service.spec.ts` | Extend with 500/503 registration errors |

---

### Task 1: Shared integration test harness

**Files:**
- Create: `Server/src/test/java/com/capstone/support/IntegrationTestProperties.java`
- Create: `Server/src/test/java/com/capstone/support/MailjetTestSupport.java`
- Modify: `Server/src/test/resources/application.properties:12`

- [ ] **Step 1: Fix global test JWT secret**

In `Server/src/test/resources/application.properties`, replace line 12:

```properties
security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
```

This is Base64 for `0123456789abcdef0123456789abcdef` (same as `JwtServiceTest`).

- [ ] **Step 2: Create property helper**

```java
package com.capstone.support;

public final class IntegrationTestProperties {

  public static final String JWT_SECRET =
      "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

  private IntegrationTestProperties() {}

  public static String[] h2CreateDrop(String dbName) {
    return new String[] {
      "spring.profiles.active=test",
      "spring.datasource.url=jdbc:h2:mem:" + dbName
          + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.flyway.enabled=false",
      "security.jwt.secret=" + JWT_SECRET,
      "security.jwt.expiration-minutes=15",
      "security.jwt.refresh-expiration-days=7",
      "security.cookies.secure=false",
      "mailjet.enabled=true",
      "mailjet.api-key-public=test-public",
      "mailjet.api-key-private=test-private",
      "mailjet.from-email=sender@example.com",
      "mailjet.from-name=Honest Car"
    };
  }

  public static String[] h2FlywaySeed(String dbName) {
    return new String[] {
      "spring.profiles.active=test",
      "spring.datasource.url=jdbc:h2:mem:" + dbName
          + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.flyway.enabled=true",
      "spring.flyway.target=1",
      "security.jwt.secret=" + JWT_SECRET,
      "security.jwt.expiration-minutes=15",
      "security.jwt.refresh-expiration-days=7",
      "security.cookies.secure=false",
      "mailjet.enabled=false"
    };
  }
}
```

- [ ] **Step 3: Create Mailjet stub helper**

```java
package com.capstone.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

public final class MailjetTestSupport {

  private MailjetTestSupport() {}

  public static void stubSuccessfulSend(MailjetClient mailjetClient) throws MailjetException {
    MailjetResponse response = org.mockito.Mockito.mock(MailjetResponse.class);
    when(response.getStatus()).thenReturn(HttpStatus.OK.value());
    when(response.getData()).thenReturn(successResponseData());
    when(mailjetClient.post(any())).thenReturn(response);
  }

  private static JSONArray successResponseData() {
    return new JSONArray()
        .put(
            new JSONObject()
                .put("Status", "success")
                .put(
                    "To",
                    new JSONArray()
                        .put(
                            new JSONObject()
                                .put("Email", "test@example.com")
                                .put("MessageUUID", "123")
                                .put("MessageID", 456)
                                .put("MessageHref", "https://example.com/message/456"))));
  }
}
```

- [ ] **Step 4: Run existing auth/data tests**

Run: `cd Server && ./gradlew test --tests "com.capstone.authentication.RegistrationIntegrationTest" --tests "com.capstone.data.*" --tests "com.capstone.authentication.JwtServiceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add Server/src/test/java/com/capstone/support/IntegrationTestProperties.java \
        Server/src/test/java/com/capstone/support/MailjetTestSupport.java \
        Server/src/test/resources/application.properties
git commit -m "test: add shared integration test harness and fix JWT test secret"
```

---

### Task 2: RefreshTokenRepositoryJPATest

**Files:**
- Create: `Server/src/test/java/com/capstone/data/RefreshTokenRepositoryJPATest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.RefreshToken;
import com.capstone.models.Role;
import com.capstone.models.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.capstone.support.IntegrationTestProperties;

@SpringBootTest(properties = IntegrationTestProperties.h2CreateDrop("refresh-token-repo"))
class RefreshTokenRepositoryJPATest {

  @Autowired private RefreshTokenRepositoryJPA refreshTokenRepository;
  @Autowired private UserRepositoryJPA userRepository;

  @Test
  void findByTokenReturnsMatchingRefreshToken() {
    User user = saveUser("refresh-find-" + System.nanoTime() + "@example.com");
    RefreshToken token = saveToken(user, "token-find-me");

    assertTrue(refreshTokenRepository.findByToken("token-find-me").isPresent());
    assertEquals(user.getUserId(), refreshTokenRepository.findByToken("token-find-me").orElseThrow().getUser().getUserId());
  }

  @Test
  @Transactional
  void deleteByUserRemovesOnlyThatUsersTokens() {
    User owner = saveUser("refresh-owner-" + System.nanoTime() + "@example.com");
    User other = saveUser("refresh-other-" + System.nanoTime() + "@example.com");
    saveToken(owner, "owner-token");
    saveToken(other, "other-token");

    refreshTokenRepository.deleteByUser(owner);
    refreshTokenRepository.flush();

    assertFalse(refreshTokenRepository.findByToken("owner-token").isPresent());
    assertTrue(refreshTokenRepository.findByToken("other-token").isPresent());
  }

  @Test
  @Transactional
  void deleteByIdReturningReportsWhetherRowWasDeleted() {
    User user = saveUser("refresh-delete-id-" + System.nanoTime() + "@example.com");
    RefreshToken token = saveToken(user, "rotate-me");
    Long id = token.getId();

    assertEquals(1, refreshTokenRepository.deleteByIdReturning(id));
    assertEquals(0, refreshTokenRepository.deleteByIdReturning(id));
  }

  private User saveUser(String email) {
    User user = new User();
    user.setUserEmail(email);
    user.setUserPw("encoded-secret");
    user.setRole(Role.USER);
    return userRepository.saveAndFlush(user);
  }

  private RefreshToken saveToken(User user, String tokenValue) {
    RefreshToken token = new RefreshToken();
    token.setUser(user);
    token.setToken(tokenValue);
    token.setExpiryDate(Instant.now().plusSeconds(3600));
    return refreshTokenRepository.saveAndFlush(token);
  }
}
```

- [ ] **Step 2: Run test to verify it passes (greenfield)**

Run: `cd Server && ./gradlew test --tests "com.capstone.data.RefreshTokenRepositoryJPATest"`
Expected: PASS (queries are already correct; this guards regression)

- [ ] **Step 3: Commit**

```bash
git add Server/src/test/java/com/capstone/data/RefreshTokenRepositoryJPATest.java
git commit -m "test: cover RefreshTokenRepositoryJPA custom queries"
```

---

### Task 3: UserRepository cleanup query test

**Files:**
- Create: `Server/src/test/java/com/capstone/data/UserRepositoryCleanupQueryTest.java`

- [ ] **Step 1: Write the test**

```java
package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.Role;
import com.capstone.models.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.capstone.support.IntegrationTestProperties;

@SpringBootTest(properties = IntegrationTestProperties.h2CreateDrop("user-cleanup-query"))
class UserRepositoryCleanupQueryTest {

  @Autowired private UserRepositoryJPA userRepository;

  @Test
  void findByEmailVerifiedFalseAndCreatedAtBeforeReturnsOnlyStaleUnverifiedUsers() {
    Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);

    User stale = saveUser("stale-" + System.nanoTime() + "@example.com", false, cutoff.minus(1, ChronoUnit.HOURS));
    User fresh = saveUser("fresh-" + System.nanoTime() + "@example.com", false, Instant.now());
    User verified = saveUser("verified-" + System.nanoTime() + "@example.com", true, cutoff.minus(2, ChronoUnit.HOURS));

    var matches = userRepository.findByEmailVerifiedFalseAndCreatedAtBefore(cutoff);

    assertEquals(1, matches.size());
    assertEquals(stale.getUserId(), matches.getFirst().getUserId());
    assertTrue(matches.stream().noneMatch(user -> user.getUserId().equals(fresh.getUserId())));
    assertTrue(matches.stream().noneMatch(user -> user.getUserId().equals(verified.getUserId())));
  }

  private User saveUser(String email, boolean verified, Instant createdAt) {
    User user = new User();
    user.setUserEmail(email);
    user.setUserPw("encoded-secret");
    user.setRole(Role.USER);
    user.setEmailVerified(verified);
    user.setCreatedAt(createdAt);
    user.setUpdatedAt(createdAt);
    return userRepository.saveAndFlush(user);
  }
}
```

- [ ] **Step 2: Run test**

Run: `cd Server && ./gradlew test --tests "com.capstone.data.UserRepositoryCleanupQueryTest"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add Server/src/test/java/com/capstone/data/UserRepositoryCleanupQueryTest.java
git commit -m "test: cover stale unverified user cleanup query"
```

---

### Task 4: UserVinRepositoryJPATest + VinRepositoryJPATest

**Files:**
- Create: `Server/src/test/java/com/capstone/data/UserVinRepositoryJPATest.java`
- Create: `Server/src/test/java/com/capstone/data/VinRepositoryJPATest.java`

Use Flyway seed DB (`IntegrationTestProperties.h2FlywaySeed("user-vin-repo")`) so vehicle tables exist. Seed user id `1` from `Server/src/test/resources/db/V1_inital_schema.sql` owns VIN `4T1C11AK5LU123456`.

- [ ] **Step 1: Write UserVinRepositoryJPATest**

```java
package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.capstone.support.IntegrationTestProperties;

@SpringBootTest(properties = IntegrationTestProperties.h2FlywaySeed("user-vin-repo"))
class UserVinRepositoryJPATest {

  private static final long SEED_USER_ID = 1L;
  private static final String CAMRY_VIN = "4T1C11AK5LU123456";
  private static final String CIVIC_VIN = "2HGFC2F59JH543210";

  @Autowired private UserVinRepositoryJPA userVinRepository;

  @Test
  void findVinNumbersForUserReturnsOnlyThatUsersVins() {
    var vins = userVinRepository.findVinNumbersForUser(SEED_USER_ID);

    assertTrue(vins.contains(CAMRY_VIN));
    assertTrue(vins.contains(CIVIC_VIN));
    assertEquals(2, vins.size());
  }

  @Test
  @Transactional
  void deleteForUserVinRemovesOnlyMatchingAssociation() {
    userVinRepository.deleteForUserVin(SEED_USER_ID, CIVIC_VIN);
    userVinRepository.flush();

    assertFalse(userVinRepository.findByUserUserIdAndVinVin(SEED_USER_ID, CIVIC_VIN).isPresent());
    assertTrue(userVinRepository.findByUserUserIdAndVinVin(SEED_USER_ID, CAMRY_VIN).isPresent());
  }
}
```

- [ ] **Step 2: Write VinRepositoryJPATest (orphan edge case)**

```java
package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.UserVinId;
import com.capstone.models.Vin;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.capstone.support.IntegrationTestProperties;

@SpringBootTest(properties = IntegrationTestProperties.h2FlywaySeed("vin-orphan-repo"))
class VinRepositoryJPATest {

  private static final String SHARED_VIN = "4T1C11AK5LU123456";

  @Autowired private VinRepositoryJPA vinRepository;
  @Autowired private UserVinRepositoryJPA userVinRepository;
  @Autowired private UserRepositoryJPA userRepository;

  @Test
  @Transactional
  void deleteOrphanedVinsSkipsVinsStillLinkedToAnotherUser() {
    User secondUser = saveUser("second-owner-" + System.nanoTime() + "@example.com");
  Vin vin = vinRepository.findById(SHARED_VIN).orElseThrow();
    userVinRepository.saveAndFlush(link(secondUser, vin));

    vinRepository.deleteOrphanedVins(List.of(SHARED_VIN));
    vinRepository.flush();

    assertTrue(vinRepository.findById(SHARED_VIN).isPresent());
  }

  @Test
  @Transactional
  void deleteOrphanedVinsRemovesVinWhenNoUserLinksRemain() {
    String orphanVin = "1HGCM82633A999999";
    Vin vin = vinRepository.findById(SHARED_VIN).orElseThrow();
    vin.setVin(orphanVin);
    vinRepository.saveAndFlush(vin);
    userVinRepository.deleteAllForUserId(1L);
    userVinRepository.flush();

    vinRepository.deleteOrphanedVins(List.of(orphanVin));
    vinRepository.flush();

    assertFalse(vinRepository.findById(orphanVin).isPresent());
  }

  private static User saveUser(String email) {
    User user = new User();
    user.setUserEmail(email);
    user.setUserPw("encoded-secret");
    user.setRole(Role.USER);
    return user;
  }

  private UserVin link(User user, Vin vin) {
    UserVin association = new UserVin();
    association.setId(new UserVinId(user.getUserId(), vin.getVin()));
    association.setUser(user);
    association.setVin(vin);
    association.setCurrentMileage(10_000);
    return association;
  }
}
```

**Note:** Adjust `VinRepositoryJPATest.deleteOrphanedVinsRemovesVinWhenNoUserLinksRemain` if H2 seed data makes the second scenario awkward — alternative is to insert a fresh `Vin` row with no `UserVin` links via `vinRepository.save`.

- [ ] **Step 3: Run tests**

Run: `cd Server && ./gradlew test --tests "com.capstone.data.UserVinRepositoryJPATest" --tests "com.capstone.data.VinRepositoryJPATest"`
Expected: PASS (fix seed/setup if needed before committing)

- [ ] **Step 4: Commit**

```bash
git add Server/src/test/java/com/capstone/data/UserVinRepositoryJPATest.java \
        Server/src/test/java/com/capstone/data/VinRepositoryJPATest.java
git commit -m "test: cover UserVin and orphan Vin repository deletes"
```

---

### Task 5: UserDeletionIntegrationTest

**Files:**
- Create: `Server/src/test/java/com/capstone/domain/UserDeletionIntegrationTest.java`

- [ ] **Step 1: Write integration test using Flyway seed user**

```java
package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.data.RefreshTokenRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.RefreshToken;
import com.capstone.models.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.capstone.support.IntegrationTestProperties;

@SpringBootTest(properties = IntegrationTestProperties.h2FlywaySeed("user-deletion"))
class UserDeletionIntegrationTest {

  @Autowired private UserDeletionService userDeletionService;
  @Autowired private UserRepositoryJPA userRepository;
  @Autowired private UserVinRepositoryJPA userVinRepository;
  @Autowired private RefreshTokenRepositoryJPA refreshTokenRepository;
  @Autowired private VinRepositoryJPA vinRepository;

  @Test
  @Transactional
  void deleteUserAndRelatedDataRemovesTokensVehiclesAndUserRow() {
    User user = userRepository.findById(1L).orElseThrow();
    String vin = userVinRepository.findVinNumbersForUser(1L).getFirst();
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);
    refreshToken.setToken("delete-flow-token");
    refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
    refreshTokenRepository.saveAndFlush(refreshToken);

    userDeletionService.deleteUserAndRelatedData(user);

    assertFalse(userRepository.findById(1L).isPresent());
    assertTrue(userVinRepository.findVinNumbersForUser(1L).isEmpty());
    assertFalse(refreshTokenRepository.findByToken("delete-flow-token").isPresent());
    // Camry VIN may remain if Civic still references same vehicle_type graph — assert user link gone:
    assertFalse(userVinRepository.findByUserUserIdAndVinVin(1L, vin).isPresent());
  }
}
```

- [ ] **Step 2: Run test**

Run: `cd Server && ./gradlew test --tests "com.capstone.domain.UserDeletionIntegrationTest"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add Server/src/test/java/com/capstone/domain/UserDeletionIntegrationTest.java
git commit -m "test: integration coverage for UserDeletionService cascade"
```

---

### Task 6: RefreshTokenIntegrationTest

**Files:**
- Create: `Server/src/test/java/com/capstone/authentication/RefreshTokenIntegrationTest.java`

- [ ] **Step 1: Write test for rotation consuming old token**

```java
package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.capstone.data.RefreshTokenRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.capstone.support.IntegrationTestProperties;
import com.capstone.support.MailjetTestSupport;
import com.mailjet.client.MailjetClient;

@SpringBootTest(properties = IntegrationTestProperties.h2FlywaySeed("refresh-token-integration"))
class RefreshTokenIntegrationTest {

  @MockitoBean private MailjetClient mailjetClient;

  @Autowired private AuthenticationService authenticationService;
  @Autowired private UserRepositoryJPA userRepository;
  @Autowired private RefreshTokenRepositoryJPA refreshTokenRepository;

  @BeforeEach
  void stubMailjet() throws Exception {
    MailjetTestSupport.stubSuccessfulSend(mailjetClient);
  }

  @Test
  void refreshTokenRotatesAndRejectsReplay() {
    User user = userRepository.findById(1L).orElseThrow();
    AuthenticationResponse session = authenticationService.createSession(user);
    String oldRefresh = session.getRefreshToken();

    AuthenticationResponse rotated = authenticationService.refreshToken(oldRefresh);

    assertNotEquals(oldRefresh, rotated.getRefreshToken());
    assertThrows(
        InvalidRefreshTokenException.class, () -> authenticationService.refreshToken(oldRefresh));
    assertEquals(0, refreshTokenRepository.deleteByIdReturning(-1L)); // sanity: repo method works
  }
}
```

Remove the sanity line `deleteByIdReturning(-1L)` before commit — it was only to illustrate repo access. Final test should end at `assertThrows`.

- [ ] **Step 2: Run test**

Run: `cd Server && ./gradlew test --tests "com.capstone.authentication.RefreshTokenIntegrationTest"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add Server/src/test/java/com/capstone/authentication/RefreshTokenIntegrationTest.java
git commit -m "test: cover refresh token rotation against real repository"
```

---

### Task 7: AuthenticationControllerIntegrationTest (MockMvc)

**Files:**
- Create: `Server/src/test/java/com/capstone/authentication/AuthenticationControllerIntegrationTest.java`

- [ ] **Step 1: Write MockMvc tests**

```java
package com.capstone.authentication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.support.IntegrationTestProperties;
import com.capstone.support.MailjetTestSupport;
import com.mailjet.client.MailjetClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = IntegrationTestProperties.h2CreateDrop("auth-controller-integration"))
@AutoConfigureMockMvc
class AuthenticationControllerIntegrationTest {

  @MockitoBean private MailjetClient mailjetClient;

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void stubMailjet() throws Exception {
    MailjetTestSupport.stubSuccessfulSend(mailjetClient);
  }

  @Test
  void registerReturns400ForInvalidPayload() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"firstname":"","lastname":"D","email":"not-an-email","password":"short"}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registerReturns409ForDuplicateEmail() throws Exception {
    String email = "dup-" + System.nanoTime() + "@example.com";
    String body =
        """
        {"firstname":"Pat","lastname":"Driver","email":"%s","password":"Password1!"}
        """
            .formatted(email);

    MockHttpServletResponse first = performRegister(body);
    String csrf = readCsrf(first);
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-XSRF-TOKEN", csrf)
                .cookie(first.getCookies())
                .content(body))
        .andExpect(status().isConflict())
        .andExpect(content().string("Unable to complete registration"));
  }

  private MockHttpServletResponse performRegister(String body) throws Exception {
    return mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();
  }

  private static String readCsrf(MockHttpServletResponse response) {
    for (var cookie : response.getCookies()) {
      if ("XSRF-TOKEN".equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    throw new IllegalStateException("Missing XSRF-TOKEN cookie");
  }
}
```

- [ ] **Step 2: Run test**

Run: `cd Server && ./gradlew test --tests "com.capstone.authentication.AuthenticationControllerIntegrationTest"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add Server/src/test/java/com/capstone/authentication/AuthenticationControllerIntegrationTest.java
git commit -m "test: MockMvc coverage for registration validation and duplicate email"
```

---

### Task 8: RepositoryQueryArchitectureTest (static guard)

**Files:**
- Create: `Server/src/test/java/com/capstone/data/RepositoryQueryArchitectureTest.java`

- [ ] **Step 1: Write architecture test**

```java
package com.capstone.data;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

class RepositoryQueryArchitectureTest {

  @Test
  void modifyingQueriesMustNotUseSelect() throws Exception {
  List<String> violations = new ArrayList<>();
    for (Class<?> repository : List.of(
        UserRepositoryJPA.class,
        EmailVerificationCodeRepositoryJPA.class,
        AccountChangeRequestRepositoryJPA.class,
        RefreshTokenRepositoryJPA.class,
        UserVinRepositoryJPA.class,
        VinRepositoryJPA.class,
        CompletedMaintenanceRepositoryJPA.class,
        CompletedRecallRepositoryJPA.class)) {
      for (Method method : repository.getDeclaredMethods()) {
        Query query = method.getAnnotation(Query.class);
        if (query == null) {
          continue;
        }
        String jpql = query.value().strip().toLowerCase();
        if (method.isAnnotationPresent(Modifying.class) && jpql.startsWith("select")) {
          violations.add(repository.getSimpleName() + "#" + method.getName() + " uses @Modifying with SELECT");
        }
        if (method.getReturnType() == boolean.class
            && jpql.startsWith("select")
            && !jpql.contains("case when")
            && !jpql.contains("count(")) {
          violations.add(
              repository.getSimpleName()
                  + "#"
                  + method.getName()
                  + " returns boolean but SELECT may yield null");
        }
      }
    }
    if (!violations.isEmpty()) {
      fail(String.join("\n", violations));
    }
  }
}
```

- [ ] **Step 2: Run test**

Run: `cd Server && ./gradlew test --tests "com.capstone.data.RepositoryQueryArchitectureTest"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add Server/src/test/java/com/capstone/data/RepositoryQueryArchitectureTest.java
git commit -m "test: guard against broken @Modifying SELECT repository queries"
```

---

### Task 9: Angular interceptor and auth error tests

**Files:**
- Create: `Client/src/app/core/auth/unauthorized.interceptor.spec.ts`
- Create: `Client/src/app/core/http/credentials.interceptor.spec.ts`
- Modify: `Client/src/app/core/auth/auth.service.spec.ts`

- [ ] **Step 1: Create unauthorized interceptor spec**

```typescript
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { apiConfig } from '../api/api.config';
import { credentialsInterceptor } from '../http/credentials.interceptor';
import { unauthorizedInterceptor } from './unauthorized.interceptor';
import { AuthService } from './auth.service';

describe('unauthorizedInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([credentialsInterceptor, unauthorizedInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('clears the session on 401 from protected APIs when signed in', () => {
    authService.login({ email: 'pat@example.com', password: 'password' }).subscribe();
    httpTesting.expectOne(`${apiConfig.authUrl}/authenticate`).flush({ emailVerified: true });
    expect(authService.isSignedIn()).toBe(true);

    http.get(`${apiConfig.accountUrl}/me`).subscribe({ error: () => undefined });
    httpTesting
      .expectOne(`${apiConfig.accountUrl}/me`)
      .flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(authService.isSignedIn()).toBe(false);
  });

  it('does not clear the session for 401 on login', () => {
    authService.login({ email: 'pat@example.com', password: 'wrong' }).subscribe({
      error: () => undefined,
    });

    httpTesting
      .expectOne(`${apiConfig.authUrl}/authenticate`)
      .flush('Invalid account credentials', { status: 401, statusText: 'Unauthorized' });

    expect(authService.isSignedIn()).toBe(false);
  });
});
```

- [ ] **Step 2: Create credentials interceptor spec**

```typescript
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { apiConfig } from '../api/api.config';
import { credentialsInterceptor } from './credentials.interceptor';

describe('credentialsInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([credentialsInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('sets withCredentials for same-origin API requests', () => {
    http.get(`${apiConfig.accountUrl}/me`).subscribe();

    const request = httpTesting.expectOne(`${apiConfig.accountUrl}/me`);
    expect(request.request.withCredentials).toBe(true);
    request.flush({});
  });
});
```

- [ ] **Step 3: Add registration error test to auth.service.spec.ts**

Append inside the `describe('AuthService', ...)` block:

```typescript
  it('surfaces server error text when registration fails with 500', () => {
    service
      .register({
        firstname: 'Pat',
        lastname: 'Driver',
        email: 'pat@example.com',
        password: 'Password1!',
      })
      .subscribe({
        error: (error) => {
          expect(error.status).toBe(500);
          expect(error.error).toBe("Sometimes things just don't go as planned.");
        },
      });

    const request = httpTesting.expectOne(`${apiConfig.authUrl}/register`);
    request.flush("Sometimes things just don't go as planned.", {
      status: 500,
      statusText: 'Internal Server Error',
    });

    expect(service.isSignedIn()).toBe(false);
  });
```

- [ ] **Step 4: Run Client tests**

Run: `cd Client && pnpm test -- auth.service.spec.ts unauthorized.interceptor.spec.ts credentials.interceptor.spec.ts`
Expected: PASS

- [ ] **Step 5: Format and commit**

```bash
cd Client && pnpm exec prettier --write \
  src/app/core/auth/unauthorized.interceptor.spec.ts \
  src/app/core/http/credentials.interceptor.spec.ts \
  src/app/core/auth/auth.service.spec.ts
cd ..
git add Client/src/app/core/auth/unauthorized.interceptor.spec.ts \
        Client/src/app/core/http/credentials.interceptor.spec.ts \
        Client/src/app/core/auth/auth.service.spec.ts
git commit -m "test: cover auth interceptors and registration error responses"
```

---

### Task 10: Final verification

- [ ] **Step 1: Run targeted Server suite**

Run: `cd Server && ./gradlew test --tests "com.capstone.data.*" --tests "com.capstone.authentication.*" --tests "com.capstone.domain.UserDeletionIntegrationTest"`
Expected: PASS

- [ ] **Step 2: Run full Client suite**

Run: `cd Client && pnpm test`
Expected: PASS (note: `local-date.spec.ts` may fail on UTC VMs — pre-existing)

- [ ] **Step 3: Run Spotless**

Run: `cd Server && ./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Final commit if formatting changed**

```bash
git add -A && git status
# commit only if spotless/prettier produced changes
```

---

## Self-review

| Spec requirement | Task |
|------------------|------|
| Repository `@Query` regression (registration bug class) | Tasks 2–4, 8 |
| Service-level multi-repo flows | Tasks 5–6 |
| HTTP status edge cases (400/409/500) | Tasks 7, 9 |
| Client session/error handling | Task 9 |
| Fix invalid test JWT secret | Task 1 |
| Static guard against reintroducing bug | Task 8 |

**Placeholder scan:** No TBD steps. All code blocks are complete.

**Type/name consistency:** Repository method names match `Server/src/main/java/com/capstone/data/*.java`. `IntegrationTestProperties` used consistently across integration tests.

**Out of scope (YAGNI for this plan):** Vehicle provider live smoke tests, `CompletedMaintenanceRepositoryJPATest` / `CompletedRecallRepositoryJPATest` as separate tasks (can follow same Flyway seed pattern as Task 4 if deletion bugs are a concern), `EmailVerifiedFilter` allowlist tests for `/api/account/delete` (low risk — add if touching filter).

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-14-missing-edge-case-tests.md`. Two execution options:

**1. Subagent-Driven (recommended)** — Dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
