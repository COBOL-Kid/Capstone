package com.capstone.domain;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.authentication.EmailNormalizer;
import com.capstone.data.*;
import com.capstone.models.User;
import com.capstone.models.dto.AccountResponse;
import com.capstone.models.dto.ChangePasswordRequest;
import com.capstone.models.dto.DeleteAccountRequest;
import com.capstone.models.dto.UpdateAccountRequest;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private final UserRepositoryJPA userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenRepositoryJPA refreshTokenRepository;
  private final CompletedMaintenanceRepositoryJPA completedMaintenanceRepository;
  private final CompletedRecallRepositoryJPA completedRecallRepository;
  private final UserVinRepositoryJPA userVinRepository;
  private final VinRepositoryJPA vinRepository;

  public AccountService(
      UserRepositoryJPA userRepository,
      PasswordEncoder passwordEncoder,
      RefreshTokenRepositoryJPA refreshTokenRepository,
      CompletedMaintenanceRepositoryJPA completedMaintenanceRepository,
      CompletedRecallRepositoryJPA completedRecallRepository,
      UserVinRepositoryJPA userVinRepository,
      VinRepositoryJPA vinRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.refreshTokenRepository = refreshTokenRepository;
    this.completedMaintenanceRepository = completedMaintenanceRepository;
    this.completedRecallRepository = completedRecallRepository;
    this.userVinRepository = userVinRepository;
    this.vinRepository = vinRepository;
  }

  @Transactional(readOnly = true)
  public AccountResponse getAccount(AuthenticatedUser principal) {
    return toResponse(loadCurrentUser(principal));
  }

  @Transactional
  public AccountResponse updateProfile(AuthenticatedUser principal, UpdateAccountRequest request) {
    User user = loadCurrentUser(principal);
    String normalizedEmail = EmailNormalizer.normalize(request.email());
    userRepository
        .findByUserEmail(normalizedEmail)
        .filter(existingUser -> !existingUser.getUserId().equals(user.getUserId()))
        .ifPresent(
            existingUser -> {
              throw new DuplicateEmailException();
            });
    user.setFirstName(cleanRequired(request.firstName(), "First name is required"));
    user.setLastName(cleanRequired(request.lastName(), "Last name is required"));
    user.setUserEmail(normalizedEmail);
    user.setUserSms(cleanOptional(request.userSms()));
    try {
      return toResponse(userRepository.save(user));
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateEmailException();
    }
  }

  @Transactional
  public void changePassword(AuthenticatedUser principal, ChangePasswordRequest request) {
    User user = loadCurrentUser(principal);
    if (!passwordEncoder.matches(request.currentPassword(), user.getUserPw())) {
      throw new InvalidAccountCredentialsException();
    }
    user.setUserPw(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
  }

  @Transactional
  public void deleteAccount(AuthenticatedUser principal, DeleteAccountRequest request) {
    User user = loadCurrentUser(principal);
    if (!passwordEncoder.matches(request.password(), user.getUserPw())) {
      throw new InvalidAccountCredentialsException();
    }
    Long userId = user.getUserId();
    List<String> userVins = userVinRepository.findVinNumbersForUser(userId);
    completedMaintenanceRepository.deleteAllForUserId(userId);
    completedRecallRepository.deleteAllForUserId(userId);
    userVinRepository.deleteAllForUserId(userId);
    if (!userVins.isEmpty()) {
      vinRepository.deleteOrphanedVins(userVins);
    }
    refreshTokenRepository.deleteByUser(user);
    userRepository.delete(user);
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
        user.getCreatedAt(),
        user.getUpdatedAt());
  }

  private String cleanRequired(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }

  private String cleanOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
