package com.capstone.email;

import com.capstone.configuration.EmailVerificationProperties;
import com.capstone.data.EmailVerificationCodeRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.TooManyRequestsException;
import com.capstone.models.EmailVerificationCode;
import com.capstone.models.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

  private final EmailVerificationCodeRepositoryJPA verificationCodeRepository;
  private final UserRepositoryJPA userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationProperties properties;
  private final EmailDeliveryGateway emailDeliveryGateway;
  private final VerificationCodeGenerator verificationCodeGenerator;
  private final VerificationEmailComposer verificationEmailComposer;

  public EmailVerificationService(
      EmailVerificationCodeRepositoryJPA verificationCodeRepository,
      UserRepositoryJPA userRepository,
      PasswordEncoder passwordEncoder,
      EmailVerificationProperties properties,
      EmailDeliveryGateway emailDeliveryGateway,
      VerificationCodeGenerator verificationCodeGenerator,
      VerificationEmailComposer verificationEmailComposer) {
    this.verificationCodeRepository = verificationCodeRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.emailDeliveryGateway = emailDeliveryGateway;
    this.verificationCodeGenerator = verificationCodeGenerator;
    this.verificationEmailComposer = verificationEmailComposer;
  }

  @Transactional(noRollbackFor = EmailDeliveryException.class)
  public void sendRegistrationCode(User user) {
    String plainCode = issueCode(user, null);
    sendVerificationEmail(user, plainCode);
  }

  @Transactional
  public String sendSignInCode(User user) {
    String challenge = UUID.randomUUID().toString();
    String plainCode = issueCode(user, hashChallenge(challenge));
    sendVerificationEmail(user, plainCode);
    return challenge;
  }

  @Transactional
  public void resendCode(User user) {
    enforceResendCooldown(user);
    String plainCode = issueCode(user, null);
    sendVerificationEmail(user, plainCode);
  }

  @Transactional
  public void verifyAuthenticatedUser(User user, String code) {
    verifyCode(user, code, null);
    markVerified(user);
  }

  @Transactional
  public User completeSignIn(String verificationChallenge, String code) {
    EmailVerificationCode stored =
        verificationCodeRepository
            .findBySignInChallengeHash(hashChallenge(verificationChallenge))
            .orElseThrow(InvalidEmailVerificationCodeException::new);
    User user = stored.getUser();
    verifyCode(user, code, stored);
    markVerified(user);
    return user;
  }

  @Transactional
  public void deleteExpiredCodes() {
    verificationCodeRepository.deleteByExpiresAtBefore(Instant.now());
  }

  private String issueCode(User user, String signInChallengeHash) {
    verificationCodeRepository.deleteByUser(user);
    String plainCode = verificationCodeGenerator.generate();
    EmailVerificationCode verificationCode = new EmailVerificationCode();
    verificationCode.setUser(user);
    verificationCode.setCodeHash(passwordEncoder.encode(plainCode));
    verificationCode.setExpiresAt(
        Instant.now().plus(Duration.ofMinutes(properties.getCodeExpirationMinutes())));
    verificationCode.setSignInChallengeHash(signInChallengeHash);
    verificationCode.setCreatedAt(Instant.now());
    verificationCode.setFailedAttempts(0);
    verificationCodeRepository.save(verificationCode);
    return plainCode;
  }

  private void verifyCode(User user, String code, EmailVerificationCode stored) {
    EmailVerificationCode verificationCode =
        stored != null
            ? stored
            : verificationCodeRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(InvalidEmailVerificationCodeException::new);

    if (verificationCode.getExpiresAt().isBefore(Instant.now())) {
      verificationCodeRepository.delete(verificationCode);
      throw new ExpiredEmailVerificationCodeException();
    }

    if (!passwordEncoder.matches(code, verificationCode.getCodeHash())) {
      handleFailedVerificationAttempt(verificationCode);
    }

    verificationCodeRepository.deleteByUser(user);
  }

  private void handleFailedVerificationAttempt(EmailVerificationCode verificationCode) {
    int attempts = verificationCode.getFailedAttempts() + 1;
    verificationCode.setFailedAttempts(attempts);
    if (attempts >= properties.getMaxVerificationAttempts()) {
      verificationCodeRepository.delete(verificationCode);
      throw new TooManyRequestsException(
          "Too many invalid verification attempts. Request a new code and try again.");
    }
    verificationCodeRepository.save(verificationCode);
    throw new InvalidEmailVerificationCodeException();
  }

  private void enforceResendCooldown(User user) {
    verificationCodeRepository
        .findTopByUserOrderByCreatedAtDesc(user)
        .ifPresent(
            existing -> {
              Instant earliestResend =
                  existing.getCreatedAt().plusSeconds(properties.getResendCooldownSeconds());
              if (Instant.now().isBefore(earliestResend)) {
                throw new TooManyRequestsException(
                    "Please wait before requesting another verification code.");
              }
            });
  }

  private void markVerified(User user) {
    user.setEmailVerified(true);
    user.setEmailVerifiedAt(Instant.now());
    userRepository.save(user);
  }

  private void sendVerificationEmail(User user, String plainCode) {
    String name = user.getFirstName() != null ? user.getFirstName() : user.getUserEmail();
    EmailContent content =
        verificationEmailComposer.compose(name, plainCode, properties.getCodeExpirationMinutes());
    emailDeliveryGateway.sendEmail(
        user.getUserEmail(), name, content.subject(), content.textPart(), content.htmlPart());
  }

  private static String hashChallenge(String challenge) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(challenge.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
