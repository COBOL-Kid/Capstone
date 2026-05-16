package com.capstone.controllers;

import com.capstone.domain.AccountService;
import com.capstone.models.User;
import com.capstone.models.dto.AccountResponse;
import com.capstone.models.dto.ChangePasswordRequest;
import com.capstone.models.dto.DeleteAccountRequest;
import com.capstone.models.dto.UpdateAccountRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    @Test
    void shouldRequireAuthentication() {
        AccountController controller = new AccountController(mock(AccountService.class));

        assertEquals(HttpStatus.UNAUTHORIZED, controller.getAccount(null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                controller.updateProfile(null, new UpdateAccountRequest("Pat", "Driver", "driver@example.com", null)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                controller.changePassword(null, new ChangePasswordRequest("old-secret", "new-secret")).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                controller.deleteAccount(null, new DeleteAccountRequest("secret")).getStatusCode());
    }

    @Test
    void shouldReturnCurrentAccount() {
        AccountService accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService);
        User user = user();
        AccountResponse account = account();

        when(accountService.getAccount(user)).thenReturn(account);

        var response = controller.getAccount(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(account, response.getBody());
    }

    @Test
    void shouldUpdateCurrentAccount() {
        AccountService accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService);
        User user = user();
        UpdateAccountRequest request = new UpdateAccountRequest("Patricia", "Driver", "driver@example.com", "+15551234567");
        AccountResponse account = account();

        when(accountService.updateProfile(user, request)).thenReturn(account);

        var response = controller.updateProfile(user, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(account, response.getBody());
    }

    @Test
    void shouldChangePasswordAndDeleteAccount() {
        AccountService accountService = mock(AccountService.class);
        AccountController controller = new AccountController(accountService);
        User user = user();
        ChangePasswordRequest passwordRequest = new ChangePasswordRequest("old-secret", "new-secret");
        DeleteAccountRequest deleteRequest = new DeleteAccountRequest("new-secret");

        assertEquals(HttpStatus.NO_CONTENT, controller.changePassword(user, passwordRequest).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteAccount(user, deleteRequest).getStatusCode());
        verify(accountService).changePassword(user, passwordRequest);
        verify(accountService).deleteAccount(user, deleteRequest);
    }

    private User user() {
        User user = new User();
        user.setUserId(1L);
        user.setUserEmail("driver@example.com");
        return user;
    }

    private AccountResponse account() {
        return new AccountResponse(1L, "driver@example.com", "Pat", "Driver", "+15551234567",
                Instant.parse("2026-01-02T03:04:00Z"), Instant.parse("2026-02-03T04:05:00Z"));
    }
}
