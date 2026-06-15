package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;
import com.capstone.support.IntegrationTestProperties;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class VinRepositoryJPATest {

  private static final String SHARED_VIN = "4T1C11AK5LU123456";
  private static final String ORPHAN_VIN = "1HGBH41JXMN109186";

  @DynamicPropertySource
  static void h2FlywaySeedProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2FlywaySeed("vin-orphan-repo")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @Autowired private VinRepositoryJPA vinRepository;
  @Autowired private UserVinRepositoryJPA userVinRepository;
  @Autowired private UserRepositoryJPA userRepository;
  @Autowired private VehicleTypeRepositoryJPA vehicleTypeRepository;
  @Autowired private EntityManager entityManager;

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
    VehicleType camryType = vehicleTypeRepository.findById(1L).orElseThrow();
    vinRepository.saveAndFlush(new Vin(ORPHAN_VIN, camryType));

    vinRepository.deleteOrphanedVins(List.of(ORPHAN_VIN));
    vinRepository.flush();
    entityManager.clear();

    assertFalse(vinRepository.findById(ORPHAN_VIN).isPresent());
  }

  private User saveUser(String email) {
    User user = new User();
    user.setUserEmail(email);
    user.setUserPw("encoded-secret");
    user.setRole(Role.USER);
    return userRepository.saveAndFlush(user);
  }

  private UserVin link(User user, Vin vin) {
    UserVin association = new UserVin(user, vin, 10_000);
    association.setAvailableImageUrls(List.of());
    return association;
  }
}
