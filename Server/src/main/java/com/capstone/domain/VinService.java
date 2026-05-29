package com.capstone.domain;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.CompletedRecallRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.UserVin;
import com.capstone.models.dto.UpdateMileageRequest;
import com.capstone.models.dto.UserVehicleResponse;
import com.capstone.models.dto.VehicleDetailResponse;
import com.capstone.read.VehicleReadService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VinService {

  private final VinRepositoryJPA vinRepositoryJPA;
  private final UserVinRepositoryJPA userVinRepositoryJPA;
  private final CompletedMaintenanceRepositoryJPA completedMaintenanceRepositoryJPA;
  private final CompletedRecallRepositoryJPA completedRecallRepositoryJPA;
  private final VehicleReadService vehicleReadService;

  public VinService(
      VinRepositoryJPA vinRepositoryJPA,
      UserVinRepositoryJPA userVinRepositoryJPA,
      CompletedMaintenanceRepositoryJPA completedMaintenanceRepositoryJPA,
      CompletedRecallRepositoryJPA completedRecallRepositoryJPA,
      VehicleReadService vehicleReadService) {
    this.vinRepositoryJPA = vinRepositoryJPA;
    this.userVinRepositoryJPA = userVinRepositoryJPA;
    this.completedMaintenanceRepositoryJPA = completedMaintenanceRepositoryJPA;
    this.completedRecallRepositoryJPA = completedRecallRepositoryJPA;
    this.vehicleReadService = vehicleReadService;
  }

  @Transactional(readOnly = true)
  public List<UserVehicleResponse> findVinsByUserId(long userId) {
    return vehicleReadService.findVehiclesForUser(userId);
  }

  @Transactional(readOnly = true)
  public Optional<VehicleDetailResponse> findVehicleDetailForUser(long userId, String vin) {
    return vehicleReadService.findVehicleDetail(userId, vin);
  }

  @Transactional
  public Optional<VehicleDetailResponse> updateMileage(
      long userId, String vin, UpdateMileageRequest request) {
    String normalizedVin = VinNormalizer.normalize(vin);
    return userVinRepositoryJPA
        .findForUserVin(userId, normalizedVin)
        .map(
            userVin -> {
              userVin.setCurrentMileage(request.currentMileage());
              userVinRepositoryJPA.save(userVin);
              return vehicleReadService.findVehicleDetail(userId, normalizedVin).orElseThrow();
            });
  }

  @Transactional
  public boolean deleteVin(long userId, String vin) {
    String normalizedVin = VinNormalizer.normalize(vin);
    return userVinRepositoryJPA
        .findForUserVin(userId, normalizedVin)
        .map(
            userVin -> {
              completedMaintenanceRepositoryJPA.deleteAllForUserVin(userId, normalizedVin);
              completedRecallRepositoryJPA.deleteAllForUserVin(userId, normalizedVin);
              userVinRepositoryJPA.deleteForUserVin(userId, normalizedVin);
              vinRepositoryJPA.deleteOrphanedVins(List.of(normalizedVin));
              return true;
            })
        .orElse(false);
  }

  @Transactional
  public Optional<UserVehicleResponse> updateSelectedImage(
      long userId, String vin, String selectedImageUrl) {
    String normalizedVin = VinNormalizer.normalize(vin);
    String normalizedImageUrl = selectedImageUrl != null ? selectedImageUrl.trim() : null;
    if (normalizedImageUrl == null || normalizedImageUrl.isBlank()) {
      throw new IllegalArgumentException("Selected image URL is required");
    }
    return userVinRepositoryJPA
        .findForUserVin(userId, normalizedVin)
        .map(userVin -> updateSelectedImage(userVin, normalizedImageUrl, userId, normalizedVin));
  }

  private UserVehicleResponse updateSelectedImage(
      UserVin userVin, String selectedImageUrl, long userId, String normalizedVin) {
    if (!userVin.getAvailableImageUrls().contains(selectedImageUrl)) {
      throw new IllegalArgumentException(
          "Selected image URL must be one of the available vehicle images");
    }
    userVin.setSelectedImageUrl(selectedImageUrl);
    userVinRepositoryJPA.save(userVin);
    return vehicleReadService.findVehiclesForUser(userId).stream()
        .filter(vehicle -> vehicle.vin().equals(normalizedVin))
        .findFirst()
        .orElseThrow();
  }
}
