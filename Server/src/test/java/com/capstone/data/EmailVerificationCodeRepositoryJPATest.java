package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.EmailVerificationCode;
import com.capstone.models.Role;
import com.capstone.models.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class EmailVerificationCodeRepositoryJPATest {

  @Autowired private EmailVerificationCodeRepositoryJPA verificationCodeRepository;
  @Autowired private UserRepositoryJPA userRepository;

  @Test
  @Transactional
  void deleteByUserRemovesCodesForThatUserOnly() {
    User owner = saveUser("owner-" + System.nanoTime() + "@example.com");
    User other = saveUser("other-" + System.nanoTime() + "@example.com");
    saveCode(owner, "111111");
    saveCode(other, "222222");

    verificationCodeRepository.deleteByUser(owner);

    assertFalse(verificationCodeRepository.findTopByUserOrderByCreatedAtDesc(owner).isPresent());
    assertTrue(verificationCodeRepository.findTopByUserOrderByCreatedAtDesc(other).isPresent());
  }

  @Test
  @Transactional
  void deleteByExpiresAtBeforeRemovesOnlyExpiredCodes() {
    User user = saveUser("expiry-" + System.nanoTime() + "@example.com");
    EmailVerificationCode expired = saveCode(user, "expired-hash");
    expired.setExpiresAt(Instant.now().minusSeconds(60));
    verificationCodeRepository.saveAndFlush(expired);
    saveCode(user, "active-hash");

    verificationCodeRepository.deleteByExpiresAtBefore(Instant.now());
    verificationCodeRepository.flush();

    assertTrue(
        verificationCodeRepository
            .findTopByUserOrderByCreatedAtDesc(user)
            .map(EmailVerificationCode::getCodeHash)
            .filter("active-hash"::equals)
            .isPresent());
  }

  private User saveUser(String email) {
    User user = new User();
    user.setUserEmail(email);
    user.setUserPw("encoded-secret");
    user.setRole(Role.USER);
    return userRepository.saveAndFlush(user);
  }

  private EmailVerificationCode saveCode(User user, String codeHash) {
    EmailVerificationCode code = new EmailVerificationCode();
    code.setUser(user);
    code.setCodeHash(codeHash);
    code.setExpiresAt(Instant.now().plusSeconds(300));
    code.setCreatedAt(Instant.now());
    return verificationCodeRepository.saveAndFlush(code);
  }
}
