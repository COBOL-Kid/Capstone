package com.capstone.data;

import com.capstone.models.EmailVerificationCode;
import com.capstone.models.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface EmailVerificationCodeRepositoryJPA
    extends JpaRepository<EmailVerificationCode, Long> {

  Optional<EmailVerificationCode> findTopByUserOrderByCreatedAtDesc(User user);

  Optional<EmailVerificationCode> findBySignInChallengeHash(String signInChallengeHash);

  @Modifying
  void deleteByUser(User user);

  @Modifying
  void deleteByExpiresAtBefore(Instant cutoff);
}
