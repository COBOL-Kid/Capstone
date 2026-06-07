package com.capstone.controllers;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.authentication.AuthenticationResponse;
import com.capstone.configuration.JwtProperties;
import com.capstone.domain.AccountChangeService;
import com.capstone.domain.AccountChangeVerificationResult;
import com.capstone.domain.AccountService;
import com.capstone.models.dto.*;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final AccountService accountService;
  private final AccountChangeService accountChangeService;
  private final JwtProperties jwtProperties;

  public AccountController(
      AccountService accountService,
      AccountChangeService accountChangeService,
      JwtProperties jwtProperties) {
    this.accountService = accountService;
    this.accountChangeService = accountChangeService;
    this.jwtProperties = jwtProperties;
  }

  @GetMapping("/me")
  public ResponseEntity<?> getAccount(@AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(accountService.getAccount(user), HttpStatus.OK);
  }

  @PatchMapping("/me")
  public ResponseEntity<?> updateProfile(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody UpdateAccountRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(accountService.updateProfile(user, request), HttpStatus.OK);
  }

  @PostMapping("/password")
  public ResponseEntity<?> changePassword(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody ChangePasswordRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    accountService.changePassword(user, request);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @PostMapping("/change-requests")
  public ResponseEntity<?> initiateChange(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody InitiateAccountChangeRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(accountChangeService.initiateChange(user, request), HttpStatus.OK);
  }

  @PostMapping("/change-requests/verify")
  public ResponseEntity<VerifyAccountChangeResponse> verifyChange(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody VerifyAccountChangeRequest request) {
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    AccountChangeVerificationResult result =
        accountChangeService.verifyChange(user, request.code());
    VerifyAccountChangeResponse body = toVerifyResponse(result);
    AuthenticationResponse session = result.session();
    if (session != null && session.hasRefreshToken()) {
      return ResponseEntity.ok().headers(createCookieHeader(session.getRefreshToken())).body(body);
    }
    return ResponseEntity.ok(body);
  }

  @PostMapping("/change-requests/resend")
  public ResponseEntity<?> resendChangeCode(@AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(accountChangeService.resendCode(user), HttpStatus.OK);
  }

  @GetMapping("/change-requests/pending")
  public ResponseEntity<?> getPendingChange(@AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    Optional<PendingAccountChangeResponse> pending = accountChangeService.getPendingChange(user);
    return pending
        .<ResponseEntity<?>>map(response -> new ResponseEntity<>(response, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
  }

  @PostMapping("/delete")
  public ResponseEntity<?> deleteAccount(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody DeleteAccountRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    accountService.deleteAccount(user, request);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private VerifyAccountChangeResponse toVerifyResponse(AccountChangeVerificationResult result) {
    AuthenticationResponse session = result.session();
    if (session == null) {
      return new VerifyAccountChangeResponse(result.account(), null, null);
    }
    return new VerifyAccountChangeResponse(
        result.account(), session.getToken(), session.getEmailVerified());
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
}
