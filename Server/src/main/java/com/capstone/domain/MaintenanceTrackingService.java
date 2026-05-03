package com.capstone.domain;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.MaintMileageRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import com.capstone.models.dto.CompletedMaintenanceResponse;
import com.capstone.models.dto.UpcomingMaintenanceResponse;
import com.capstone.models.CompletedMaintenance;
import com.capstone.models.MaintMileage;
import com.capstone.models.User;
import com.capstone.models.UserVin;

@Service
public class MaintenanceTrackingService {

	private final CompletedMaintenanceRepositoryJPA completedMaintenanceRepository;
	private final MaintMileageRepositoryJPA maintMileageRepository;
	private final UserVinRepositoryJPA userVinRepository;

	public MaintenanceTrackingService(CompletedMaintenanceRepositoryJPA completedMaintenanceRepository,
			MaintMileageRepositoryJPA maintMileageRepository, UserVinRepositoryJPA userVinRepository) {
		this.completedMaintenanceRepository = completedMaintenanceRepository;
		this.maintMileageRepository = maintMileageRepository;
		this.userVinRepository = userVinRepository;
	}

	@Transactional(readOnly = true)
	public List<CompletedMaintenanceResponse> findCompletedMaintenance(User user, String vin) {
		return completedMaintenanceRepository.findAllForUserVin(user.getUserId(), normalizeVin(vin)).stream()
				.map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public List<UpcomingMaintenanceResponse> findUpcomingMaintenance(User user, String vin) {
		String normalizedVin = normalizeVin(vin);
		UserVin userVin = userVinRepository.findForUserVin(user.getUserId(), normalizedVin)
				.orElseThrow(() -> new IllegalArgumentException("VIN is not associated with this user"));

		int threshold = userVin.getCurrentMileage() + 10000;
		Long vehicleTypeId = userVin.getVin().getVehicleTypeId().getVehicleTypeId();

		return maintMileageRepository.findUpcomingAndPastDue(vehicleTypeId, threshold, user.getUserId(), normalizedVin)
				.stream()
				.map(m -> new UpcomingMaintenanceResponse(
						m.getMaintMileageId(),
						normalizedVin,
						m.getMileageDue(),
						m.getMaintDesc()
				)).toList();
	}

	@Transactional
	public CompletedMaintenanceResponse completeMaintenance(User user, CompleteMaintenanceRequest request) {
		if (request == null || request.maintMileageId() == null) {
			throw new IllegalArgumentException("Maintenance item is required");
		}
		if (request.mileageCompleted() == null) {
			throw new IllegalArgumentException("Mileage completed is required");
		}
		if (request.mileageCompleted() < 0) {
			throw new IllegalArgumentException("Mileage completed cannot be negative");
		}
		UserVin userVin = userVinRepository.findByUserUserIdAndVinVin(user.getUserId(), normalizeVin(request.vin()))
				.orElseThrow(() -> new IllegalArgumentException("VIN is not associated with this user"));
		MaintMileage maintMileage = maintMileageRepository.findById(request.maintMileageId())
				.orElseThrow(() -> new IllegalArgumentException("Maintenance item not found"));
		CompletedMaintenance completedMaintenance = completedMaintenanceRepository
				.findByUserVinAndMaintMileage(userVin, maintMileage)
				.orElseGet(() -> saveCompletedMaintenance(userVin, maintMileage, request));
		return toResponse(completedMaintenance);
	}

	private CompletedMaintenance saveCompletedMaintenance(UserVin userVin, MaintMileage maintMileage,
			CompleteMaintenanceRequest request) {
		CompletedMaintenance completedMaintenance = new CompletedMaintenance(userVin, maintMileage,
				request.completedDate() != null ? request.completedDate() : LocalDate.now(),
				request.mileageCompleted());
		completedMaintenance.setCost(request.cost());
		completedMaintenance.setNotes(request.notes());
		return completedMaintenanceRepository.save(completedMaintenance);
	}

	private CompletedMaintenanceResponse toResponse(CompletedMaintenance completedMaintenance) {
		return new CompletedMaintenanceResponse(completedMaintenance.getCompletedMaintenanceId(),
				completedMaintenance.getUserVin().getVin().getVin(),
				completedMaintenance.getMaintMileage().getMaintMileageId(), completedMaintenance.getCompletedDate(),
				completedMaintenance.getMileageCompleted(), completedMaintenance.getCost(),
				completedMaintenance.getNotes());
	}

	private String normalizeVin(String vin) {
		if (vin == null || vin.isBlank()) {
			throw new IllegalArgumentException("VIN is required");
		}
		return vin.trim().toUpperCase();
	}
}
