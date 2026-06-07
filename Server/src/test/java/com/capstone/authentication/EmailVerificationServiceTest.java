package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.capstone.configuration.EmailVerificationProperties;
import com.capstone.data.EmailVerificationCodeRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.email.VerificationEmailComposer;
import com.capstone.integration.MailjetEmailClient;
import com.capstone.models.EmailVerificationCode;
import com.capstone.models.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class EmailVerificationServiceTest {

  private final VerificationEmailComposer verificationEmailComposer =
      new VerificationEmailComposer();

  @Test
  void shouldVerifyAuthenticatedUserWithValidCode() {
    EmailVerificationCodeRepositoryJPA codeRepository =
        mock(EmailVerificationCodeRepositoryJPA.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    EmailVerificationProperties properties = properties();
    MailjetEmailClient mailjetEmailClient = mock(MailjetEmailClient.class);
    EmailVerificationService service =
        new EmailVerificationService(
            codeRepository,
            userRepository,
            passwordEncoder,
            properties,
            Optional.of(mailjetEmailClient),
            verificationEmailComposer);

    User user = user();
    EmailVerificationCode stored = storedCode(user, "hash", Instant.now().plusSeconds(300), null);
    when(codeRepository.findTopByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.of(stored));
    when(passwordEncoder.matches("123456", "hash")).thenReturn(true);

    service.verifyAuthenticatedUser(user, "123456");

    assertTrue(user.isEmailVerified());
    assertNotNull(user.getEmailVerifiedAt());
    verify(codeRepository).deleteByUser(user);
    verify(userRepository).save(user);
  }

  @Test
  void shouldRejectExpiredCode() {
    EmailVerificationCodeRepositoryJPA codeRepository =
        mock(EmailVerificationCodeRepositoryJPA.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    EmailVerificationService service =
        new EmailVerificationService(
            codeRepository,
            userRepository,
            passwordEncoder,
            properties(),
            Optional.of(mock(MailjetEmailClient.class)),
            verificationEmailComposer);

    User user = user();
    EmailVerificationCode stored = storedCode(user, "hash", Instant.now().minusSeconds(60), null);
    when(codeRepository.findTopByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.of(stored));

    assertThrows(
        ExpiredEmailVerificationCodeException.class,
        () -> service.verifyAuthenticatedUser(user, "123456"));
    verify(codeRepository).delete(stored);
  }

  @Test
  void shouldRotateCodesWhenSendingRegistrationCode() {
    EmailVerificationCodeRepositoryJPA codeRepository =
        mock(EmailVerificationCodeRepositoryJPA.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    MailjetEmailClient mailjetEmailClient = mock(MailjetEmailClient.class);
    EmailVerificationService service =
        new EmailVerificationService(
            codeRepository,
            userRepository,
            passwordEncoder,
            properties(),
            Optional.of(mailjetEmailClient),
            verificationEmailComposer);

    User user = user();
    when(passwordEncoder.encode(anyString())).thenReturn("encoded-code");

    service.sendRegistrationCode(user);

    verify(codeRepository).deleteByUser(user);
    verify(codeRepository).save(any(EmailVerificationCode.class));
    verify(mailjetEmailClient)
        .sendEmail(
            eq(user.getUserEmail()),
            anyString(),
            anyString(),
            contains("expires in 5 minutes"),
            anyString());
  }

  private static EmailVerificationProperties properties() {
    EmailVerificationProperties properties = new EmailVerificationProperties();
    properties.setCodeExpirationMinutes(5);
    properties.setUnverifiedAccountRetentionHours(24);
    return properties;
  }

  private static User user() {
    User user = new User();
    user.setUserId(1L);
    user.setUserEmail("driver@example.com");
    user.setFirstName("Pat");
    return user;
  }

  private static EmailVerificationCode storedCode(
      User user, String hash, Instant expiresAt, String challengeHash) {
    EmailVerificationCode code = new EmailVerificationCode();
    code.setUser(user);
    code.setCodeHash(hash);
    code.setExpiresAt(expiresAt);
    code.setSignInChallengeHash(challengeHash);
    code.setCreatedAt(Instant.now());
    return code;
  }
}
