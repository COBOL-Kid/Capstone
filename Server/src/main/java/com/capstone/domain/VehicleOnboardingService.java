package com.capstone.domain;

import com.capstone.data.*;
import com.capstone.domain.VehicleDataMapper.VehicleIdentity;
import com.capstone.integration.*;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.VehicleWarranty;
import com.capstone.models.VehicleWarrantyId;
import com.capstone.models.Vin;
import com.capstone.models.dto.AddVinRequest;
import com.capstone.models.dto.AddVinResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class VehicleOnboardingService {

  private final VinRepositoryJPA vinRepository;
  private final VehicleTypeRepositoryJPA vehicleTypeRepository;
  private final UserVinRepositoryJPA userVinRepository;
  private final RecallRepositoryJPA recallRepository;
  private final MaintMileageRepositoryJPA maintMileageRepository;
  private final MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository;
  private final MiscMaintCostRepositoryJPA miscMaintCostRepository;
  private final VehicleWarrantyRepositoryJPA vehicleWarrantyRepository;
  private final VehicleDataProviderClient vehicleDataProviderClient;
  private final VehicleDataMapper vehicleDataMapper;
  private final TransactionTemplate transactionTemplate;
  private final ExecutorService vehicleDataExecutor;

  public VehicleOnboardingService(
      VinRepositoryJPA vinRepository,
      VehicleTypeRepositoryJPA vehicleTypeRepository,
      UserVinRepositoryJPA userVinRepository,
      RecallRepositoryJPA recallRepository,
      MaintMileageRepositoryJPA maintMileageRepository,
      MaintMileageSummaryRepositoryJPA maintMileageSummaryRepository,
      MiscMaintCostRepositoryJPA miscMaintCostRepository,
      VehicleWarrantyRepositoryJPA vehicleWarrantyRepository,
      VehicleDataProviderClient vehicleDataProviderClient,
      VehicleDataMapper vehicleDataMapper,
      TransactionTemplate transactionTemplate,
      @Qualifier("vehicleDataExecutor") ExecutorService vehicleDataExecutor) {
    this.vinRepository = vinRepository;
    this.vehicleTypeRepository = vehicleTypeRepository;
    this.userVinRepository = userVinRepository;
    this.recallRepository = recallRepository;
    this.maintMileageRepository = maintMileageRepository;
    this.maintMileageSummaryRepository = maintMileageSummaryRepository;
    this.miscMaintCostRepository = miscMaintCostRepository;
    this.vehicleWarrantyRepository = vehicleWarrantyRepository;
    this.vehicleDataProviderClient = vehicleDataProviderClient;
    this.vehicleDataMapper = vehicleDataMapper;
    this.transactionTemplate = transactionTemplate;
    this.vehicleDataExecutor = vehicleDataExecutor;
  }

  public AddVinResponse addVinToUser(User user, AddVinRequest request) {
    if (user == null || user.getUserId() == null) {
      throw new IllegalArgumentException("Authenticated user is required");
    }
    if (request == null) {
      throw new IllegalArgumentException("Request is required");
    }
    String normalizedVin = VinNormalizer.normalize(request.vin());
    Integer currentMileage = request.currentMileage();
    if (currentMileage == null) {
      throw new IllegalArgumentException("Current mileage is required");
    }
    if (currentMileage < 0) {
      throw new IllegalArgumentException("Current mileage cannot be negative");
    }

    Vin existingVin = vinRepository.findById(normalizedVin).orElse(null);
    if (existingVin != null) {
      return linkExistingVin(user, existingVin, currentMileage);
    }

    VinDecodeResponse vinDecodeResponse = vehicleDataProviderClient.decodeVin(normalizedVin);
    if (Boolean.FALSE.equals(vinDecodeResponse.vinValid())) {
      throw new VinNotFoundException();
    }
    VehicleIdentity identity = vehicleDataMapper.toVehicleIdentity(vinDecodeResponse);
    VehicleType existingVehicleType =
        vehicleTypeRepository
            .findByIdentity(
                identity.year(),
                identity.make(),
                identity.model(),
                identity.trim(),
                identity.style())
            .orElse(null);

    if (existingVehicleType != null) {
      ensureWarrantyForYearMakeModel(
          existingVehicleType.getVehicleYear(),
          existingVehicleType.getVehicleMake(),
          existingVehicleType.getVehicleModel());
      return saveVinAndAssociation(user, normalizedVin, currentMileage, existingVehicleType);
    }

    NewVehicleTypePrefetch prefetch = fetchNewVehicleTypeData(normalizedVin, identity);
    VehicleType vehicleType =
        vehicleDataMapper.toVehicleType(vinDecodeResponse, prefetch.supplemental().ownerManual());
    return saveFullVehicleData(
        user,
        normalizedVin,
        currentMileage,
        vehicleType,
        prefetch.supplemental().repairEstimates(),
        prefetch.supplemental().repairCosts(),
        prefetch.supplemental().recalls(),
        prefetch.supplemental().vehicleWarranty(),
        prefetch.photos());
  }

  protected AddVinResponse linkExistingVin(User user, Vin vin, int currentMileage) {
    VehicleType vehicleType = vin.getVehicleType();
    ensureWarrantyForYearMakeModel(
        vehicleType.getVehicleYear(), vehicleType.getVehicleMake(), vehicleType.getVehicleModel());
    return transactionTemplate.execute(
        _ -> {
          UserVin userVin =
              userVinRepository
                  .findByUserUserIdAndVinVin(user.getUserId(), vin.getVin())
                  .orElse(null);
          boolean createdAssociation = false;
          if (userVin == null) {
            userVin = userVinRepository.save(newUserVin(user, vin, currentMileage));
            createdAssociation = true;
          }
          return response(vin, userVin, false, false, createdAssociation);
        });
  }

  protected AddVinResponse saveVinAndAssociation(
      User user, String vinNumber, int currentMileage, VehicleType vehicleType) {
    return transactionTemplate.execute(
        _ -> {
          var existingVin = vinRepository.findById(vinNumber);
          boolean createdVin = existingVin.isEmpty();
          Vin vin =
              existingVin.orElseGet(() -> vinRepository.save(new Vin(vinNumber, vehicleType)));
          UserVin userVin =
              userVinRepository.findByUserUserIdAndVinVin(user.getUserId(), vinNumber).orElse(null);
          boolean createdAssociation = false;
          if (userVin == null) {
            userVin = userVinRepository.save(newUserVin(user, vin, currentMileage));
            createdAssociation = true;
          }
          return response(vin, userVin, createdVin, false, createdAssociation);
        });
  }

  protected AddVinResponse saveFullVehicleData(
      User user,
      String vinNumber,
      int currentMileage,
      VehicleType incomingVehicleType,
      RepairEstimatesResponse repairEstimatesResponse,
      RepairCostResponse repairCostResponse,
      VehicleRecallsResponse recallResponse,
      VehicleWarrantyResponse vehicleWarrantyResponse,
      List<String> preloadedPhotos) {
    return transactionTemplate.execute(
        _ -> {
          VehicleType vehicleType =
              vehicleTypeRepository
                  .findByIdentity(
                      incomingVehicleType.getVehicleYear(),
                      incomingVehicleType.getVehicleMake(),
                      incomingVehicleType.getVehicleModel(),
                      incomingVehicleType.getVehicleTrim(),
                      incomingVehicleType.getVehicleStyle())
                  .orElse(null);
          boolean createdVehicleType = false;
          if (vehicleType == null) {
            vehicleType = vehicleTypeRepository.save(incomingVehicleType);
            MaintenanceScheduleImport scheduleImport =
                vehicleDataMapper.toMaintenanceScheduleImport(vehicleType, repairEstimatesResponse);
            maintMileageSummaryRepository.saveAll(scheduleImport.summaries());
            maintMileageRepository.saveAll(scheduleImport.maintMileages());
            miscMaintCostRepository.saveAll(
                vehicleDataMapper.toMiscMaintCosts(vehicleType, repairCostResponse));
            recallRepository.saveAll(vehicleDataMapper.toRecalls(vehicleType, recallResponse));
            persistWarrantyIfAbsent(vehicleType, vehicleWarrantyResponse);
            createdVehicleType = true;
          }
          VehicleType savedVehicleType = vehicleType;
          var existingVin = vinRepository.findById(vinNumber);
          boolean createdVin = existingVin.isEmpty();
          Vin vin =
              existingVin.orElseGet(() -> vinRepository.save(new Vin(vinNumber, savedVehicleType)));
          UserVin userVin =
              userVinRepository.findByUserUserIdAndVinVin(user.getUserId(), vinNumber).orElse(null);
          boolean createdAssociation = false;
          if (userVin == null) {
            userVin =
                userVinRepository.save(newUserVin(user, vin, currentMileage, preloadedPhotos));
            createdAssociation = true;
          }
          return response(vin, userVin, createdVin, createdVehicleType, createdAssociation);
        });
  }

  protected void ensureWarrantyForYearMakeModel(String year, String make, String model) {
    if (warrantyExists(year, make, model)) {
      return;
    }
    VehicleWarrantyResponse warrantyResponse =
        vehicleDataProviderClient.getVehicleWarranty(year, make, model);
    Optional<VehicleWarranty> mapped =
        vehicleDataMapper.toVehicleWarranty(warrantyResponse, year, make, model);
    if (mapped.isEmpty()) {
      return;
    }
    transactionTemplate.execute(
        _ -> {
          persistWarrantyIfAbsent(year, make, model, mapped.get());
          return null;
        });
  }

  private void persistWarrantyIfAbsent(VehicleType vehicleType, VehicleWarrantyResponse response) {
    String year = vehicleType.getVehicleYear();
    String make = vehicleType.getVehicleMake();
    String model = vehicleType.getVehicleModel();
    persistWarrantyIfAbsent(
        year,
        make,
        model,
        vehicleDataMapper.toVehicleWarranty(response, year, make, model).orElse(null));
  }

  private void persistWarrantyIfAbsent(
      String year, String make, String model, VehicleWarranty vehicleWarranty) {
    if (vehicleWarranty == null || warrantyExists(year, make, model)) {
      return;
    }
    vehicleWarrantyRepository.save(vehicleWarranty);
  }

  private boolean warrantyExists(String year, String make, String model) {
    return vehicleWarrantyRepository.existsById(new VehicleWarrantyId(year, make, model));
  }

  private UserVin newUserVin(User user, Vin vin, int currentMileage) {
    return newUserVin(user, vin, currentMileage, null);
  }

  private UserVin newUserVin(User user, Vin vin, int currentMileage, List<String> preloadedPhotos) {
    UserVin userVin = new UserVin(user, vin, currentMileage);
    List<String> availableImageUrls =
        preloadedPhotos != null ? preloadedPhotos : fetchVehiclePhotos(vin.getVin());
    userVin.setAvailableImageUrls(availableImageUrls);
    userVin.setSelectedImageUrl(
        availableImageUrls.isEmpty() ? null : availableImageUrls.getFirst());
    return userVin;
  }

  private AddVinResponse response(
      Vin vin,
      UserVin userVin,
      boolean createdVin,
      boolean createdVehicleType,
      boolean createdAssociation) {
    VehicleType vehicleType = vin.getVehicleType();
    return new AddVinResponse(
        vin.getVin(),
        userVin.getCurrentMileage(),
        vehicleType.getVehicleTypeId(),
        vehicleType.getVehicleMake(),
        vehicleType.getVehicleModel(),
        vehicleType.getVehicleTrim(),
        vehicleType.getVehicleYear(),
        userVin.getAvailableImageUrls(),
        userVin.getSelectedImageUrl(),
        createdVin,
        createdVehicleType,
        createdAssociation);
  }

  private List<String> fetchVehiclePhotos(String vinNumber) {
    VehiclePhotosResponse photosResponse = vehicleDataProviderClient.getPhotos(vinNumber);
    if (photosResponse == null) {
      return List.of();
    }
    return photosResponse.retailPhotos().stream()
        .filter(url -> url != null && !url.isBlank())
        .distinct()
        .toList();
  }

  private NewVehicleTypePrefetch fetchNewVehicleTypeData(
      String normalizedVin, VehicleIdentity identity) {
    Future<List<String>> photos =
        vehicleDataExecutor.submit(() -> fetchVehiclePhotos(normalizedVin));
    Future<OwnerManualResponse> ownerManual =
        vehicleDataExecutor.submit(() -> vehicleDataProviderClient.getOwnerManual(normalizedVin));
    Future<RepairEstimatesResponse> repairEstimates =
        vehicleDataExecutor.submit(
            () -> vehicleDataProviderClient.getRepairEstimates(normalizedVin));
    Future<RepairCostResponse> repairCosts =
        vehicleDataExecutor.submit(() -> vehicleDataProviderClient.getRepairCosts(normalizedVin));
    Future<VehicleRecallsResponse> recalls =
        vehicleDataExecutor.submit(() -> vehicleDataProviderClient.getRecalls(normalizedVin));
    Future<VehicleWarrantyResponse> vehicleWarranty =
        vehicleDataExecutor.submit(
            () ->
                vehicleDataProviderClient.getVehicleWarranty(
                    identity.year(), identity.make(), identity.model()));

    SupplementalVehicleData supplemental =
        new SupplementalVehicleData(
            await(ownerManual),
            await(repairEstimates),
            await(repairCosts),
            await(recalls),
            await(vehicleWarranty));
    return new NewVehicleTypePrefetch(supplemental, await(photos));
  }

  private <T> T await(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while fetching vehicle data", ex);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException("Failed to fetch vehicle data", cause);
    }
  }

  private record SupplementalVehicleData(
      OwnerManualResponse ownerManual,
      RepairEstimatesResponse repairEstimates,
      RepairCostResponse repairCosts,
      VehicleRecallsResponse recalls,
      VehicleWarrantyResponse vehicleWarranty) {}

  private record NewVehicleTypePrefetch(
      SupplementalVehicleData supplemental, List<String> photos) {}
}
