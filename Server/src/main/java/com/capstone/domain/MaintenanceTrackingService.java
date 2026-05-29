package com.capstone.domain;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.MaintMileageRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.models.CompletedMaintenance;
import com.capstone.models.MaintMileage;
import com.capstone.models.UserVin;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import com.capstone.models.dto.CompletedMaintenanceResponse;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceTrackingService {

  private final CompletedMaintenanceRepositoryJPA completedMaintenanceRepository;
  private final MaintMileageRepositoryJPA maintMileageRepository;
  private final UserVinRepositoryJPA userVinRepository;

  public MaintenanceTrackingService(
      CompletedMaintenanceRepositoryJPA completedMaintenanceRepository,
      MaintMileageRepositoryJPA maintMileageRepository,
      UserVinRepositoryJPA userVinRepository) {
    this.completedMaintenanceRepository = completedMaintenanceRepository;
    this.maintMileageRepository = maintMileageRepository;
    this.userVinRepository = userVinRepository;
  }

  @Transactional
  public CompletedMaintenanceResponse completeMaintenance(
      long userId, CompleteMaintenanceRequest request) {
    if (request == null || request.maintMileageId() == null) {
      throw new IllegalArgumentException("Maintenance item is required");
    }
    if (request.mileageCompleted() == null) {
      throw new IllegalArgumentException("Mileage completed is required");
    }
    if (request.mileageCompleted() < 0) {
      throw new IllegalArgumentException("Mileage completed cannot be negative");
    }
    UserVin userVin =
        userVinRepository
            .findForUserVin(userId, VinNormalizer.normalize(request.vin()))
            .orElseThrow(VinNotAssociatedException::new);
    Long vehicleTypeId = userVin.getVin().getVehicleType().getVehicleTypeId();
    MaintMileage maintMileage =
        maintMileageRepository
            .findByMaintMileageIdAndVehicleTypeId_VehicleTypeId(
                request.maintMileageId(), vehicleTypeId)
            .orElseThrow(MaintenanceItemNotFoundException::new);
    CompletedMaintenance completedMaintenance =
        completedMaintenanceRepository
            .findByUserVinAndMaintMileage(userVin, maintMileage)
            .orElseGet(() -> saveCompletedMaintenance(userVin, maintMileage, request));
    return toResponse(completedMaintenance);
  }

  @Transactional
  public boolean uncompleteMaintenance(long userId, Long completedMaintenanceId) {
    if (completedMaintenanceId == null) {
      throw new IllegalArgumentException("Completed maintenance id is required");
    }
    CompletedMaintenance completedMaintenance =
        completedMaintenanceRepository
            .findById(completedMaintenanceId)
            .orElseThrow(MaintenanceItemNotFoundException::new);
    if (!completedMaintenance.getUserVin().getId().getUserId().equals(userId)) {
      throw new MaintenanceItemNotFoundException();
    }
    completedMaintenanceRepository.delete(completedMaintenance);
    return true;
  }

  private CompletedMaintenance saveCompletedMaintenance(
      UserVin userVin, MaintMileage maintMileage, CompleteMaintenanceRequest request) {
    CompletedMaintenance completedMaintenance =
        new CompletedMaintenance(
            userVin,
            maintMileage,
            request.completedDate() != null ? request.completedDate() : LocalDate.now(),
            request.mileageCompleted());
    completedMaintenance.setCost(request.cost());
    completedMaintenance.setNotes(request.notes());
    return completedMaintenanceRepository.save(completedMaintenance);
  }

  private CompletedMaintenanceResponse toResponse(CompletedMaintenance completedMaintenance) {
    MaintMileage maintMileage = completedMaintenance.getMaintMileage();
    return new CompletedMaintenanceResponse(
        completedMaintenance.getCompletedMaintenanceId(),
        completedMaintenance.getUserVin().getVin().getVin(),
        maintMileage.getMaintMileageId(),
        completedMaintenance.getCompletedDate(),
        completedMaintenance.getMileageCompleted(),
        completedMaintenance.getCost(),
        completedMaintenance.getNotes(),
        maintMileage.getMaintDesc(),
        maintMileage.getMileageDue());
  }
}
