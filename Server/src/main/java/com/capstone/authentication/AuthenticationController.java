package com.capstone.authentication;

import com.capstone.configuration.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

  private final AuthenticationService service;
  private final JwtProperties jwtProperties;
  private final AuthCookies authCookies;

  public AuthenticationController(
      AuthenticationService service, JwtProperties jwtProperties, AuthCookies authCookies) {
    this.service = service;
    this.jwtProperties = jwtProperties;
    this.authCookies = authCookies;
  }

  @GetMapping("/csrf")
  public ResponseEntity<Void> csrf() {
    return ResponseEntity.noContent().build();
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
  public ResponseEntity<AuthenticationResponse> refresh(HttpServletRequest request) {
    String refreshToken =
        authCookies
            .readRefreshToken(request)
            .orElseThrow(() -> new InvalidRefreshTokenException("Missing refresh token cookie"));
    return sessionResponse(service.refreshToken(refreshToken));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest request) {
    authCookies.readRefreshToken(request).ifPresent(refreshToken -> service.logout(refreshToken));
    return ResponseEntity.ok().headers(authCookies.clearSessionCookies()).build();
  }

  private ResponseEntity<AuthenticationResponse> sessionResponse(AuthenticationResponse response) {
    if (response.hasRefreshToken()) {
      long refreshMaxAge = Duration.ofDays(jwtProperties.getRefreshExpirationDays()).getSeconds();
      long accessMaxAge = Duration.ofMinutes(jwtProperties.getExpirationMinutes()).getSeconds();
      return ResponseEntity.ok()
          .headers(
              authCookies.setSessionCookies(
                  response.getToken(), response.getRefreshToken(), accessMaxAge, refreshMaxAge))
          .body(response);
    }
    return ResponseEntity.ok().body(response);
  }
}
