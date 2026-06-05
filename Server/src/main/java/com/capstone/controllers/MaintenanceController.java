package com.capstone.controllers;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.domain.MaintenanceTrackingService;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance")
@Validated
public class MaintenanceController {

  private final MaintenanceTrackingService maintenanceTrackingService;

  public MaintenanceController(MaintenanceTrackingService maintenanceTrackingService) {
    this.maintenanceTrackingService = maintenanceTrackingService;
  }

  @PostMapping("/completed")
  public ResponseEntity<?> completeMaintenance(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CompleteMaintenanceRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(
        maintenanceTrackingService.completeMaintenance(user.userId(), request), HttpStatus.CREATED);
  }

  @DeleteMapping("/completed/{completedMaintenanceId}")
  public ResponseEntity<?> uncompleteMaintenance(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable("completedMaintenanceId") Long completedMaintenanceId) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    maintenanceTrackingService.uncompleteMaintenance(user.userId(), completedMaintenanceId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
