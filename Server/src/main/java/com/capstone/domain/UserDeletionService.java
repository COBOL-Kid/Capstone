package com.capstone.domain;

import com.capstone.data.*;
import com.capstone.models.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDeletionService {

  private final UserRepositoryJPA userRepository;
  private final RefreshTokenRepositoryJPA refreshTokenRepository;
  private final EmailVerificationCodeRepositoryJPA emailVerificationCodeRepository;
  private final CompletedMaintenanceRepositoryJPA completedMaintenanceRepository;
  private final CompletedRecallRepositoryJPA completedRecallRepository;
  private final UserVinRepositoryJPA userVinRepository;
  private final VinRepositoryJPA vinRepository;

  public UserDeletionService(
      UserRepositoryJPA userRepository,
      RefreshTokenRepositoryJPA refreshTokenRepository,
      EmailVerificationCodeRepositoryJPA emailVerificationCodeRepository,
      CompletedMaintenanceRepositoryJPA completedMaintenanceRepository,
      CompletedRecallRepositoryJPA completedRecallRepository,
      UserVinRepositoryJPA userVinRepository,
      VinRepositoryJPA vinRepository) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.emailVerificationCodeRepository = emailVerificationCodeRepository;
    this.completedMaintenanceRepository = completedMaintenanceRepository;
    this.completedRecallRepository = completedRecallRepository;
    this.userVinRepository = userVinRepository;
    this.vinRepository = vinRepository;
  }

  @Transactional
  public void deleteUserAndRelatedData(User user) {
    Long userId = user.getUserId();
    List<String> userVins = userVinRepository.findVinNumbersForUser(userId);
    completedMaintenanceRepository.deleteAllForUserId(userId);
    completedRecallRepository.deleteAllForUserId(userId);
    userVinRepository.deleteAllForUserId(userId);
    if (!userVins.isEmpty()) {
      vinRepository.deleteOrphanedVins(userVins);
    }
    emailVerificationCodeRepository.deleteByUser(user);
    refreshTokenRepository.deleteByUser(user);
    userRepository.delete(user);
  }
}
