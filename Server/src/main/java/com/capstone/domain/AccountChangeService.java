package com.capstone.domain;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.authentication.AuthenticationResponse;
import com.capstone.authentication.AuthenticationService;
import com.capstone.configuration.EmailVerificationProperties;
import com.capstone.data.AccountChangeRequestRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.email.EmailDeliveryException;
import com.capstone.email.EmailNormalizer;
import com.capstone.email.ExpiredEmailVerificationCodeException;
import com.capstone.email.InvalidEmailVerificationCodeException;
import com.capstone.email.MailjetEmailClient;
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
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationProperties properties;
  private final Optional<MailjetEmailClient> mailjetEmailClient;
  private final AuthenticationService authenticationService;

  public AccountChangeService(
      UserRepositoryJPA userRepository,
      AccountChangeRequestRepositoryJPA changeRequestRepository,
      PasswordEncoder passwordEncoder,
      EmailVerificationProperties properties,
      Optional<MailjetEmailClient> mailjetEmailClient,
      AuthenticationService authenticationService) {
    this.userRepository = userRepository;
    this.changeRequestRepository = changeRequestRepository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.mailjetEmailClient = mailjetEmailClient;
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
      case PASSWORD -> user.setUserPw(pending.getNewPasswordHash());
      case SMS -> user.setUserSms(pending.getNewUserSms());
    }

    try {
      userRepository.save(user);
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateEmailException();
    }
    changeRequestRepository.delete(pending);

    return new AccountChangeVerificationResult(toResponse(user), session);
  }

  @Transactional
  public AccountChangeInitiatedResponse resendCode(AuthenticatedUser principal) {
    User user = loadCurrentUser(principal);
    AccountChangeRequest pending =
        changeRequestRepository
            .findByUser(user)
            .orElseThrow(PendingAccountChangeNotFoundException::new);

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
    if (newPassword == null || newPassword.isBlank()) {
      throw new IllegalArgumentException("New password is required");
    }
    if (newPassword.length() < 8 || newPassword.length() > 72) {
      throw new IllegalArgumentException("New password must be between 8 and 72 characters");
    }
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
    return plainCode;
  }

  private void verifyCode(AccountChangeRequest pending, String code) {
    if (pending.getExpiresAt().isBefore(Instant.now())) {
      changeRequestRepository.delete(pending);
      throw new ExpiredEmailVerificationCodeException();
    }
    if (!passwordEncoder.matches(code, pending.getCodeHash())) {
      throw new InvalidEmailVerificationCodeException();
    }
  }

  private void sendVerificationEmail(User user, String plainCode, AccountChangeType changeType) {
    MailjetEmailClient client =
        mailjetEmailClient.orElseThrow(
            () -> new EmailDeliveryException("Email delivery is not configured"));
    String name = user.getFirstName() != null ? user.getFirstName() : user.getUserEmail();
    String changeLabel =
        switch (changeType) {
          case EMAIL -> "email address";
          case PASSWORD -> "password";
          case SMS -> "SMS phone number";
        };
    String subject = "Confirm your Honest Car account change";
    String text =
        "Your verification code to change your "
            + changeLabel
            + " is "
            + plainCode
            + ". It expires in "
            + properties.getCodeExpirationMinutes()
            + " minutes.";
    String html =
        "<p>Your verification code to change your <strong>"
            + changeLabel
            + "</strong> is <strong>"
            + plainCode
            + "</strong>. It expires in "
            + properties.getCodeExpirationMinutes()
            + " minutes.</p>";
    client.sendEmail(user.getUserEmail(), name, subject, text, html);
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
