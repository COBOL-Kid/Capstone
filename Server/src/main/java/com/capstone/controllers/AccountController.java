package com.capstone.controllers;

import com.capstone.domain.AccountService;
import com.capstone.models.User;
import com.capstone.models.dto.ChangePasswordRequest;
import com.capstone.models.dto.DeleteAccountRequest;
import com.capstone.models.dto.UpdateAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping("/me")
  public ResponseEntity<?> getAccount(@AuthenticationPrincipal User user) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(accountService.getAccount(user), HttpStatus.OK);
  }

  @PatchMapping("/me")
  public ResponseEntity<?> updateProfile(
      @AuthenticationPrincipal User user, @Valid @RequestBody UpdateAccountRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(accountService.updateProfile(user, request), HttpStatus.OK);
  }

  @PostMapping("/password")
  public ResponseEntity<?> changePassword(
      @AuthenticationPrincipal User user, @Valid @RequestBody ChangePasswordRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    accountService.changePassword(user, request);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @PostMapping("/delete")
  public ResponseEntity<?> deleteAccount(
      @AuthenticationPrincipal User user, @Valid @RequestBody DeleteAccountRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    accountService.deleteAccount(user, request);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
