package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.support.IntegrationTestProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class UserVinRepositoryJPATest {

  private static final long SEED_USER_ID = 1L;
  private static final String CAMRY_VIN = "4T1C11AK5LU123456";
  private static final String CIVIC_VIN = "2HGFC2F59JH543210";

  @DynamicPropertySource
  static void h2FlywaySeedProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2FlywaySeed("user-vin-repo")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @Autowired private UserVinRepositoryJPA userVinRepository;
  @Autowired private CompletedMaintenanceRepositoryJPA completedMaintenanceRepository;

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
    completedMaintenanceRepository.deleteAllForUserVin(SEED_USER_ID, CIVIC_VIN);
    completedMaintenanceRepository.flush();

    userVinRepository.deleteForUserVin(SEED_USER_ID, CIVIC_VIN);
    userVinRepository.flush();

    assertFalse(userVinRepository.findByUserUserIdAndVinVin(SEED_USER_ID, CIVIC_VIN).isPresent());
    assertTrue(userVinRepository.findByUserUserIdAndVinVin(SEED_USER_ID, CAMRY_VIN).isPresent());
  }
}
