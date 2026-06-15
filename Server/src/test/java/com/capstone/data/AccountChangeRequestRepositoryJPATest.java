package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.AccountChangeRequest;
import com.capstone.models.AccountChangeType;
import com.capstone.models.Role;
import com.capstone.models.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class AccountChangeRequestRepositoryJPATest {

  @Autowired private AccountChangeRequestRepositoryJPA changeRequestRepository;
  @Autowired private UserRepositoryJPA userRepository;

  @Test
  @Transactional
  void deleteByUserRemovesPendingChangeRequest() {
    User user = saveUser("change-" + System.nanoTime() + "@example.com");
    saveChangeRequest(user);

    changeRequestRepository.deleteByUser(user);
    changeRequestRepository.flush();

    assertFalse(changeRequestRepository.findByUser(user).isPresent());
  }

  @Test
  @Transactional
  void findByUserReturnsPendingChangeRequest() {
    User user = saveUser("find-change-" + System.nanoTime() + "@example.com");
    saveChangeRequest(user);

    assertTrue(changeRequestRepository.findByUser(user).isPresent());
  }

  private User saveUser(String email) {
    User user = new User();
    user.setUserEmail(email);
    user.setUserPw("encoded-secret");
    user.setRole(Role.USER);
    return userRepository.saveAndFlush(user);
  }

  private void saveChangeRequest(User user) {
    AccountChangeRequest request = new AccountChangeRequest();
    request.setUser(user);
    request.setChangeType(AccountChangeType.EMAIL);
    request.setNewEmail("new-" + user.getUserEmail());
    request.setCodeHash("hash");
    request.setExpiresAt(Instant.now().plusSeconds(300));
    request.setCreatedAt(Instant.now());
    changeRequestRepository.saveAndFlush(request);
  }
}
