package com.capstone.domain;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.CompletedRecallRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;
import com.capstone.models.dto.UpdateMileageRequest;
import com.capstone.models.dto.UserVehicleResponse;
import com.capstone.models.dto.VehicleDetailResponse;
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

  public VinService(
      VinRepositoryJPA vinRepositoryJPA,
      UserVinRepositoryJPA userVinRepositoryJPA,
      CompletedMaintenanceRepositoryJPA completedMaintenanceRepositoryJPA,
      CompletedRecallRepositoryJPA completedRecallRepositoryJPA) {
    this.vinRepositoryJPA = vinRepositoryJPA;
    this.userVinRepositoryJPA = userVinRepositoryJPA;
    this.completedMaintenanceRepositoryJPA = completedMaintenanceRepositoryJPA;
    this.completedRecallRepositoryJPA = completedRecallRepositoryJPA;
  }

  public List<UserVehicleResponse> findVinsByUserId(Long userId) {
    return userVinRepositoryJPA.findAllForUser(userId).stream()
        .map(this::toUserVehicleResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<VehicleDetailResponse> findVehicleDetailForUser(Long userId, String vin) {
    String normalizedVin = normalizeVin(vin);
    return userVinRepositoryJPA
        .findForUserVin(userId, normalizedVin)
        .map(this::toVehicleDetailResponse);
  }

  @Transactional
  public Optional<VehicleDetailResponse> updateMileage(
      Long userId, String vin, UpdateMileageRequest request) {
    String normalizedVin = normalizeVin(vin);
    return userVinRepositoryJPA
        .findForUserVin(userId, normalizedVin)
        .map(
            userVin -> {
              userVin.setCurrentMileage(request.currentMileage());
              return toVehicleDetailResponse(userVinRepositoryJPA.save(userVin));
            });
  }

  @Transactional
  public boolean deleteVin(Long userId, String vin) {
    String normalizedVin = normalizeVin(vin);
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

  private VehicleDetailResponse toVehicleDetailResponse(UserVin userVin) {
    Vin vin = userVin.getVin();
    VehicleType vehicleType = vin.getVehicleType();
    return new VehicleDetailResponse(
        vin.getVin(),
        vehicleType.getVehicleTypeId(),
        vehicleType.getVehicleMake(),
        vehicleType.getVehicleModel(),
        vehicleType.getVehicleTrim(),
        vehicleType.getVehicleYear(),
        vehicleType.getVehicleStyle(),
        vehicleType.getSourceVin(),
        vehicleType.getOrigin(),
        vehicleType.getBody(),
        vehicleType.getEngineDescription(),
        vehicleType.getTransmissionStyle(),
        vehicleType.getDriveType(),
        vehicleType.getOwnersManual(),
        userVin.getCurrentMileage(),
        userVin.getAvailableImageUrls(),
        userVin.getSelectedImageUrl());
  }

  @Transactional
  public Optional<UserVehicleResponse> updateSelectedImage(
      Long userId, String vin, String selectedImageUrl) {
    String normalizedVin = normalizeVin(vin);
    String normalizedImageUrl = selectedImageUrl != null ? selectedImageUrl.trim() : null;
    if (normalizedImageUrl == null || normalizedImageUrl.isBlank()) {
      throw new IllegalArgumentException("Selected image URL is required");
    }
    return userVinRepositoryJPA
        .findForUserVin(userId, normalizedVin)
        .map(userVin -> updateSelectedImage(userVin, normalizedImageUrl));
  }

  private UserVehicleResponse updateSelectedImage(UserVin userVin, String selectedImageUrl) {
    if (!userVin.getAvailableImageUrls().contains(selectedImageUrl)) {
      throw new IllegalArgumentException(
          "Selected image URL must be one of the available vehicle images");
    }
    userVin.setSelectedImageUrl(selectedImageUrl);
    return toUserVehicleResponse(userVinRepositoryJPA.save(userVin));
  }

  private UserVehicleResponse toUserVehicleResponse(UserVin userVin) {
    Vin vin = userVin.getVin();
    VehicleType vehicleType = vin.getVehicleType();
    return new UserVehicleResponse(
        vin.getVin(),
        userVin.getCurrentMileage(),
        vehicleType.getVehicleTypeId(),
        vehicleType.getVehicleMake(),
        vehicleType.getVehicleModel(),
        vehicleType.getVehicleTrim(),
        vehicleType.getVehicleYear(),
        userVin.getAvailableImageUrls(),
        userVin.getSelectedImageUrl());
  }

  private String normalizeVin(String vin) {
    if (vin == null || vin.isBlank()) {
      throw new IllegalArgumentException("VIN is required");
    }
    String normalizedVin = vin.trim().toUpperCase();
    if (normalizedVin.length() != 17) {
      throw new IllegalArgumentException("VIN must be 17 characters");
    }
    return normalizedVin;
  }
}
