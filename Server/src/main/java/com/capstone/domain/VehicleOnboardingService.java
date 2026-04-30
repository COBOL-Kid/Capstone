package com.capstone.domain;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.capstone.data.MaintCostRepositoryJPA;
import com.capstone.data.MaintMileageRepositoryJPA;
import com.capstone.data.RecallRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VehicleTypeRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.domain.VehicleDataMapper.VehicleIdentity;
import com.capstone.domain.dto.AddVinRequest;
import com.capstone.domain.dto.AddVinResponse;
import com.capstone.integration.MaintenanceScheduleResponse;
import com.capstone.integration.OwnerManualResponse;
import com.capstone.integration.RecallResponse;
import com.capstone.integration.RepairCostResponse;
import com.capstone.integration.VehicleDataProviderClient;
import com.capstone.integration.VehiclePhotosResponse;
import com.capstone.integration.VinDecodeResponse;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;

@Service
public class VehicleOnboardingService {

	private final VinRepositoryJPA vinRepository;
	private final VehicleTypeRepositoryJPA vehicleTypeRepository;
	private final UserVinRepositoryJPA userVinRepository;
	private final RecallRepositoryJPA recallRepository;
	private final MaintMileageRepositoryJPA maintMileageRepository;
	private final MaintCostRepositoryJPA maintCostRepository;
	private final VehicleDataProviderClient vehicleDataProviderClient;
	private final VehicleDataMapper vehicleDataMapper;
	private final TransactionTemplate transactionTemplate;

	public VehicleOnboardingService(VinRepositoryJPA vinRepository, VehicleTypeRepositoryJPA vehicleTypeRepository,
			UserVinRepositoryJPA userVinRepository, RecallRepositoryJPA recallRepository,
			MaintMileageRepositoryJPA maintMileageRepository, MaintCostRepositoryJPA maintCostRepository,
			VehicleDataProviderClient vehicleDataProviderClient, VehicleDataMapper vehicleDataMapper,
			TransactionTemplate transactionTemplate) {
		this.vinRepository = vinRepository;
		this.vehicleTypeRepository = vehicleTypeRepository;
		this.userVinRepository = userVinRepository;
		this.recallRepository = recallRepository;
		this.maintMileageRepository = maintMileageRepository;
		this.maintCostRepository = maintCostRepository;
		this.vehicleDataProviderClient = vehicleDataProviderClient;
		this.vehicleDataMapper = vehicleDataMapper;
		this.transactionTemplate = transactionTemplate;
	}

	public AddVinResponse addVinToUser(User user, AddVinRequest request) {
		if (user == null || user.getUserId() == null) {
			throw new IllegalArgumentException("Authenticated user is required");
		}
		String normalizedVin = normalizeVin(request != null ? request.vin() : null);
		Integer currentMileage = request != null ? request.currentMileage() : null;
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
		VehicleIdentity identity = vehicleDataMapper.toVehicleIdentity(vinDecodeResponse);
		VehicleType existingVehicleType = vehicleTypeRepository
				.findByIdentity(identity.year(), identity.make(), identity.model(), identity.trim(), identity.style())
				.orElse(null);

		if (existingVehicleType != null) {
			return saveVinAndAssociation(user, normalizedVin, currentMileage, existingVehicleType, false);
		}

		SupplementalVehicleData supplementalVehicleData = fetchSupplementalVehicleData(normalizedVin);
		VehicleType vehicleType = vehicleDataMapper.toVehicleType(vinDecodeResponse,
				supplementalVehicleData.ownerManual());
		return saveFullVehicleData(user, normalizedVin, currentMileage, vehicleType,
				supplementalVehicleData.maintenanceSchedule(), supplementalVehicleData.repairCosts(),
				supplementalVehicleData.recalls());
	}

	protected AddVinResponse linkExistingVin(User user, Vin vin, int currentMileage) {
		return transactionTemplate.execute(status -> {
			UserVin userVin = userVinRepository.findByUserUserIdAndVinVin(user.getUserId(), vin.getVin()).orElse(null);
			boolean createdAssociation = false;
			if (userVin == null) {
				userVin = userVinRepository.save(newUserVin(user, vin, currentMileage));
				createdAssociation = true;
			}
			return response(vin, userVin, false, false, createdAssociation);
		});
	}

	protected AddVinResponse saveVinAndAssociation(User user, String vinNumber, int currentMileage,
			VehicleType vehicleType, boolean createdVehicleType) {
		return transactionTemplate.execute(status -> {
			VehicleType savedVehicleType = vehicleType;
			var existingVin = vinRepository.findById(vinNumber);
			boolean createdVin = existingVin.isEmpty();
			Vin vin = existingVin
					.orElseGet(() -> vinRepository.save(new Vin(vinNumber, currentMileage, savedVehicleType)));
			UserVin userVin = userVinRepository.findByUserUserIdAndVinVin(user.getUserId(), vinNumber).orElse(null);
			boolean createdAssociation = false;
			if (userVin == null) {
				userVin = userVinRepository.save(newUserVin(user, vin, currentMileage));
				createdAssociation = true;
			}
			return response(vin, userVin, createdVin, createdVehicleType, createdAssociation);
		});
	}

	protected AddVinResponse saveFullVehicleData(User user, String vinNumber, int currentMileage,
			VehicleType incomingVehicleType, MaintenanceScheduleResponse maintenanceScheduleResponse,
			RepairCostResponse repairCostResponse, RecallResponse recallResponse) {
		return transactionTemplate.execute(status -> {
			VehicleType vehicleType = vehicleTypeRepository.findByIdentity(incomingVehicleType.getVehicleYear(),
					incomingVehicleType.getVehicleMake(), incomingVehicleType.getVehicleModel(),
					incomingVehicleType.getVehicleTrim(), incomingVehicleType.getVehicleStyle()).orElse(null);
			boolean createdVehicleType = false;
			if (vehicleType == null) {
				vehicleType = vehicleTypeRepository.save(incomingVehicleType);
				maintMileageRepository
						.saveAll(vehicleDataMapper.toMaintMileages(vehicleType, maintenanceScheduleResponse));
				maintCostRepository.saveAll(vehicleDataMapper.toMaintCosts(vehicleType, repairCostResponse));
				recallRepository.saveAll(vehicleDataMapper.toRecalls(vehicleType, recallResponse));
				createdVehicleType = true;
			}
			VehicleType savedVehicleType = vehicleType;
			var existingVin = vinRepository.findById(vinNumber);
			boolean createdVin = existingVin.isEmpty();
			Vin vin = existingVin
					.orElseGet(() -> vinRepository.save(new Vin(vinNumber, currentMileage, savedVehicleType)));
			UserVin userVin = userVinRepository.findByUserUserIdAndVinVin(user.getUserId(), vinNumber).orElse(null);
			boolean createdAssociation = false;
			if (userVin == null) {
				userVin = userVinRepository.save(newUserVin(user, vin, currentMileage));
				createdAssociation = true;
			}
			return response(vin, userVin, createdVin, createdVehicleType, createdAssociation);
		});
	}

	private UserVin newUserVin(User user, Vin vin, int currentMileage) {
		UserVin userVin = new UserVin(user, vin, currentMileage);
		List<String> availableImageUrls = fetchVehiclePhotos(vin.getVin());
		userVin.setAvailableImageUrls(availableImageUrls);
		userVin.setSelectedImageUrl(availableImageUrls.isEmpty() ? null : availableImageUrls.getFirst());
		return userVin;
	}

	private AddVinResponse response(Vin vin, UserVin userVin, boolean createdVin, boolean createdVehicleType,
			boolean createdAssociation) {
		VehicleType vehicleType = vin.getVehicleTypeId();
		return new AddVinResponse(vin.getVin(), userVin.getCurrentMileage(), vehicleType.getVehicleTypeId(),
				vehicleType.getVehicleMake(), vehicleType.getVehicleModel(), vehicleType.getVehicleTrim(),
				vehicleType.getVehicleYear(), userVin.getAvailableImageUrls(), userVin.getSelectedImageUrl(),
				createdVin, createdVehicleType, createdAssociation);
	}

	private List<String> fetchVehiclePhotos(String vinNumber) {
		VehiclePhotosResponse photosResponse = vehicleDataProviderClient.getPhotos(vinNumber);
		return photosResponse.retailPhotos().stream().filter(url -> url != null && !url.isBlank()).distinct().toList();
	}

	private SupplementalVehicleData fetchSupplementalVehicleData(String normalizedVin) {
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Future<OwnerManualResponse> ownerManual = executor
					.submit(() -> vehicleDataProviderClient.getOwnerManual(normalizedVin));
			Future<MaintenanceScheduleResponse> maintenanceSchedule = executor
					.submit(() -> vehicleDataProviderClient.getMaintenanceSchedule(normalizedVin));
			Future<RepairCostResponse> repairCosts = executor
					.submit(() -> vehicleDataProviderClient.getRepairCosts(normalizedVin));
			Future<RecallResponse> recalls = executor.submit(() -> vehicleDataProviderClient.getRecalls(normalizedVin));

			return new SupplementalVehicleData(await(ownerManual), await(maintenanceSchedule), await(repairCosts),
					await(recalls));
		}
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

	private record SupplementalVehicleData(OwnerManualResponse ownerManual,
			MaintenanceScheduleResponse maintenanceSchedule, RepairCostResponse repairCosts, RecallResponse recalls) {
	}
}
