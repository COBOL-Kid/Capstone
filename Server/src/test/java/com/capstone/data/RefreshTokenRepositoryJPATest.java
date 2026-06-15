package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.RefreshToken;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.support.IntegrationTestProperties;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class RefreshTokenRepositoryJPATest {

  @DynamicPropertySource
  static void h2CreateDropProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2CreateDrop("refresh-token-repo")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @Autowired private RefreshTokenRepositoryJPA refreshTokenRepository;
  @Autowired private UserRepositoryJPA userRepository;

  @Test
  void findByTokenReturnsMatchingRefreshToken() {
    User user = saveUser("refresh-find-" + System.nanoTime() + "@example.com");
    saveToken(user, "token-find-me");

    assertTrue(refreshTokenRepository.findByToken("token-find-me").isPresent());
    assertEquals(
        user.getUserId(),
        refreshTokenRepository.findByToken("token-find-me").orElseThrow().getUser().getUserId());
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

    assertEquals(1, refreshTokenRepository.deleteByIdReturning(token.getId()));
    assertEquals(0, refreshTokenRepository.deleteByIdReturning(token.getId()));
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
