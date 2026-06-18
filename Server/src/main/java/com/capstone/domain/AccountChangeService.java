package com.capstone.domain;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.authentication.AuthenticationResponse;
import com.capstone.authentication.AuthenticationService;
import com.capstone.authentication.PasswordPolicy;
import com.capstone.configuration.EmailVerificationProperties;
import com.capstone.data.AccountChangeRequestRepositoryJPA;
import com.capstone.data.RefreshTokenRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.email.EmailContent;
import com.capstone.email.EmailDeliveryException;
import com.capstone.email.EmailNormalizer;
import com.capstone.email.ExpiredEmailVerificationCodeException;
import com.capstone.email.InvalidEmailVerificationCodeException;
import com.capstone.email.MailjetEmailClient;
import com.capstone.email.VerificationEmailComposer;
import com.capstone.logging.AuditLog;
import com.capstone.models.AccountChangeRequest;
import com.capstone.models.AccountChangeType;
import com.capstone.models.User;
import com.capstone.models.dto.AccountChangeInitiatedResponse;
import com.capstone.models.dto.AccountResponse;
import com.capstone.models.dto.InitiateAccountChangeRequest;
import com.capstone.models.dto.PendingAccountChangeResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountChangeService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Pattern SMS_PATTERN = Pattern.compile("^$|^(?=.*\\d)[+0-9() .-]+$");

  private final UserRepositoryJPA userRepository;
  private final AccountChangeRequestRepositoryJPA changeRequestRepository;
  private final RefreshTokenRepositoryJPA refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationProperties properties;
  private final Optional<MailjetEmailClient> mailjetEmailClient;
  private final VerificationEmailComposer verificationEmailComposer;
  private final AuthenticationService authenticationService;

  public AccountChangeService(
      UserRepositoryJPA userRepository,
      AccountChangeRequestRepositoryJPA changeRequestRepository,
      RefreshTokenRepositoryJPA refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      EmailVerificationProperties properties,
      Optional<MailjetEmailClient> mailjetEmailClient,
      VerificationEmailComposer verificationEmailComposer,
      AuthenticationService authenticationService) {
    this.userRepository = userRepository;
    this.changeRequestRepository = changeRequestRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.mailjetEmailClient = mailjetEmailClient;
    this.verificationEmailComposer = verificationEmailComposer;
    this.authenticationService = authenticationService;
  }

  @Transactional
  public AccountChangeInitiatedResponse initiateChange(
      AuthenticatedUser principal, InitiateAccountChangeRequest request) {
    User user = loadCurrentUser(principal);
    changeRequestRepository.deleteByUser(user);

    AccountChangeRequest pending = new AccountChangeRequest();
    pending.setUser(user);
    pending.setChangeType(request.changeType());
    pending.setCreatedAt(Instant.now());

    switch (request.changeType()) {
      case EMAIL -> pending.setNewEmail(validateNewEmail(user, request.newEmail()));
      case PASSWORD -> {
        validateCurrentPassword(user, request.currentPassword());
        validateNewPassword(request.newPassword());
        pending.setNewPasswordHash(passwordEncoder.encode(request.newPassword()));
      }
      case SMS -> pending.setNewUserSms(validateNewSms(user, request.userSms()));
    }

    String plainCode = issueCode(pending);
    changeRequestRepository.save(pending);
    sendVerificationEmail(user, plainCode, request.changeType());

    AuditLog.info(
        "account_change_initiated",
        "userId",
        user.getUserId(),
        "changeType",
        request.changeType().name());

    return new AccountChangeInitiatedResponse(
        request.changeType(), properties.getCodeExpirationMinutes());
  }

  @Transactional
  public AccountChangeVerificationResult verifyChange(AuthenticatedUser principal, String code) {
    User user = loadCurrentUser(principal);
    AccountChangeRequest pending =
        changeRequestRepository
            .findByUser(user)
            .orElseThrow(PendingAccountChangeNotFoundException::new);

    verifyCode(pending, code);

    AuthenticationResponse session = null;
    switch (pending.getChangeType()) {
      case EMAIL -> {
        applyEmailChange(user, pending.getNewEmail());
        session = authenticationService.createSession(user);
      }
      case PASSWORD -> {
        user.setUserPw(pending.getNewPasswordHash());
        refreshTokenRepository.deleteByUser(user);
      }
      case SMS -> user.setUserSms(pending.getNewUserSms());
    }

    try {
      userRepository.save(user);
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateEmailException();
    }
    changeRequestRepository.delete(pending);

    AuditLog.info(
        "account_change_verified",
        "userId",
        user.getUserId(),
        "changeType",
        pending.getChangeType().name());

    return new AccountChangeVerificationResult(toResponse(user), session, pending.getChangeType());
  }

  @Transactional
  public AccountChangeInitiatedResponse resendCode(AuthenticatedUser principal) {
    User user = loadCurrentUser(principal);
    AccountChangeRequest pending =
        changeRequestRepository
            .findByUser(user)
            .orElseThrow(PendingAccountChangeNotFoundException::new);

    enforceResendCooldown(pending);
    String plainCode = issueCode(pending);
    changeRequestRepository.save(pending);
    sendVerificationEmail(user, plainCode, pending.getChangeType());

    return new AccountChangeInitiatedResponse(
        pending.getChangeType(), properties.getCodeExpirationMinutes());
  }

  @Transactional(readOnly = true)
  public Optional<PendingAccountChangeResponse> getPendingChange(AuthenticatedUser principal) {
    User user = loadCurrentUser(principal);
    return changeRequestRepository
        .findByUser(user)
        .map(
            pending ->
                new PendingAccountChangeResponse(
                    pending.getChangeType(), properties.getCodeExpirationMinutes()));
  }

  private String validateNewEmail(User user, String newEmail) {
    if (newEmail == null || newEmail.isBlank()) {
      throw new IllegalArgumentException("New email is required");
    }
    String normalized = EmailNormalizer.normalize(newEmail);
    if (normalized.equals(user.getUserEmail())) {
      throw new IllegalArgumentException("New email must be different from your current email");
    }
    userRepository
        .findByUserEmail(normalized)
        .filter(existing -> !existing.getUserId().equals(user.getUserId()))
        .ifPresent(
            existing -> {
              throw new DuplicateEmailException();
            });
    return normalized;
  }

  private void validateCurrentPassword(User user, String currentPassword) {
    if (currentPassword == null || currentPassword.isBlank()) {
      throw new IllegalArgumentException("Current password is required");
    }
    if (!passwordEncoder.matches(currentPassword, user.getUserPw())) {
      throw new InvalidAccountCredentialsException();
    }
  }

  private void validateNewPassword(String newPassword) {
    PasswordPolicy.validate(newPassword);
  }

  private String validateNewSms(User user, String userSms) {
    String normalized = cleanOptionalSms(userSms);
    if (!SMS_PATTERN.matcher(userSms == null ? "" : userSms).matches()) {
      throw new IllegalArgumentException("SMS number contains invalid characters");
    }
    if (Objects.equals(normalized, user.getUserSms())) {
      throw new IllegalArgumentException(
          "New SMS number must be different from your current number");
    }
    return normalized;
  }

  private void applyEmailChange(User user, String newEmail) {
    userRepository
        .findByUserEmail(newEmail)
        .filter(existing -> !existing.getUserId().equals(user.getUserId()))
        .ifPresent(
            existing -> {
              throw new DuplicateEmailException();
            });
    user.setUserEmail(newEmail);
    user.setEmailVerified(true);
    user.setEmailVerifiedAt(Instant.now());
  }

  private String issueCode(AccountChangeRequest pending) {
    String plainCode = generateCode();
    pending.setCodeHash(passwordEncoder.encode(plainCode));
    pending.setExpiresAt(
        Instant.now().plus(Duration.ofMinutes(properties.getCodeExpirationMinutes())));
    pending.setCreatedAt(Instant.now());
    pending.setFailedAttempts(0);
    return plainCode;
  }

  private void verifyCode(AccountChangeRequest pending, String code) {
    if (pending.getExpiresAt().isBefore(Instant.now())) {
      changeRequestRepository.delete(pending);
      throw new ExpiredEmailVerificationCodeException();
    }
    if (!passwordEncoder.matches(code, pending.getCodeHash())) {
      handleFailedVerificationAttempt(pending);
    }
  }

  private void handleFailedVerificationAttempt(AccountChangeRequest pending) {
    int attempts = pending.getFailedAttempts() + 1;
    pending.setFailedAttempts(attempts);
    if (attempts >= properties.getMaxVerificationAttempts()) {
      changeRequestRepository.delete(pending);
      throw new TooManyRequestsException(
          "Too many invalid verification attempts. Request a new code and try again.");
    }
    changeRequestRepository.save(pending);
    throw new InvalidEmailVerificationCodeException();
  }

  private void enforceResendCooldown(AccountChangeRequest pending) {
    Instant earliestResend =
        pending.getCreatedAt().plusSeconds(properties.getResendCooldownSeconds());
    if (Instant.now().isBefore(earliestResend)) {
      throw new TooManyRequestsException(
          "Please wait before requesting another verification code.");
    }
  }

  private void sendVerificationEmail(User user, String plainCode, AccountChangeType changeType) {
    MailjetEmailClient client =
        mailjetEmailClient.orElseThrow(
            () -> new EmailDeliveryException("Email delivery is not configured"));
    String name = user.getFirstName() != null ? user.getFirstName() : user.getUserEmail();
    EmailContent content =
        verificationEmailComposer.composeAccountChange(
            name, plainCode, properties.getCodeExpirationMinutes(), changeType);
    client.sendEmail(
        user.getUserEmail(), name, content.subject(), content.textPart(), content.htmlPart());
  }

  private User loadCurrentUser(AuthenticatedUser principal) {
    if (principal == null || principal.userId() == null) {
      throw new InvalidAccountCredentialsException();
    }
    return userRepository
        .findById(principal.userId())
        .orElseThrow(InvalidAccountCredentialsException::new);
  }

  private AccountResponse toResponse(User user) {
    return new AccountResponse(
        user.getUserId(),
        user.getUserEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getUserSms(),
        user.isEmailVerified(),
        user.getEmailVerifiedAt(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  private static String cleanOptionalSms(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static String generateCode() {
    int value = RANDOM.nextInt(900_000) + 100_000;
    return Integer.toString(value);
  }
}
