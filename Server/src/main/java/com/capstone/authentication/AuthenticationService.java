package com.capstone.authentication;

import com.capstone.configuration.JwtProperties;
import com.capstone.data.RefreshTokenRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.DuplicateEmailException;
import com.capstone.models.RefreshToken;
import com.capstone.models.Role;
import com.capstone.models.User;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final int LOCK_TIME_DURATION_MINUTES = 15;

  private final UserRepositoryJPA repository;
  private final RefreshTokenRepositoryJPA refreshTokenRepositoryJPA;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final JwtProperties jwtProperties;
  private final EmailVerificationService emailVerificationService;

  public AuthenticationService(
      UserRepositoryJPA repository,
      RefreshTokenRepositoryJPA refreshTokenRepositoryJPA,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthenticationManager authenticationManager,
      JwtProperties jwtProperties,
      EmailVerificationService emailVerificationService) {
    this.repository = repository;
    this.refreshTokenRepositoryJPA = refreshTokenRepositoryJPA;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.authenticationManager = authenticationManager;
    this.jwtProperties = jwtProperties;
    this.emailVerificationService = emailVerificationService;
  }

  @Transactional
  public AuthenticationResponse register(RegisterRequest request) {
    String email = EmailNormalizer.normalize(request.getEmail());
    if (repository.existsByUserEmail(email)) {
      throw new DuplicateEmailException();
    }
    User user = new User();
    user.setFirstName(clean(request.getFirstname()));
    user.setLastName(clean(request.getLastname()));
    user.setUserEmail(email);
    user.setUserPw(passwordEncoder.encode(request.getPassword()));
    user.setRole(Role.USER);
    user.setEmailVerified(false);

    try {
      repository.saveAndFlush(user);
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateEmailException();
    }

    emailVerificationService.sendRegistrationCode(user);

    String jwtToken = jwtService.generateToken(buildExtraClaims(user), user);
    RefreshToken refreshToken = createRefreshToken(user);

    log.info("Audit - User registered successfully: {}", email);

    return AuthenticationResponse.unverifiedSession(jwtToken, refreshToken.getToken());
  }

  @Transactional
  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    String email = EmailNormalizer.normalize(request.getEmail());
    User user =
        repository
            .findByUserEmail(email)
            .orElseThrow(
                () -> {
                  log.warn("Audit - Login failed, user not found: {}", email);
                  return new UsernameNotFoundException("Invalid email or password");
                });

    if (user.getLockoutEnd() != null && user.getLockoutEnd().isAfter(LocalDateTime.now())) {
      log.warn("Audit - Login attempt on locked account: {}", email);
      throw new LockedException("Account is locked");
    }

    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(email, request.getPassword()));
    } catch (BadCredentialsException ex) {
      user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
      log.warn(
          "Audit - Login failed for user: {}. Attempt {}/{}",
          email,
          user.getFailedLoginAttempts(),
          MAX_FAILED_ATTEMPTS);
      if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
        user.setLockoutEnd(LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION_MINUTES));
        log.warn("Audit - Account locked for user: {}", email);
      }
      repository.save(user);
      throw new BadCredentialsException("Invalid email or password", ex);
    }

    user.setFailedLoginAttempts(0);
    user.setLockoutEnd(null);
    repository.save(user);

    if (!user.isEmailVerified()) {
      String challenge = emailVerificationService.sendSignInCode(user);
      log.info("Audit - Login requires email verification for user: {}", email);
      return AuthenticationResponse.verificationRequired(challenge);
    }

    log.info("Audit - Login successful for user: {}", email);
    return issueSession(user);
  }

  @Transactional
  public AuthenticationResponse verifyEmail(AuthenticatedUser principal, String code) {
    User user = loadUser(principal);
    emailVerificationService.verifyAuthenticatedUser(user, code);
    repository.flush();
    user = repository.findById(user.getUserId()).orElseThrow();
    return issueSession(user);
  }

  @Transactional
  public AuthenticationResponse resendVerificationEmail(AuthenticatedUser principal) {
    User user = loadUser(principal);
    if (user.isEmailVerified()) {
      return issueSession(user);
    }
    emailVerificationService.resendCode(user);
    AuthenticationResponse response = new AuthenticationResponse();
    response.setEmailVerified(false);
    return response;
  }

  @Transactional
  public AuthenticationResponse completeEmailVerificationSignIn(
      CompleteEmailVerificationRequest request) {
    User user =
        emailVerificationService.completeSignIn(request.verificationChallenge(), request.code());
    log.info("Audit - Email verification sign-in completed for user: {}", user.getUserEmail());
    return issueSession(user);
  }

  private AuthenticationResponse issueSession(User user) {
    refreshTokenRepositoryJPA.deleteByUser(user);
    RefreshToken refreshToken = createRefreshToken(user);
    String jwtToken = jwtService.generateToken(buildExtraClaims(user), user);
    if (user.isEmailVerified()) {
      return AuthenticationResponse.verifiedSession(jwtToken, refreshToken.getToken());
    }
    return AuthenticationResponse.unverifiedSession(jwtToken, refreshToken.getToken());
  }

  private User loadUser(AuthenticatedUser principal) {
    if (principal == null || principal.userId() == null) {
      throw new UsernameNotFoundException("User not found");
    }
    return repository
        .findById(principal.userId())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  private Map<String, Object> buildExtraClaims(User user) {
    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("userId", user.getUserId());
    extraClaims.put("role", user.getRole().name());
    extraClaims.put("emailVerified", user.isEmailVerified());
    return extraClaims;
  }

  private String clean(String value) {
    return value != null ? value.trim() : null;
  }

  private RefreshToken createRefreshToken(User user) {
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);
    refreshToken.setToken(UUID.randomUUID().toString());
    refreshToken.setExpiryDate(
        Instant.now().plus(Duration.ofDays(jwtProperties.getRefreshExpirationDays())));
    return refreshTokenRepositoryJPA.save(refreshToken);
  }

  @Transactional
  public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepositoryJPA.delete(token);
      throw new InvalidRefreshTokenException("Refresh token was expired");
    }
    return token;
  }

  @Transactional
  public AuthenticationResponse refreshToken(String token) {
    RefreshToken oldToken =
        refreshTokenRepositoryJPA
            .findByToken(token)
            .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not recognized"));
    verifyExpiration(oldToken);
    if (refreshTokenRepositoryJPA.deleteByIdReturning(oldToken.getId()) != 1) {
      throw new InvalidRefreshTokenException("Refresh token already consumed");
    }
    User user = oldToken.getUser();
    user = repository.findById(user.getUserId()).orElseThrow();
    return issueSession(user);
  }

  @Transactional
  public void logout(String token) {
    refreshTokenRepositoryJPA.deleteByToken(token);
  }

  @Transactional
  public AuthenticationResponse createSession(User user) {
    return issueSession(user);
  }
}
