package com.capstone.authentication;

import static org.mockito.Mockito.*;

import com.capstone.configuration.EmailVerificationProperties;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.UserDeletionService;
import com.capstone.email.EmailVerificationService;
import com.capstone.models.User;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class UnverifiedAccountCleanupJobTest {

  @Test
  void shouldDeleteOnlyStaleUnverifiedAccountsAndExpiredCodes() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    UserDeletionService userDeletionService = mock(UserDeletionService.class);
    EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    EmailVerificationProperties properties = new EmailVerificationProperties();
    properties.setUnverifiedAccountRetentionHours(24);
    UnverifiedAccountCleanupJob job =
        new UnverifiedAccountCleanupJob(
            userRepository, userDeletionService, emailVerificationService, properties);

    User staleUser = new User();
    staleUser.setUserId(9L);
    when(userRepository.findByEmailVerifiedFalseAndCreatedAtBefore(any(Instant.class)))
        .thenReturn(List.of(staleUser));

    job.cleanupUnverifiedAccounts();

    verify(userDeletionService).deleteUserAndRelatedData(staleUser);
    verify(emailVerificationService).deleteExpiredCodes();
    verify(userDeletionService, never())
        .deleteUserAndRelatedData(argThat(user -> user.getUserId() == null));
  }
}
