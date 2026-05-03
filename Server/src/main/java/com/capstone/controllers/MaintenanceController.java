package com.capstone.controllers;

import static com.capstone.models.dto.VinValidation.VIN_MESSAGE;
import static com.capstone.models.dto.VinValidation.VIN_PATTERN;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.domain.MaintenanceTrackingService;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import com.capstone.models.dto.CompletedMaintenanceResponse;
import com.capstone.models.dto.UpcomingMaintenanceResponse;
import com.capstone.models.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/maintenance")
@Validated
public class MaintenanceController {

	private final MaintenanceTrackingService maintenanceTrackingService;

	public MaintenanceController(MaintenanceTrackingService maintenanceTrackingService) {
		this.maintenanceTrackingService = maintenanceTrackingService;
	}

	@GetMapping("/{vin}/completed")
	public ResponseEntity<?> getCompletedMaintenance(@AuthenticationPrincipal User user,
			@PathVariable @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE) String vin) {
		if (user == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		List<CompletedMaintenanceResponse> completedMaintenance = maintenanceTrackingService
				.findCompletedMaintenance(user, vin);
		if (completedMaintenance.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(completedMaintenance, HttpStatus.OK);
	}

	@GetMapping("/{vin}/upcoming")
	public ResponseEntity<?> getUpcomingMaintenance(@AuthenticationPrincipal User user,
			@PathVariable @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE) String vin) {
		if (user == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		List<UpcomingMaintenanceResponse> upcomingMaintenance = maintenanceTrackingService
				.findUpcomingMaintenance(user, vin);
		if (upcomingMaintenance.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(upcomingMaintenance, HttpStatus.OK);
	}

	@PostMapping("/completed")
	public ResponseEntity<?> completeMaintenance(@AuthenticationPrincipal User user,
			@Valid @RequestBody CompleteMaintenanceRequest request) {
		if (user == null) {
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
		}
		return new ResponseEntity<>(maintenanceTrackingService.completeMaintenance(user, request), HttpStatus.CREATED);
	}
}
