package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.capstone.configuration.JwtProperties;
import com.capstone.models.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class AuthenticationControllerTest {

  @Test
  void shouldSetRefreshCookieWhenSessionIncludesRefreshToken() {
    AuthenticationService service = mock(AuthenticationService.class);
    JwtProperties jwtProperties = mock(JwtProperties.class);
    AuthenticationController controller = new AuthenticationController(service, jwtProperties);
    AuthenticationResponse response =
        AuthenticationResponse.verifiedSession("jwt", "refresh-value");

    when(service.authenticate(any())).thenReturn(response);
    when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);

    var result = controller.authenticate(new AuthenticationRequest("driver@example.com", "secret"));

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertNotNull(result.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    assertTrue(result.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("refreshToken"));
    assertEquals("jwt", result.getBody().getToken());
  }

  @Test
  void shouldOmitRefreshCookieWhenVerificationIsRequired() {
    AuthenticationService service = mock(AuthenticationService.class);
    AuthenticationController controller =
        new AuthenticationController(service, mock(JwtProperties.class));
    AuthenticationResponse response =
        AuthenticationResponse.verificationRequired("challenge-token");

    when(service.authenticate(any())).thenReturn(response);

    var result = controller.authenticate(new AuthenticationRequest("driver@example.com", "secret"));

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertNull(result.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    assertEquals(Boolean.TRUE, result.getBody().getVerificationRequired());
    assertEquals("challenge-token", result.getBody().getVerificationChallenge());
  }

  @Test
  void shouldRequireAuthenticationForResendAndVerifyEndpoints() {
    AuthenticationController controller =
        new AuthenticationController(mock(AuthenticationService.class), mock(JwtProperties.class));

    assertEquals(HttpStatus.UNAUTHORIZED, controller.resendEmailVerification(null).getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller.verifyEmail(null, new VerifyEmailRequest("123456")).getStatusCode());
  }

  @Test
  void shouldDelegateAuthenticatedVerificationRequests() {
    AuthenticationService service = mock(AuthenticationService.class);
    JwtProperties jwtProperties = mock(JwtProperties.class);
    AuthenticationController controller = new AuthenticationController(service, jwtProperties);
    AuthenticatedUser user = new AuthenticatedUser(1L, "driver@example.com", Role.USER);
    AuthenticationResponse response = AuthenticationResponse.verifiedSession("jwt", "refresh");

    when(service.verifyEmail(user, "123456")).thenReturn(response);
    when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);

    var result = controller.verifyEmail(user, new VerifyEmailRequest("123456"));

    verify(service).verifyEmail(user, "123456");
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals("jwt", result.getBody().getToken());
  }
}
