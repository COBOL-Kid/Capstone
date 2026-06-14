package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.data.RefreshTokenRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.models.RefreshToken;
import com.capstone.models.User;
import com.capstone.support.IntegrationTestProperties;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class UserDeletionIntegrationTest {

  private static final long SEED_USER_ID = 1L;

  @DynamicPropertySource
  static void h2FlywaySeedProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2FlywaySeed("user-deletion")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @Autowired private UserDeletionService userDeletionService;
  @Autowired private UserRepositoryJPA userRepository;
  @Autowired private UserVinRepositoryJPA userVinRepository;
  @Autowired private RefreshTokenRepositoryJPA refreshTokenRepository;
  @Autowired private EntityManager entityManager;

  @Test
  @Transactional
  void deleteUserAndRelatedDataRemovesTokensVehiclesAndUserRow() {
    User user = userRepository.findById(SEED_USER_ID).orElseThrow();
    String vin = userVinRepository.findVinNumbersForUser(SEED_USER_ID).getFirst();

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);
    refreshToken.setToken("delete-flow-token");
    refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
    refreshTokenRepository.saveAndFlush(refreshToken);
    entityManager.detach(refreshToken);

    userDeletionService.deleteUserAndRelatedData(user);

    assertFalse(userRepository.findById(SEED_USER_ID).isPresent());
    assertTrue(userVinRepository.findVinNumbersForUser(SEED_USER_ID).isEmpty());
    assertFalse(refreshTokenRepository.findByToken("delete-flow-token").isPresent());
    assertFalse(userVinRepository.findByUserUserIdAndVinVin(SEED_USER_ID, vin).isPresent());
  }
}
