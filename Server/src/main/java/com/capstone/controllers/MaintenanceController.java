package com.capstone.controllers;

import com.capstone.domain.MaintenanceTrackingService;
import com.capstone.models.User;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import com.capstone.models.dto.CompletedMaintenanceResponse;
import com.capstone.models.dto.MaintenanceCostResponse;
import com.capstone.models.dto.UpcomingMaintenanceResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.capstone.models.dto.VinValidation.VIN_MESSAGE;
import static com.capstone.models.dto.VinValidation.VIN_PATTERN;

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
                                                     @PathVariable("vin") @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE) String vin) {
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
                                                    @PathVariable("vin") @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE) String vin) {
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

    @GetMapping("/{vin}/costs")
    public ResponseEntity<?> getMaintenanceCosts(@AuthenticationPrincipal User user,
                                                 @PathVariable("vin") @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE) String vin) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<MaintenanceCostResponse> costs = maintenanceTrackingService.findMaintenanceCosts(user, vin);
        if (costs.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(costs, HttpStatus.OK);
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
