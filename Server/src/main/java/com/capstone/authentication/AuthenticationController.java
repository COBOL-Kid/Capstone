package com.capstone.authentication;

import com.capstone.configuration.JwtProperties;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  private final AuthenticationService service;
  private final JwtProperties jwtProperties;

  public AuthenticationController(AuthenticationService service, JwtProperties jwtProperties) {
    this.service = service;
    this.jwtProperties = jwtProperties;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(
      @Valid @RequestBody RegisterRequest request) {
    return sessionResponse(service.register(request));
  }

  @PostMapping("/authenticate")
  public ResponseEntity<AuthenticationResponse> authenticate(
      @Valid @RequestBody AuthenticationRequest request) {
    return sessionResponse(service.authenticate(request));
  }

  @PostMapping("/email-verification/resend")
  public ResponseEntity<AuthenticationResponse> resendEmailVerification(
      @AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return sessionResponse(service.resendVerificationEmail(user));
  }

  @PostMapping("/email-verification/verify")
  public ResponseEntity<AuthenticationResponse> verifyEmail(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody VerifyEmailRequest request) {
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return sessionResponse(service.verifyEmail(user, request.code()));
  }

  @PostMapping("/email-verification/complete-sign-in")
  public ResponseEntity<AuthenticationResponse> completeEmailVerificationSignIn(
      @Valid @RequestBody CompleteEmailVerificationRequest request) {
    return sessionResponse(service.completeEmailVerificationSignIn(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthenticationResponse> refresh(
      @CookieValue(name = "refreshToken", required = false) String refreshToken) {
    if (refreshToken == null || refreshToken.isEmpty()) {
      throw new InvalidRefreshTokenException("Missing refresh token cookie");
    }
    return sessionResponse(service.refreshToken(refreshToken));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(
      @CookieValue(name = "refreshToken", required = false) String refreshToken) {
    if (refreshToken != null && !refreshToken.isEmpty()) {
      service.logout(refreshToken);
    }
    return ResponseEntity.ok().headers(createCleanCookieHeader()).build();
  }

  private ResponseEntity<AuthenticationResponse> sessionResponse(AuthenticationResponse response) {
    if (response.hasRefreshToken()) {
      return ResponseEntity.ok()
          .headers(createCookieHeader(response.getRefreshToken()))
          .body(response);
    }
    return ResponseEntity.ok().body(response);
  }

  private HttpHeaders createCookieHeader(String refreshToken) {
    long maxAgeSeconds = Duration.ofDays(jwtProperties.getRefreshExpirationDays()).getSeconds();
    ResponseCookie cookie =
        ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true)
            .path("/api/auth")
            .maxAge(maxAgeSeconds)
            .sameSite("Strict")
            .build();
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
    return headers;
  }

  private HttpHeaders createCleanCookieHeader() {
    ResponseCookie cookie =
        ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/api/auth")
            .maxAge(0)
            .sameSite("Strict")
            .build();
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
    return headers;
  }
}
