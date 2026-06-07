package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.capstone.configuration.JwtProperties;
import com.capstone.data.RefreshTokenRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.DuplicateEmailException;
import com.capstone.email.EmailVerificationService;
import com.capstone.models.RefreshToken;
import com.capstone.models.Role;
import com.capstone.models.User;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthenticationServiceTest {

  @Test
  void shouldRegisterUserWithEncodedPasswordTokenClaimsAndVerificationCode() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    JwtService jwtService = mock(JwtService.class);
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    JwtProperties jwtProperties = mock(JwtProperties.class);
    EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    AuthenticationService service =
        authenticationService(
            repository,
            refreshTokenRepositoryJPA,
            passwordEncoder,
            jwtService,
            authenticationManager,
            jwtProperties,
            emailVerificationService);
    RegisterRequest request =
        new RegisterRequest(" Pat ", " Driver ", " DRIVER@Example.COM ", "secret");

    when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);
    when(refreshTokenRepositoryJPA.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
    when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("jwt-token");

    var response = service.register(request);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(repository).saveAndFlush(userCaptor.capture());
    User savedUser = userCaptor.getValue();
    assertEquals("Pat", savedUser.getFirstName());
    assertEquals("Driver", savedUser.getLastName());
    assertEquals("driver@example.com", savedUser.getUserEmail());
    assertEquals("encoded-secret", savedUser.getUserPw());
    assertEquals(Role.USER, savedUser.getRole());
    assertFalse(savedUser.isEmailVerified());
    assertEquals("jwt-token", response.getToken());
    assertEquals(Boolean.FALSE, response.getEmailVerified());
    verify(emailVerificationService).sendRegistrationCode(savedUser);
    verify(refreshTokenRepositoryJPA).save(any(RefreshToken.class));
    assertTokenClaims(jwtService, savedUser, false);
  }

  @Test
  void shouldRejectDuplicateRegistrationEmail() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    AuthenticationService service =
        authenticationService(
            repository,
            mock(RefreshTokenRepositoryJPA.class),
            mock(PasswordEncoder.class),
            mock(JwtService.class),
            mock(AuthenticationManager.class),
            mock(JwtProperties.class),
            mock(EmailVerificationService.class));

    when(repository.existsByUserEmail("driver@example.com")).thenReturn(true);

    assertThrows(
        DuplicateEmailException.class,
        () ->
            service.register(
                new RegisterRequest("Pat", "Driver", " Driver@Example.COM ", "secret")));
    verify(repository, never()).saveAndFlush(any(User.class));
  }

  @Test
  void shouldTranslateDataIntegrityViolationOnConcurrentRegistrationToDuplicateEmail() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    AuthenticationService service =
        authenticationService(
            repository,
            mock(RefreshTokenRepositoryJPA.class),
            passwordEncoder,
            mock(JwtService.class),
            mock(AuthenticationManager.class),
            mock(JwtProperties.class),
            mock(EmailVerificationService.class));

    when(repository.existsByUserEmail("driver@example.com")).thenReturn(false);
    when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
    when(repository.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("uk_user_email"));

    assertThrows(
        DuplicateEmailException.class,
        () ->
            service.register(new RegisterRequest("Pat", "Driver", "driver@example.com", "secret")));
  }

  @Test
  void shouldAuthenticateVerifiedUserAndIssueSession() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    JwtService jwtService = mock(JwtService.class);
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    JwtProperties jwtProperties = mock(JwtProperties.class);
    AuthenticationService service =
        authenticationService(
            repository,
            refreshTokenRepositoryJPA,
            mock(PasswordEncoder.class),
            jwtService,
            authenticationManager,
            jwtProperties,
            mock(EmailVerificationService.class));
    AuthenticationRequest request = new AuthenticationRequest(" DRIVER@Example.COM ", "secret");
    User user = verifiedUser();

    when(repository.findByUserEmail("driver@example.com")).thenReturn(Optional.of(user));
    when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);
    when(refreshTokenRepositoryJPA.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("jwt-token");

    var response = service.authenticate(request);

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    assertEquals("jwt-token", response.getToken());
    assertEquals(Boolean.TRUE, response.getEmailVerified());
    assertNull(response.getVerificationRequired());
    verify(refreshTokenRepositoryJPA).deleteByUser(user);
    verify(refreshTokenRepositoryJPA).save(any(RefreshToken.class));
    assertTokenClaims(jwtService, user, true);
  }

  @Test
  void shouldRequireVerificationForUnverifiedSignIn() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    AuthenticationService service =
        authenticationService(
            repository,
            refreshTokenRepositoryJPA,
            mock(PasswordEncoder.class),
            mock(JwtService.class),
            authenticationManager,
            mock(JwtProperties.class),
            emailVerificationService);
    User user = unverifiedUser();

    when(repository.findByUserEmail("driver@example.com")).thenReturn(Optional.of(user));
    when(emailVerificationService.sendSignInCode(user)).thenReturn("challenge-token");

    var response = service.authenticate(new AuthenticationRequest("driver@example.com", "secret"));

    assertNull(response.getToken());
    assertEquals(Boolean.TRUE, response.getVerificationRequired());
    assertEquals("challenge-token", response.getVerificationChallenge());
    verify(emailVerificationService).sendSignInCode(user);
    verifyNoInteractions(refreshTokenRepositoryJPA);
  }

  @Test
  void shouldRejectAuthenticationWhenAccountIsLocked() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    AuthenticationService service =
        authenticationService(
            repository,
            mock(RefreshTokenRepositoryJPA.class),
            mock(PasswordEncoder.class),
            mock(JwtService.class),
            authenticationManager,
            mock(JwtProperties.class),
            mock(EmailVerificationService.class));
    User user = unverifiedUser();
    user.setLockoutEnd(java.time.LocalDateTime.now().plusMinutes(10));

    when(repository.findByUserEmail("driver@example.com")).thenReturn(Optional.of(user));

    assertThrows(
        org.springframework.security.authentication.LockedException.class,
        () -> service.authenticate(new AuthenticationRequest("driver@example.com", "secret")));
    verifyNoInteractions(authenticationManager);
  }

  @Test
  void shouldIncrementFailedAttemptsOnBadCredentials() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    AuthenticationService service =
        authenticationService(
            repository,
            mock(RefreshTokenRepositoryJPA.class),
            mock(PasswordEncoder.class),
            mock(JwtService.class),
            authenticationManager,
            mock(JwtProperties.class),
            mock(EmailVerificationService.class));
    User user = unverifiedUser();
    user.setFailedLoginAttempts(2);

    when(repository.findByUserEmail("driver@example.com")).thenReturn(Optional.of(user));
    when(authenticationManager.authenticate(any()))
        .thenThrow(
            new org.springframework.security.authentication.BadCredentialsException("Invalid"));

    assertThrows(
        org.springframework.security.authentication.BadCredentialsException.class,
        () -> service.authenticate(new AuthenticationRequest("driver@example.com", "wrong")));
    assertEquals(3, user.getFailedLoginAttempts());
    verify(repository).save(user);
  }

  @Test
  void shouldLogoutByDeletingRefreshToken() {
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    AuthenticationService service =
        authenticationService(
            mock(UserRepositoryJPA.class),
            refreshTokenRepositoryJPA,
            mock(PasswordEncoder.class),
            mock(JwtService.class),
            mock(AuthenticationManager.class),
            mock(JwtProperties.class),
            mock(EmailVerificationService.class));

    service.logout("refresh-token");

    verify(refreshTokenRepositoryJPA).deleteByToken("refresh-token");
  }

  @Test
  void shouldRotateRefreshTokenWithCurrentVerificationState() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    JwtService jwtService = mock(JwtService.class);
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    JwtProperties jwtProperties = mock(JwtProperties.class);
    AuthenticationService service =
        authenticationService(
            repository,
            refreshTokenRepositoryJPA,
            mock(PasswordEncoder.class),
            jwtService,
            mock(AuthenticationManager.class),
            jwtProperties,
            mock(EmailVerificationService.class));

    User user = verifiedUser();
    RefreshToken oldToken = new RefreshToken();
    oldToken.setId(42L);
    oldToken.setToken("old-refresh-token");
    oldToken.setUser(user);
    oldToken.setExpiryDate(java.time.Instant.now().plusSeconds(3600));

    when(refreshTokenRepositoryJPA.findByToken("old-refresh-token"))
        .thenReturn(Optional.of(oldToken));
    when(refreshTokenRepositoryJPA.deleteByIdReturning(42L)).thenReturn(1);
    when(repository.findById(1L)).thenReturn(Optional.of(user));
    when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);
    when(refreshTokenRepositoryJPA.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("new-jwt-token");

    var response = service.refreshToken("old-refresh-token");

    assertEquals("new-jwt-token", response.getToken());
    assertEquals(Boolean.TRUE, response.getEmailVerified());
    assertTokenClaims(jwtService, user, true);
  }

  @Test
  void shouldRejectRefreshWhenTokenAlreadyConsumed() {
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    AuthenticationService service =
        authenticationService(
            mock(UserRepositoryJPA.class),
            refreshTokenRepositoryJPA,
            mock(PasswordEncoder.class),
            mock(JwtService.class),
            mock(AuthenticationManager.class),
            mock(JwtProperties.class),
            mock(EmailVerificationService.class));

    User user = verifiedUser();
    RefreshToken oldToken = new RefreshToken();
    oldToken.setId(42L);
    oldToken.setToken("old-refresh-token");
    oldToken.setUser(user);
    oldToken.setExpiryDate(java.time.Instant.now().plusSeconds(3600));

    when(refreshTokenRepositoryJPA.findByToken("old-refresh-token"))
        .thenReturn(Optional.of(oldToken));
    when(refreshTokenRepositoryJPA.deleteByIdReturning(42L)).thenReturn(0);

    assertThrows(
        InvalidRefreshTokenException.class, () -> service.refreshToken("old-refresh-token"));
    verify(refreshTokenRepositoryJPA, never()).save(any(RefreshToken.class));
  }

  @Test
  void shouldRejectRefreshWhenTokenIsExpired() {
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    AuthenticationService service =
        authenticationService(
            mock(UserRepositoryJPA.class),
            refreshTokenRepositoryJPA,
            mock(PasswordEncoder.class),
            mock(JwtService.class),
            mock(AuthenticationManager.class),
            mock(JwtProperties.class),
            mock(EmailVerificationService.class));

    User user = verifiedUser();
    RefreshToken oldToken = new RefreshToken();
    oldToken.setId(42L);
    oldToken.setToken("old-refresh-token");
    oldToken.setUser(user);
    oldToken.setExpiryDate(java.time.Instant.now().minusSeconds(60));

    when(refreshTokenRepositoryJPA.findByToken("old-refresh-token"))
        .thenReturn(Optional.of(oldToken));

    assertThrows(
        InvalidRefreshTokenException.class, () -> service.refreshToken("old-refresh-token"));
    verify(refreshTokenRepositoryJPA).delete(oldToken);
    verify(refreshTokenRepositoryJPA, never()).deleteByIdReturning(any());
  }

  @Test
  void shouldRejectRefreshWhenTokenIsUnknown() {
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    AuthenticationService service =
        authenticationService(
            mock(UserRepositoryJPA.class),
            refreshTokenRepositoryJPA,
            mock(PasswordEncoder.class),
            mock(JwtService.class),
            mock(AuthenticationManager.class),
            mock(JwtProperties.class),
            mock(EmailVerificationService.class));

    when(refreshTokenRepositoryJPA.findByToken("missing")).thenReturn(Optional.empty());

    assertThrows(InvalidRefreshTokenException.class, () -> service.refreshToken("missing"));
  }

  @Test
  void shouldVerifyEmailAndIssueFreshSession() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    JwtService jwtService = mock(JwtService.class);
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    JwtProperties jwtProperties = mock(JwtProperties.class);
    EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    AuthenticationService service =
        authenticationService(
            repository,
            refreshTokenRepositoryJPA,
            mock(PasswordEncoder.class),
            jwtService,
            mock(AuthenticationManager.class),
            jwtProperties,
            emailVerificationService);
    AuthenticatedUser principal = new AuthenticatedUser(1L, "driver@example.com", Role.USER);
    User user = verifiedUser();

    when(repository.findById(1L)).thenReturn(Optional.of(user));
    when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);
    when(refreshTokenRepositoryJPA.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("verified-token");

    var response = service.verifyEmail(principal, "123456");

    verify(emailVerificationService).verifyAuthenticatedUser(user, "123456");
    assertEquals("verified-token", response.getToken());
    assertEquals(Boolean.TRUE, response.getEmailVerified());
  }

  @Test
  void shouldCompleteSignInVerificationAndIssueSession() {
    UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
    JwtService jwtService = mock(JwtService.class);
    RefreshTokenRepositoryJPA refreshTokenRepositoryJPA = mock(RefreshTokenRepositoryJPA.class);
    JwtProperties jwtProperties = mock(JwtProperties.class);
    EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    AuthenticationService service =
        authenticationService(
            repository,
            refreshTokenRepositoryJPA,
            mock(PasswordEncoder.class),
            jwtService,
            mock(AuthenticationManager.class),
            jwtProperties,
            emailVerificationService);
    User user = verifiedUser();
    CompleteEmailVerificationRequest request =
        new CompleteEmailVerificationRequest("challenge-token", "123456");

    when(emailVerificationService.completeSignIn("challenge-token", "123456")).thenReturn(user);
    when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);
    when(refreshTokenRepositoryJPA.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jwtService.generateToken(anyMap(), eq(user))).thenReturn("session-token");

    var response = service.completeEmailVerificationSignIn(request);

    assertEquals("session-token", response.getToken());
    assertEquals(Boolean.TRUE, response.getEmailVerified());
  }

  @Test
  void shouldRedactSensitiveValuesFromToString() {
    assertFalse(
        new RegisterRequest("Pat", "Driver", "driver@example.com", "secret")
            .toString()
            .contains("secret"));
    assertFalse(
        new AuthenticationRequest("driver@example.com", "secret").toString().contains("secret"));
    assertFalse(new AuthenticationResponse("jwt-token").toString().contains("jwt-token"));
  }

  private AuthenticationService authenticationService(
      UserRepositoryJPA repository,
      RefreshTokenRepositoryJPA refreshTokenRepositoryJPA,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthenticationManager authenticationManager,
      JwtProperties jwtProperties,
      EmailVerificationService emailVerificationService) {
    return new AuthenticationService(
        repository,
        refreshTokenRepositoryJPA,
        passwordEncoder,
        jwtService,
        authenticationManager,
        jwtProperties,
        emailVerificationService);
  }

  private static User unverifiedUser() {
    User user = new User();
    user.setUserId(1L);
    user.setFirstName("Pat");
    user.setLastName("Driver");
    user.setUserEmail("driver@example.com");
    user.setUserPw("encoded-secret");
    user.setRole(Role.USER);
    user.setEmailVerified(false);
    return user;
  }

  private static User verifiedUser() {
    User user = unverifiedUser();
    user.setEmailVerified(true);
    return user;
  }

  @SuppressWarnings("unchecked")
  private void assertTokenClaims(JwtService jwtService, User user, boolean emailVerified) {
    ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(jwtService)
        .generateToken(claimsCaptor.capture(), org.mockito.ArgumentMatchers.same(user));
    Map<String, Object> claims = claimsCaptor.getValue();
    assertFalse(claims.containsKey("firstName"));
    assertFalse(claims.containsKey("lastName"));
    assertEquals(user.getUserId(), claims.get("userId"));
    assertEquals(user.getRole().name(), claims.get("role"));
    assertEquals(emailVerified, claims.get("emailVerified"));
  }
}
