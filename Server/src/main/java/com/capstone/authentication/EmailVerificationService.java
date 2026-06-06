package com.capstone.authentication;

import com.capstone.configuration.EmailVerificationProperties;
import com.capstone.data.EmailVerificationCodeRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.integration.EmailDeliveryException;
import com.capstone.integration.MailjetEmailClient;
import com.capstone.models.EmailVerificationCode;
import com.capstone.models.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final EmailVerificationCodeRepositoryJPA verificationCodeRepository;
  private final UserRepositoryJPA userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailVerificationProperties properties;
  private final Optional<MailjetEmailClient> mailjetEmailClient;

  public EmailVerificationService(
      EmailVerificationCodeRepositoryJPA verificationCodeRepository,
      UserRepositoryJPA userRepository,
      PasswordEncoder passwordEncoder,
      EmailVerificationProperties properties,
      Optional<MailjetEmailClient> mailjetEmailClient) {
    this.verificationCodeRepository = verificationCodeRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.mailjetEmailClient = mailjetEmailClient;
  }

  @Transactional
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
    String plainCode = generateCode();
    EmailVerificationCode verificationCode = new EmailVerificationCode();
    verificationCode.setUser(user);
    verificationCode.setCodeHash(passwordEncoder.encode(plainCode));
    verificationCode.setExpiresAt(
        Instant.now().plus(Duration.ofMinutes(properties.getCodeExpirationMinutes())));
    verificationCode.setSignInChallengeHash(signInChallengeHash);
    verificationCode.setCreatedAt(Instant.now());
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
      throw new InvalidEmailVerificationCodeException();
    }

    verificationCodeRepository.deleteByUser(user);
  }

  private void markVerified(User user) {
    user.setEmailVerified(true);
    user.setEmailVerifiedAt(Instant.now());
    userRepository.save(user);
  }

  private void sendVerificationEmail(User user, String plainCode) {
    MailjetEmailClient client =
        mailjetEmailClient.orElseThrow(
            () -> new EmailDeliveryException("Email delivery is not configured"));
    String name = user.getFirstName() != null ? user.getFirstName() : user.getUserEmail();
    client.sendEmail(
        user.getUserEmail(),
        name,
        "Verify your Honest Car account",
        "Your verification code is "
            + plainCode
            + ". It expires in "
            + properties.getCodeExpirationMinutes()
            + " minutes.",
        "<p>Your verification code is <strong>"
            + plainCode
            + "</strong>. It expires in "
            + properties.getCodeExpirationMinutes()
            + " minutes.</p>");
  }

  private static String generateCode() {
    int value = RANDOM.nextInt(900_000) + 100_000;
    return Integer.toString(value);
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
