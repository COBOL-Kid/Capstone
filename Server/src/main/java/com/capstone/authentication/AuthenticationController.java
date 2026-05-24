package com.capstone.authentication;

import com.capstone.configuration.JwtProperties;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    AuthenticationResponse response = service.register(request);
    return ResponseEntity.ok()
        .headers(createCookieHeader(response.getRefreshToken()))
        .body(response);
  }

  @PostMapping("/authenticate")
  public ResponseEntity<AuthenticationResponse> authenticate(
      @Valid @RequestBody AuthenticationRequest request) {
    AuthenticationResponse response = service.authenticate(request);
    return ResponseEntity.ok()
        .headers(createCookieHeader(response.getRefreshToken()))
        .body(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthenticationResponse> refresh(
      @CookieValue(name = "refreshToken", required = false) String refreshToken) {
    if (refreshToken == null || refreshToken.isEmpty()) {
      throw new InvalidRefreshTokenException("Missing refresh token cookie");
    }
    AuthenticationResponse response = service.refreshToken(refreshToken);
    return ResponseEntity.ok()
        .headers(createCookieHeader(response.getRefreshToken()))
        .body(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(
      @CookieValue(name = "refreshToken", required = false) String refreshToken) {
    if (refreshToken != null && !refreshToken.isEmpty()) {
      service.logout(refreshToken);
    }
    return ResponseEntity.ok().headers(createCleanCookieHeader()).build();
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
