package com.capstone.domain;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.logging.AuditLog;
import com.capstone.models.User;
import com.capstone.models.dto.AccountResponse;
import com.capstone.models.dto.ChangePasswordRequest;
import com.capstone.models.dto.DeleteAccountRequest;
import com.capstone.models.dto.UpdateAccountRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private final UserRepositoryJPA userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserDeletionService userDeletionService;

  public AccountService(
      UserRepositoryJPA userRepository,
      PasswordEncoder passwordEncoder,
      UserDeletionService userDeletionService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.userDeletionService = userDeletionService;
  }

  @Transactional(readOnly = true)
  public AccountResponse getAccount(AuthenticatedUser principal) {
    return toResponse(loadCurrentUser(principal));
  }

  @Transactional
  public AccountResponse updateProfile(AuthenticatedUser principal, UpdateAccountRequest request) {
    User user = loadCurrentUser(principal);
    user.setFirstName(cleanRequired(request.firstName(), "First name is required"));
    user.setLastName(cleanRequired(request.lastName(), "Last name is required"));
    User savedUser = userRepository.save(user);
    return toResponse(savedUser);
  }

  @Transactional
  public void changePassword(AuthenticatedUser principal, ChangePasswordRequest request) {
    throw new AccountChangeRequiredException();
  }

  @Transactional
  public void deleteAccount(AuthenticatedUser principal, DeleteAccountRequest request) {
    User user = loadCurrentUser(principal);
    if (!passwordEncoder.matches(request.password(), user.getUserPw())) {
      throw new InvalidAccountCredentialsException();
    }
    Long userId = user.getUserId();
    userDeletionService.deleteUserAndRelatedData(user);
    AuditLog.info("account_deleted", "userId", userId);
  }

  private User loadCurrentUser(AuthenticatedUser principal) {
    if (principal == null || principal.userId() == null) {
      throw new InvalidAccountCredentialsException();
    }
    return userRepository
        .findById(principal.userId())
        .orElseThrow(InvalidAccountCredentialsException::new);
  }

  private AccountResponse toResponse(User user) {
    return new AccountResponse(
        user.getUserId(),
        user.getUserEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getUserSms(),
        user.isEmailVerified(),
        user.getEmailVerifiedAt(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  private String cleanRequired(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }
}
