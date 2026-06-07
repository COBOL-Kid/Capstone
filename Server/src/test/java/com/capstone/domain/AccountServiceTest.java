package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.capstone.authentication.AuthenticatedUser;
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
  void shouldUpdateProfileWithTrimmedNamesOnly() {
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    AccountService service = service(userRepository);
    User storedUser = storedUser();
    UpdateAccountRequest request = new UpdateAccountRequest(" Patricia ", " Driver-Smith ");

    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
    when(userRepository.save(storedUser)).thenReturn(storedUser);

    var response = service.updateProfile(authenticatedPrincipal(), request);

    assertEquals("Patricia", storedUser.getFirstName());
    assertEquals("Driver-Smith", storedUser.getLastName());
    assertEquals("driver@example.com", storedUser.getUserEmail());
    assertEquals("+15551234567", storedUser.getUserSms());
    assertEquals("Patricia", response.firstName());
    assertEquals("driver@example.com", response.email());
    verify(userRepository).save(storedUser);
  }

  @Test
  void shouldRejectDirectPasswordChange() {
    AccountService service = service(mock(UserRepositoryJPA.class));

    assertThrows(
        AccountChangeRequiredException.class,
        () ->
            service.changePassword(
                authenticatedPrincipal(), new ChangePasswordRequest("old-secret", "new-secret")));
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
      UserRepositoryJPA userRepository,
      PasswordEncoder passwordEncoder,
      UserDeletionService userDeletionService) {
    return new AccountService(userRepository, passwordEncoder, userDeletionService);
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
