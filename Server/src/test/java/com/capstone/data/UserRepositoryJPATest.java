package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.Role;
import com.capstone.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserRepositoryJPATest {

  @Autowired private UserRepositoryJPA userRepository;

  @Test
  void existsByUserEmailReturnsFalseWhenNoUserMatches() {
    assertFalse(userRepository.existsByUserEmail("missing-" + System.nanoTime() + "@example.com"));
  }

  @Test
  void existsByUserEmailReturnsTrueWhenUserMatches() {
    String email = "exists-test-" + System.nanoTime() + "@example.com";
    User user = new User();
    user.setUserEmail(email);
    user.setUserPw("encoded-secret");
    user.setRole(Role.USER);
    userRepository.saveAndFlush(user);

    assertTrue(userRepository.existsByUserEmail(email));
  }
}
