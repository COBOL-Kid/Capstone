package com.capstone.data;

import com.capstone.models.EmailVerificationCode;
import com.capstone.models.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerificationCodeRepositoryJPA
    extends JpaRepository<EmailVerificationCode, Long> {

  @Query("SELECT e FROM EmailVerificationCode e WHERE e.user = :user ORDER BY e.createdAt desc")
  Optional<EmailVerificationCode> findTopByUserOrderByCreatedAtDesc(User user);

  @Query("SELECT e FROM EmailVerificationCode e WHERE e.signInChallengeHash = :signInChallengeHash")
  Optional<EmailVerificationCode> findBySignInChallengeHash(String signInChallengeHash);

  @Query("SELECT e FROM EmailVerificationCode e WHERE e.user = :user")
  @Modifying
  void deleteByUser(User user);

  @Query("SELECT e FROM EmailVerificationCode e WHERE e.expiresAt < :cutoff")
  @Modifying
  void deleteByExpiresAtBefore(Instant cutoff);
}
