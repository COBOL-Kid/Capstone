package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.authentication.AuthenticationResponse;
import com.capstone.authentication.AuthenticationService;
import com.capstone.configuration.EmailVerificationProperties;
import com.capstone.data.AccountChangeRequestRepositoryJPA;
import com.capstone.data.RefreshTokenRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.email.ExpiredEmailVerificationCodeException;
import com.capstone.email.InvalidEmailVerificationCodeException;
import com.capstone.email.MailjetEmailClient;
import com.capstone.email.VerificationEmailComposer;
import com.capstone.models.AccountChangeRequest;
import com.capstone.models.AccountChangeType;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.dto.InitiateAccountChangeRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountChangeServiceTest {

  private UserRepositoryJPA userRepository;
  private AccountChangeRequestRepositoryJPA changeRequestRepository;
  private RefreshTokenRepositoryJPA refreshTokenRepository;
  private PasswordEncoder passwordEncoder;
  private EmailVerificationProperties properties;
  private MailjetEmailClient mailjetEmailClient;
  private VerificationEmailComposer verificationEmailComposer;
  private AuthenticationService authenticationService;
  private AccountChangeService service;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepositoryJPA.class);
    changeRequestRepository = mock(AccountChangeRequestRepositoryJPA.class);
    refreshTokenRepository = mock(RefreshTokenRepositoryJPA.class);
    passwordEncoder = mock(PasswordEncoder.class);
    properties = new EmailVerificationProperties();
    mailjetEmailClient = mock(MailjetEmailClient.class);
    verificationEmailComposer = new VerificationEmailComposer();
    authenticationService = mock(AuthenticationService.class);
    service =
        new AccountChangeService(
            userRepository,
            changeRequestRepository,
            refreshTokenRepository,
            passwordEncoder,
            properties,
            Optional.of(mailjetEmailClient),
            verificationEmailComposer,
            authenticationService);
  }

  @Test
  void shouldInitiateEmailChangeAndSendCodeToCurrentEmail() {
    User user = storedUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.findByUserEmail("new.driver@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encoded-code");

    var response =
        service.initiateChange(
            authenticatedPrincipal(),
            new InitiateAccountChangeRequest(
                AccountChangeType.EMAIL, "new.driver@example.com", null, null, null));

    assertEquals(AccountChangeType.EMAIL, response.changeType());
    assertEquals(5, response.expiresInMinutes());
    verify(changeRequestRepository).deleteByUser(user);

    ArgumentCaptor<AccountChangeRequest> captor =
        ArgumentCaptor.forClass(AccountChangeRequest.class);
    verify(changeRequestRepository).save(captor.capture());
    assertEquals("new.driver@example.com", captor.getValue().getNewEmail());
    verify(mailjetEmailClient)
        .sendEmail(
            eq("driver@example.com"),
            eq("Pat"),
            eq("Confirm your Honest Car account change"),
            contains("email address"),
            contains("email address"));
  }

  @Test
  void shouldInitiatePasswordChangeWithBrandedEmail() {
    User user = storedUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("current-secret", "encoded-old")).thenReturn(true);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded-code", "encoded-new-password");

    service.initiateChange(
        authenticatedPrincipal(),
        new InitiateAccountChangeRequest(
            AccountChangeType.PASSWORD, null, "current-secret", "New-secret1!", null));

    ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailjetEmailClient)
        .sendEmail(
            eq("driver@example.com"),
            eq("Pat"),
            eq("Confirm your Honest Car account change"),
            contains("change your password"),
            htmlCaptor.capture());
    String html = htmlCaptor.getValue();
    assertTrue(html.contains("Confirm your password change"));
    assertTrue(html.contains("Honest Car"));
    assertTrue(html.contains("#2f6fb4"));
  }

  @Test
  void shouldVerifyEmailChangeAndIssueNewSession() {
    User user = storedUser();
    AccountChangeRequest pending = pendingRequest(user, AccountChangeType.EMAIL);
    pending.setNewEmail("new.driver@example.com");
    pending.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
    pending.setCodeHash("encoded-code");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(changeRequestRepository.findByUser(user)).thenReturn(Optional.of(pending));
    when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true);
    when(userRepository.findByUserEmail("new.driver@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(user)).thenReturn(user);
    when(authenticationService.createSession(user))
        .thenReturn(AuthenticationResponse.verifiedSession("new-token", "refresh-token"));

    var result = service.verifyChange(authenticatedPrincipal(), "123456");

    assertEquals("new.driver@example.com", user.getUserEmail());
    assertTrue(user.isEmailVerified());
    assertNotNull(user.getEmailVerifiedAt());
    assertEquals("new-token", result.session().getToken());
    verify(changeRequestRepository).delete(pending);
  }

  @Test
  void shouldVerifyPasswordChange() {
    User user = storedUser();
    AccountChangeRequest pending = pendingRequest(user, AccountChangeType.PASSWORD);
    pending.setNewPasswordHash("encoded-new-password");
    pending.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
    pending.setCodeHash("encoded-code");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(changeRequestRepository.findByUser(user)).thenReturn(Optional.of(pending));
    when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true);
    when(userRepository.save(user)).thenReturn(user);

    var result = service.verifyChange(authenticatedPrincipal(), "123456");

    assertEquals("encoded-new-password", user.getUserPw());
    assertNull(result.session());
    assertEquals(AccountChangeType.PASSWORD, result.changeType());
    verify(refreshTokenRepository).deleteByUser(user);
    verify(authenticationService, never()).createSession(any());
  }

  @Test
  void shouldVerifySmsChange() {
    User user = storedUser();
    AccountChangeRequest pending = pendingRequest(user, AccountChangeType.SMS);
    pending.setNewUserSms("+1 555 999 0000");
    pending.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
    pending.setCodeHash("encoded-code");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(changeRequestRepository.findByUser(user)).thenReturn(Optional.of(pending));
    when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true);
    when(userRepository.save(user)).thenReturn(user);

    service.verifyChange(authenticatedPrincipal(), "123456");

    assertEquals("+1 555 999 0000", user.getUserSms());
  }

  @Test
  void shouldRejectInvalidVerificationCode() {
    User user = storedUser();
    AccountChangeRequest pending = pendingRequest(user, AccountChangeType.SMS);
    pending.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
    pending.setCodeHash("encoded-code");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(changeRequestRepository.findByUser(user)).thenReturn(Optional.of(pending));
    when(passwordEncoder.matches("000000", "encoded-code")).thenReturn(false);

    assertThrows(
        InvalidEmailVerificationCodeException.class,
        () -> service.verifyChange(authenticatedPrincipal(), "000000"));
    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldRejectExpiredVerificationCode() {
    User user = storedUser();
    AccountChangeRequest pending = pendingRequest(user, AccountChangeType.EMAIL);
    pending.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
    pending.setCodeHash("encoded-code");

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(changeRequestRepository.findByUser(user)).thenReturn(Optional.of(pending));

    assertThrows(
        ExpiredEmailVerificationCodeException.class,
        () -> service.verifyChange(authenticatedPrincipal(), "123456"));
    verify(changeRequestRepository).delete(pending);
  }

  @Test
  void shouldRejectPasswordInitiationWhenCurrentPasswordIsWrong() {
    User user = storedUser();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

    assertThrows(
        InvalidAccountCredentialsException.class,
        () ->
            service.initiateChange(
                authenticatedPrincipal(),
                new InitiateAccountChangeRequest(
                    AccountChangeType.PASSWORD, null, "wrong", "new-secret-1", null)));
    verify(changeRequestRepository, never()).save(any());
  }

  @Test
  void shouldRejectDuplicateEmailOnInitiation() {
    User user = storedUser();
    User otherUser = storedUser();
    otherUser.setUserId(2L);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.findByUserEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

    assertThrows(
        DuplicateEmailException.class,
        () ->
            service.initiateChange(
                authenticatedPrincipal(),
                new InitiateAccountChangeRequest(
                    AccountChangeType.EMAIL, "taken@example.com", null, null, null)));
  }

  @Test
  void shouldResendCodeForPendingChange() {
    User user = storedUser();
    AccountChangeRequest pending = pendingRequest(user, AccountChangeType.PASSWORD);
    pending.setNewPasswordHash("encoded-new-password");
    pending.setCreatedAt(Instant.now().minusSeconds(120));

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(changeRequestRepository.findByUser(user)).thenReturn(Optional.of(pending));
    when(passwordEncoder.encode(anyString())).thenReturn("new-encoded-code");

    var response = service.resendCode(authenticatedPrincipal());

    assertEquals(AccountChangeType.PASSWORD, response.changeType());
    verify(changeRequestRepository).save(pending);
    verify(mailjetEmailClient).sendEmail(any(), any(), any(), any(), any());
  }

  @Test
  void shouldReturnPendingChangeWhenPresent() {
    User user = storedUser();
    AccountChangeRequest pending = pendingRequest(user, AccountChangeType.SMS);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(changeRequestRepository.findByUser(user)).thenReturn(Optional.of(pending));

    var pendingChange = service.getPendingChange(authenticatedPrincipal());

    assertTrue(pendingChange.isPresent());
    assertEquals(AccountChangeType.SMS, pendingChange.get().changeType());
  }

  private AuthenticatedUser authenticatedPrincipal() {
    return new AuthenticatedUser(1L, "driver@example.com", Role.USER, true);
  }

  private User storedUser() {
    User user = new User();
    user.setUserId(1L);
    user.setUserEmail("driver@example.com");
    user.setFirstName("Pat");
    user.setLastName("Driver");
    user.setUserSms("+15551234567");
    user.setUserPw("encoded-old");
    user.setEmailVerified(true);
    return user;
  }

  private AccountChangeRequest pendingRequest(User user, AccountChangeType changeType) {
    AccountChangeRequest pending = new AccountChangeRequest();
    pending.setUser(user);
    pending.setChangeType(changeType);
    pending.setCreatedAt(Instant.now());
    return pending;
  }
}
