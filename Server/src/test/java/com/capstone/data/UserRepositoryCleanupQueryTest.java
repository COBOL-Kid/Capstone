package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.support.IntegrationTestProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class UserRepositoryCleanupQueryTest {

  @DynamicPropertySource
  static void h2CreateDropProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2CreateDrop("user-cleanup-query")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @Autowired private UserRepositoryJPA userRepository;

  @Test
  void findByEmailVerifiedFalseAndCreatedAtBeforeReturnsOnlyStaleUnverifiedUsers() {
    Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);

    User stale =
        saveUser(
            "stale-" + System.nanoTime() + "@example.com",
            false,
            cutoff.minus(1, ChronoUnit.HOURS));
    User fresh = saveUser("fresh-" + System.nanoTime() + "@example.com", false, Instant.now());
    User verified =
        saveUser(
            "verified-" + System.nanoTime() + "@example.com",
            true,
            cutoff.minus(2, ChronoUnit.HOURS));

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
