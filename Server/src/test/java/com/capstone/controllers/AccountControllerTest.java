package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.capstone.authentication.AuthCookies;
import com.capstone.authentication.AuthenticatedUser;
import com.capstone.authentication.AuthenticationResponse;
import com.capstone.configuration.CookieSecurityProperties;
import com.capstone.configuration.JwtProperties;
import com.capstone.domain.AccountChangeService;
import com.capstone.domain.AccountChangeVerificationResult;
import com.capstone.domain.AccountService;
import com.capstone.models.AccountChangeType;
import com.capstone.models.Role;
import com.capstone.models.dto.*;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AccountControllerTest {

  @Test
  void shouldRequireAuthentication() {
    AccountController controller =
        controller(mock(AccountService.class), mock(AccountChangeService.class));

    assertEquals(HttpStatus.UNAUTHORIZED, controller.getAccount(null).getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller.updateProfile(null, new UpdateAccountRequest("Pat", "Driver")).getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller
            .changePassword(null, new ChangePasswordRequest("old-secret", "new-secret"))
            .getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller.deleteAccount(null, new DeleteAccountRequest("secret")).getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller
            .initiateChange(
                null,
                new InitiateAccountChangeRequest(
                    AccountChangeType.EMAIL, "new@example.com", null, null, null))
            .getStatusCode());
  }

  @Test
  void shouldReturnCurrentAccount() {
    AccountService accountService = mock(AccountService.class);
    AccountController controller = controller(accountService, mock(AccountChangeService.class));
    AuthenticatedUser user = user();
    AccountResponse account = account();

    when(accountService.getAccount(user)).thenReturn(account);

    var response = controller.getAccount(user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(account, response.getBody());
  }

  @Test
  void shouldUpdateCurrentAccountNames() {
    AccountService accountService = mock(AccountService.class);
    AccountController controller = controller(accountService, mock(AccountChangeService.class));
    AuthenticatedUser authenticatedUser = user();
    UpdateAccountRequest request = new UpdateAccountRequest("Patricia", "Driver");
    AccountResponse accountResponse = account();

    when(accountService.updateProfile(authenticatedUser, request)).thenReturn(accountResponse);

    var response = controller.updateProfile(authenticatedUser, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(accountResponse, response.getBody());
  }

  @Test
  void shouldInitiateAndVerifyAccountChange() {
    AccountChangeService accountChangeService = mock(AccountChangeService.class);
    AccountController controller = controller(mock(AccountService.class), accountChangeService);
    AuthenticatedUser authenticatedUser = user();
    InitiateAccountChangeRequest initiateRequest =
        new InitiateAccountChangeRequest(
            AccountChangeType.PASSWORD, null, "old-secret", "new-secret-1", null);
    AccountChangeInitiatedResponse initiated =
        new AccountChangeInitiatedResponse(AccountChangeType.PASSWORD, 5);
    AccountResponse accountResponse = account();
    AuthenticationResponse session =
        AuthenticationResponse.verifiedSession("fresh-token", "refresh-token");
    AccountChangeVerificationResult verificationResult =
        new AccountChangeVerificationResult(accountResponse, session);

    when(accountChangeService.initiateChange(authenticatedUser, initiateRequest))
        .thenReturn(initiated);
    when(accountChangeService.verifyChange(authenticatedUser, "123456"))
        .thenReturn(verificationResult);

    assertEquals(
        HttpStatus.OK,
        controller.initiateChange(authenticatedUser, initiateRequest).getStatusCode());
  }

  @Test
  void shouldReturnNoContentWhenNoPendingChange() {
    AccountChangeService accountChangeService = mock(AccountChangeService.class);
    AccountController controller = controller(mock(AccountService.class), accountChangeService);
    AuthenticatedUser authenticatedUser = user();

    when(accountChangeService.getPendingChange(authenticatedUser)).thenReturn(Optional.empty());

    assertEquals(
        HttpStatus.NO_CONTENT, controller.getPendingChange(authenticatedUser).getStatusCode());
  }

  @Test
  void shouldChangePasswordAndDeleteAccount() {
    AccountService accountService = mock(AccountService.class);
    AccountController controller = controller(accountService, mock(AccountChangeService.class));
    AuthenticatedUser authenticatedUser = user();
    ChangePasswordRequest passwordRequest = new ChangePasswordRequest("old-secret", "new-secret");
    DeleteAccountRequest deleteRequest = new DeleteAccountRequest("new-secret");

    assertEquals(
        HttpStatus.NO_CONTENT,
        controller.changePassword(authenticatedUser, passwordRequest).getStatusCode());
    assertEquals(
        HttpStatus.NO_CONTENT,
        controller.deleteAccount(authenticatedUser, deleteRequest).getStatusCode());
    verify(accountService).changePassword(authenticatedUser, passwordRequest);
    verify(accountService).deleteAccount(authenticatedUser, deleteRequest);
  }

  private AccountController controller(
      AccountService accountService, AccountChangeService accountChangeService) {
    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setRefreshExpirationDays(7);
    return new AccountController(
        accountService,
        accountChangeService,
        jwtProperties,
        new AuthCookies(new CookieSecurityProperties()));
  }

  private AuthenticatedUser user() {
    return new AuthenticatedUser(1L, "driver@example.com", Role.USER, true);
  }

  private AccountResponse account() {
    return new AccountResponse(
        1L,
        "driver@example.com",
        "Pat",
        "Driver",
        "+15551234567",
        true,
        Instant.parse("2026-01-02T03:04:00Z"),
        Instant.parse("2026-01-02T03:04:00Z"),
        Instant.parse("2026-02-03T04:05:00Z"));
  }
}
