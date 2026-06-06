package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.authentication.EmailVerificationService;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.dto.ChangePasswordRequest;
import com.capstone.models.dto.DeleteAccountRequest;
import com.capstone.models.dto.UpdateAccountRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountServiceTest {

  @Test
  void shouldReturnSafeAccountProfileForCurrentUser() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    AccountService service = service(userRepository);
    AuthenticatedUser principal = authenticatedPrincipal();
    User storedUser = storedUser();

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));

    var response = service.getAccount(principal);

    assertEquals(1L, response.userId());
    assertEquals("driver@example.com", response.email());
    assertEquals("Pat", response.firstName());
    assertEquals("Driver", response.lastName());
    assertEquals("+15551234567", response.userSms());
    assertTrue(response.emailVerified());
    assertEquals(storedUser.getEmailVerifiedAt(), response.emailVerifiedAt());
    assertEquals(storedUser.getCreatedAt(), response.createdAt());
    assertEquals(storedUser.getUpdatedAt(), response.updatedAt());
    assertTrue(response.emailVerified());
  }

  @Test
  void shouldUpdateProfileWithTrimmedValues() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    AccountService service = service(userRepository, emailVerificationService);
    User storedUser = storedUser();
    UpdateAccountRequest request =
        new UpdateAccountRequest(" Patricia ", " Driver-Smith ", " DRIVER@Example.COM ", "   ");

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
    when(userRepository.findByUserEmail("driver@example.com")).thenReturn(Optional.of(storedUser));
    when(userRepository.save(storedUser)).thenReturn(storedUser);

    var response = service.updateProfile(authenticatedPrincipal(), request);

    assertEquals("Patricia", storedUser.getFirstName());
    assertEquals("Driver-Smith", storedUser.getLastName());
    assertEquals("driver@example.com", storedUser.getUserEmail());
    assertNull(storedUser.getUserSms());
    assertEquals("Patricia", response.firstName());
    assertEquals("driver@example.com", response.email());
    verify(userRepository).save(storedUser);
    verifyNoInteractions(emailVerificationService);
  }

  @Test
  void shouldResetVerificationAndSendCodeWhenEmailChanges() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
    AccountService service = service(userRepository, emailVerificationService);
    User storedUser = storedUser();
    storedUser.setEmailVerified(true);
    storedUser.setEmailVerifiedAt(Instant.parse("2026-01-02T03:04:00Z"));
    UpdateAccountRequest request =
        new UpdateAccountRequest("Pat", "Driver", " NEW.Driver@Example.COM ", null);

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
    when(userRepository.findByUserEmail("new.driver@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(storedUser)).thenReturn(storedUser);

    var response = service.updateProfile(authenticatedPrincipal(), request);

    assertEquals("new.driver@example.com", storedUser.getUserEmail());
    assertFalse(storedUser.isEmailVerified());
    assertNull(storedUser.getEmailVerifiedAt());
    assertFalse(response.emailVerified());
    verify(emailVerificationService).sendRegistrationCode(storedUser);
  }

  @Test
  void shouldUpdateProfileWithNormalizedNewEmailAndTrimmedPhone() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    AccountService service = service(userRepository);
    User storedUser = storedUser();
    UpdateAccountRequest request =
        new UpdateAccountRequest("Pat", "Driver", " NEW.Driver@Example.COM ", " +1 555 222 3333 ");

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
    when(userRepository.findByUserEmail("new.driver@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(storedUser)).thenReturn(storedUser);

    var response = service.updateProfile(authenticatedPrincipal(), request);

    assertEquals("new.driver@example.com", storedUser.getUserEmail());
    assertEquals("+1 555 222 3333", storedUser.getUserSms());
    assertEquals("new.driver@example.com", response.email());
    assertEquals("+1 555 222 3333", response.userSms());
  }

  @Test
  void shouldRejectProfileUpdateWhenEmailBelongsToDifferentUser() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    AccountService service = service(userRepository);
    User storedUser = storedUser();
    User otherUser = storedUser();
    otherUser.setUserId(2L);
    UpdateAccountRequest request =
        new UpdateAccountRequest("Pat", "Driver", "taken@example.com", null);

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
    when(userRepository.findByUserEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

    assertThrows(
        DuplicateEmailException.class,
        () -> service.updateProfile(authenticatedPrincipal(), request));
    verify(userRepository, never()).save(any());
  }

  @Test
  void shouldRejectProfileUpdateWhenEmailBecomesDuplicateDuringSave() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    AccountService service = service(userRepository);
    User storedUser = storedUser();
    UpdateAccountRequest request =
        new UpdateAccountRequest("Pat", "Driver", "taken@example.com", null);

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
    when(userRepository.findByUserEmail("taken@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(storedUser))
        .thenThrow(new DataIntegrityViolationException("duplicate email"));

    assertThrows(
        DuplicateEmailException.class,
        () -> service.updateProfile(authenticatedPrincipal(), request));
  }

  @Test
  void shouldChangePasswordWhenCurrentPasswordMatches() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    AccountService service = service(userRepository, passwordEncoder);
    User storedUser = storedUser();

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
    when(passwordEncoder.matches("old-secret", "encoded-old")).thenReturn(true);
    when(passwordEncoder.encode("new-secret")).thenReturn("encoded-new");

    service.changePassword(
        authenticatedPrincipal(), new ChangePasswordRequest("old-secret", "new-secret"));

    assertEquals("encoded-new", storedUser.getUserPw());
    verify(userRepository).save(storedUser);
  }

  @Test
  void shouldRejectAccountAccessWhenPrincipalIsMissing() {
    AccountService service = service(mock(UserRepositoryJPA.class));

    assertThrows(InvalidAccountCredentialsException.class, () -> service.getAccount(null));
  }

  @Test
  void shouldRejectAccountAccessWhenUserRecordMissing() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    AccountService service = service(userRepository);

    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(
        InvalidAccountCredentialsException.class,
        () -> service.getAccount(authenticatedPrincipal()));
  }

  @Test
  void shouldRejectAccountDeletionWhenPasswordDoesNotMatch() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    UserDeletionService userDeletionService = mock(UserDeletionService.class);
    AccountService service = service(userRepository, passwordEncoder, userDeletionService);

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser()));
    when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

    assertThrows(
        InvalidAccountCredentialsException.class,
        () -> service.deleteAccount(authenticatedPrincipal(), new DeleteAccountRequest("wrong")));
    verify(userDeletionService, never()).deleteUserAndRelatedData(any());
  }

  @Test
  void shouldRejectPasswordChangeWhenCurrentPasswordDoesNotMatch() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    AccountService service = service(userRepository, passwordEncoder);

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser()));
    when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

    assertThrows(
        InvalidAccountCredentialsException.class,
        () ->
            service.changePassword(
                authenticatedPrincipal(), new ChangePasswordRequest("wrong", "new-secret")));
  }

  @Test
  void shouldDelegateAccountDeletionToUserDeletionService() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    UserDeletionService userDeletionService = mock(UserDeletionService.class);
    AccountService service = service(userRepository, passwordEncoder, userDeletionService);
    User storedUser = storedUser();

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
    when(passwordEncoder.matches("secret", "encoded-old")).thenReturn(true);

    service.deleteAccount(authenticatedPrincipal(), new DeleteAccountRequest("secret"));

    InOrder inOrder = inOrder(userDeletionService);
    inOrder.verify(userDeletionService).deleteUserAndRelatedData(storedUser);
    verify(userRepository, never()).delete(any(User.class));
  }

  private AccountService service(UserRepositoryJPA userRepository) {
    return service(userRepository, mock(PasswordEncoder.class));
  }

  private AccountService service(
      UserRepositoryJPA userRepository, PasswordEncoder passwordEncoder) {
    return service(userRepository, passwordEncoder, mock(UserDeletionService.class));
  }

  private AccountService service(
      UserRepositoryJPA userRepository, EmailVerificationService emailVerificationService) {
    return new AccountService(
        userRepository,
        mock(PasswordEncoder.class),
        mock(UserDeletionService.class),
        emailVerificationService);
  }

  private AccountService service(
      UserRepositoryJPA userRepository,
      PasswordEncoder passwordEncoder,
      UserDeletionService userDeletionService) {
    return new AccountService(
        userRepository, passwordEncoder, userDeletionService, mock(EmailVerificationService.class));
  }

  private AuthenticatedUser authenticatedPrincipal() {
    return new AuthenticatedUser(1L, "driver@example.com", Role.USER);
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
    user.setEmailVerifiedAt(Instant.parse("2026-01-02T03:04:00Z"));
    user.setCreatedAt(Instant.parse("2026-01-02T03:04:00Z"));
    user.setUpdatedAt(Instant.parse("2026-02-03T04:05:00Z"));
    return user;
  }
}
