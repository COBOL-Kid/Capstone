package com.capstone.authentication;

import com.capstone.configuration.EmailVerificationProperties;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.UserDeletionService;
import com.capstone.email.EmailVerificationService;
import com.capstone.models.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UnverifiedAccountCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(UnverifiedAccountCleanupJob.class);

  private final UserRepositoryJPA userRepository;
  private final UserDeletionService userDeletionService;
  private final EmailVerificationService emailVerificationService;
  private final EmailVerificationProperties properties;

  public UnverifiedAccountCleanupJob(
      UserRepositoryJPA userRepository,
      UserDeletionService userDeletionService,
      EmailVerificationService emailVerificationService,
      EmailVerificationProperties properties) {
    this.userRepository = userRepository;
    this.userDeletionService = userDeletionService;
    this.emailVerificationService = emailVerificationService;
    this.properties = properties;
  }

  @Scheduled(cron = "0 */15 * * * *")
  public void cleanupUnverifiedAccounts() {
    long startNanos = System.nanoTime();
    Instant cutoff =
        Instant.now().minus(Duration.ofHours(properties.getUnverifiedAccountRetentionHours()));
    List<User> staleUsers = userRepository.findByEmailVerifiedFalseAndCreatedAtBefore(cutoff);
    for (User user : staleUsers) {
      log.debug("Deleting unverified account userId={}", user.getUserId());
      userDeletionService.deleteUserAndRelatedData(user);
    }
    emailVerificationService.deleteExpiredCodes();
    long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
    log.info(
        "cleanup_unverified_accounts deletedCount={} durationMs={}", staleUsers.size(), durationMs);
  }
}
