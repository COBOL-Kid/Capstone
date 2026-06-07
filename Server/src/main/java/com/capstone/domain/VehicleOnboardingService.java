package com.capstone.domain;

import com.capstone.data.*;
import com.capstone.domain.VehicleDataMapper.VehicleIdentity;
import com.capstone.integration.OwnerManualResponse;
import com.capstone.integration.RepairCostResponse;
import com.capstone.integration.RepairEstimatesResponse;
import com.capstone.integration.RepairEstimatesVinProbeResult;
import com.capstone.integration.TrimOptionsResponse;
import com.capstone.integration.VehicleDataProviderClient;
import com.capstone.integration.VehicleDataProviderPrefetchMetrics;
import com.capstone.integration.VehicleDataProviderRequestMetrics;
import com.capstone.integration.VehiclePhotosResponse;
import com.capstone.integration.VehicleRecallsResponse;
import com.capstone.integration.VehicleWarrantyResponse;
import com.capstone.integration.VinDecodeResponse;
import com.capstone.logging.LogRedaction;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.VehicleWarranty;
import com.capstone.models.VehicleWarrantyId;
import com.capstone.models.Vin;
import com.capstone.models.dto.AddVinOutcome;
import com.capstone.models.dto.AddVinRequest;
import com.capstone.models.dto.AddVinResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class VehicleOnboardingService {

  private static final Logger log = LoggerFactory.getLogger(VehicleOnboardingService.class);

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
  private final VehicleDataProviderRequestMetrics vehicleDataProviderRequestMetrics;
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
      VehicleDataProviderRequestMetrics vehicleDataProviderRequestMetrics,
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
    this.vehicleDataProviderRequestMetrics = vehicleDataProviderRequestMetrics;
    this.transactionTemplate = transactionTemplate;
    this.vehicleDataExecutor = vehicleDataExecutor;
  }

  public AddVinOutcome addVinToUser(User user, AddVinRequest request) {
    if (user == null || user.getUserId() == null) {
      throw new IllegalArgumentException("Authenticated user is required");
    }
    if (!user.isEmailVerified()) {
      throw new EmailNotVerifiedException();
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
      return new AddVinOutcome.Completed(linkExistingVin(user, existingVin, currentMileage));
    }

    VinDecodeResponse vinDecodeResponse = vehicleDataProviderClient.decodeVin(normalizedVin);
    if (Boolean.FALSE.equals(vinDecodeResponse.vinValid())) {
      throw new VinNotFoundException();
    }
    VehicleIdentity identity = vehicleDataMapper.toVehicleIdentity(vinDecodeResponse);
    String selectedTrim = request.hasSelectedTrim() ? request.selectedTrim().trim() : null;
    VehicleIdentity lookupIdentity =
        selectedTrim != null
            ? new VehicleIdentity(
                identity.year(), identity.make(), identity.model(), selectedTrim, identity.style())
            : identity;

    VehicleType existingVehicleType =
        vehicleTypeRepository
            .findByIdentity(
                lookupIdentity.year(),
                lookupIdentity.make(),
                lookupIdentity.model(),
                lookupIdentity.trim(),
                lookupIdentity.style())
            .orElse(null);

    if (existingVehicleType != null) {
      ensureWarrantyForYearMakeModel(
          existingVehicleType.getVehicleYear(),
          existingVehicleType.getVehicleMake(),
          existingVehicleType.getVehicleModel());
      return new AddVinOutcome.Completed(
          saveVinAndAssociation(user, normalizedVin, currentMileage, existingVehicleType));
    }

    NewVehicleTypeFetchResult fetchResult =
        fetchNewVehicleTypeData(normalizedVin, identity, selectedTrim != null, selectedTrim);
    if (fetchResult instanceof NewVehicleTypeFetchResult.TrimSelectionRequired required) {
      return new AddVinOutcome.TrimSelectionRequired(
          required.year(), required.make(), required.model());
    }

    NewVehicleTypePrefetch prefetch = ((NewVehicleTypeFetchResult.Ready) fetchResult).prefetch();
    String trimOverride = ((NewVehicleTypeFetchResult.Ready) fetchResult).trimOverride();
    VehicleType vehicleType =
        vehicleDataMapper.toVehicleType(
            vinDecodeResponse, prefetch.supplemental().ownerManual(), trimOverride);
    return new AddVinOutcome.Completed(
        saveFullVehicleData(
            user,
            normalizedVin,
            currentMileage,
            vehicleType,
            prefetch.supplemental().repairEstimates(),
            prefetch.supplemental().repairCosts(),
            prefetch.supplemental().recalls(),
            prefetch.supplemental().vehicleWarranty(),
            prefetch.photos()));
  }

  public List<String> getTrimOptions(String year, String make, String model) {
    TrimOptionsResponse response = vehicleDataProviderClient.getTrimOptions(year, make, model);
    if (response == null || response.data() == null || response.data().trims() == null) {
      return List.of();
    }
    return response.data().trims().stream()
        .filter(trim -> trim != null && !trim.isBlank())
        .toList();
  }

  protected AddVinResponse linkExistingVin(User user, Vin vin, int currentMileage) {
    VehicleType vehicleType = vin.getVehicleType();
    ensureWarrantyForYearMakeModel(
        vehicleType.getVehicleYear(), vehicleType.getVehicleMake(), vehicleType.getVehicleModel());
    List<String> photosForNewAssociation =
        userVinRepository.findByUserUserIdAndVinVin(user.getUserId(), vin.getVin()).isEmpty()
            ? fetchVehiclePhotos(vin.getVin())
            : null;
    return transactionTemplate.execute(
        _ -> {
          UserVin userVin =
              userVinRepository
                  .findByUserUserIdAndVinVin(user.getUserId(), vin.getVin())
                  .orElse(null);
          boolean createdAssociation = false;
          if (userVin == null) {
            List<String> photos = photosForNewAssociation;
            if (photos == null) {
              photos = fetchVehiclePhotos(vin.getVin());
            }
            userVin = userVinRepository.save(newUserVin(user, vin, currentMileage, photos));
            createdAssociation = true;
          }
          return response(vin, userVin, false, false, createdAssociation);
        });
  }

  protected AddVinResponse saveVinAndAssociation(
      User user, String vinNumber, int currentMileage, VehicleType vehicleType) {
    List<String> photosForNewAssociation =
        userVinRepository.findByUserUserIdAndVinVin(user.getUserId(), vinNumber).isEmpty()
            ? fetchVehiclePhotos(vinNumber)
            : null;
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
            List<String> photos = photosForNewAssociation;
            if (photos == null) {
              photos = fetchVehiclePhotos(vinNumber);
            }
            userVin = userVinRepository.save(newUserVin(user, vin, currentMileage, photos));
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

  private UserVin newUserVin(User user, Vin vin, int currentMileage, List<String> preloadedPhotos) {
    UserVin userVin = new UserVin(user, vin, currentMileage);
    List<String> availableImageUrls = preloadedPhotos != null ? preloadedPhotos : List.of();
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

  private NewVehicleTypeFetchResult fetchNewVehicleTypeData(
      String normalizedVin, VehicleIdentity identity, boolean useFallback, String selectedTrim) {
    VehicleDataProviderPrefetchMetrics prefetchMetrics = new VehicleDataProviderPrefetchMetrics();
    ScopedValue.Carrier prefetchScope =
        vehicleDataProviderRequestMetrics.bindPrefetchMetrics(prefetchMetrics);
    long prefetchStartNanos = System.nanoTime();

    Future<List<String>> photos =
        vehicleDataExecutor.submit(
            () -> prefetchScope.call(() -> fetchVehiclePhotos(normalizedVin)));

    if (useFallback) {
      NewVehicleTypePrefetch prefetch =
          fetchFallbackVehicleData(identity, selectedTrim, photos, prefetchScope);
      logPrefetchComplete(normalizedVin, prefetchStartNanos, prefetchMetrics, prefetch, true);
      return new NewVehicleTypeFetchResult.Ready(prefetch, selectedTrim);
    }

    RepairEstimatesVinProbeResult probe =
        prefetchScope.call(
            () -> vehicleDataProviderClient.probeRepairEstimatesByVin(normalizedVin));
    if (probe instanceof RepairEstimatesVinProbeResult.TrimSelectionRequired) {
      photos.cancel(true);
      return new NewVehicleTypeFetchResult.TrimSelectionRequired(
          identity.year(), identity.make(), identity.model());
    }

    RepairEstimatesResponse repairEstimatesResponse =
        probe instanceof RepairEstimatesVinProbeResult.Found found ? found.response() : null;

    Future<OwnerManualResponse> ownerManual =
        vehicleDataExecutor.submit(
            () ->
                prefetchScope.call(() -> vehicleDataProviderClient.getOwnerManual(normalizedVin)));
    Future<RepairCostResponse> repairCosts =
        vehicleDataExecutor.submit(
            () ->
                prefetchScope.call(() -> vehicleDataProviderClient.getRepairCosts(normalizedVin)));
    Future<VehicleRecallsResponse> recalls =
        vehicleDataExecutor.submit(
            () -> prefetchScope.call(() -> vehicleDataProviderClient.getRecalls(normalizedVin)));
    Future<VehicleWarrantyResponse> vehicleWarranty =
        vehicleDataExecutor.submit(
            () ->
                prefetchScope.call(
                    () ->
                        vehicleDataProviderClient.getVehicleWarranty(
                            identity.year(), identity.make(), identity.model())));

    OwnerManualResponse ownerManualResponse = await(ownerManual);
    RepairCostResponse repairCostResponse = await(repairCosts);
    VehicleRecallsResponse recallsResponse = await(recalls);
    VehicleWarrantyResponse warrantyResponse = await(vehicleWarranty);
    List<String> photoUrls = await(photos);

    SupplementalVehicleData supplemental =
        new SupplementalVehicleData(
            ownerManualResponse,
            repairEstimatesResponse,
            repairCostResponse,
            recallsResponse,
            warrantyResponse);
    NewVehicleTypePrefetch prefetch = new NewVehicleTypePrefetch(supplemental, photoUrls);
    logPrefetchComplete(normalizedVin, prefetchStartNanos, prefetchMetrics, prefetch, false);
    return new NewVehicleTypeFetchResult.Ready(prefetch, null);
  }

  private NewVehicleTypePrefetch fetchFallbackVehicleData(
      VehicleIdentity identity,
      String selectedTrim,
      Future<List<String>> photos,
      ScopedValue.Carrier prefetchScope) {
    String year = identity.year();
    String make = identity.make();
    String model = identity.model();

    Future<RepairEstimatesResponse> repairEstimates =
        vehicleDataExecutor.submit(
            () ->
                prefetchScope.call(
                    () ->
                        vehicleDataProviderClient.getRepairEstimates(
                            year, make, model, selectedTrim)));
    Future<OwnerManualResponse> ownerManual =
        vehicleDataExecutor.submit(
            () ->
                prefetchScope.call(
                    () -> vehicleDataProviderClient.getOwnerManual(year, make, model)));
    Future<RepairCostResponse> repairCosts =
        vehicleDataExecutor.submit(
            () ->
                prefetchScope.call(
                    () -> vehicleDataProviderClient.getRepairCosts(year, make, model)));
    Future<VehicleRecallsResponse> recalls =
        vehicleDataExecutor.submit(
            () ->
                prefetchScope.call(() -> vehicleDataProviderClient.getRecalls(year, make, model)));

    RepairEstimatesResponse repairEstimatesResponse = await(repairEstimates);
    OwnerManualResponse ownerManualResponse = await(ownerManual);
    RepairCostResponse repairCostResponse = await(repairCosts);
    VehicleRecallsResponse recallsResponse = await(recalls);
    List<String> photoUrls = await(photos);

    SupplementalVehicleData supplemental =
        new SupplementalVehicleData(
            ownerManualResponse,
            repairEstimatesResponse,
            repairCostResponse,
            recallsResponse,
            null);
    return new NewVehicleTypePrefetch(supplemental, photoUrls);
  }

  private void logPrefetchComplete(
      String normalizedVin,
      long prefetchStartNanos,
      VehicleDataProviderPrefetchMetrics prefetchMetrics,
      NewVehicleTypePrefetch prefetch,
      boolean fallback) {
    long prefetchDurationMs = (System.nanoTime() - prefetchStartNanos) / 1_000_000L;
    SupplementalVehicleData supplemental = prefetch.supplemental();
    log.info(
        "Vehicle data prefetch completed vin={} fallback={} durationMs={} peakConcurrentRequests={} "
            + "ownerManual={} repairEstimates={} repairCosts={} recalls={} warranty={} photos={}",
        LogRedaction.maskVin(normalizedVin),
        fallback,
        prefetchDurationMs,
        prefetchMetrics.maxInFlight(),
        supplemental.ownerManual() != null,
        supplemental.repairEstimates() != null,
        supplemental.repairCosts() != null,
        supplemental.recalls() != null,
        supplemental.vehicleWarranty() != null,
        prefetch.photos() != null && !prefetch.photos().isEmpty());
  }

  private <T> T await(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while fetching vehicle data", ex);
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof VinNotFoundException vinNotFoundException) {
        throw vinNotFoundException;
      }
      if (cause instanceof HttpStatusCodeException httpException
          && httpException.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        throw new VinNotFoundException();
      }
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException("Failed to fetch vehicle data", cause);
    }
  }

  private sealed interface NewVehicleTypeFetchResult {
    record Ready(NewVehicleTypePrefetch prefetch, String trimOverride)
        implements NewVehicleTypeFetchResult {}

    record TrimSelectionRequired(String year, String make, String model)
        implements NewVehicleTypeFetchResult {}
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
