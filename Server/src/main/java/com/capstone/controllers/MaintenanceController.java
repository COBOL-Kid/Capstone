package com.capstone.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.domain.MaintenanceTrackingService;
import com.capstone.domain.dto.CompleteMaintenanceRequest;
import com.capstone.domain.dto.CompletedMaintenanceResponse;
import com.capstone.models.User;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceTrackingService maintenanceTrackingService;

    public MaintenanceController(MaintenanceTrackingService maintenanceTrackingService) {
        this.maintenanceTrackingService = maintenanceTrackingService;
    }

    @GetMapping("/{vin}/completed")
    public ResponseEntity<?> getCompletedMaintenance(@AuthenticationPrincipal User user, @PathVariable String vin) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<CompletedMaintenanceResponse> completedMaintenance = maintenanceTrackingService.findCompletedMaintenance(
                user,
                vin);
        if (completedMaintenance.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(completedMaintenance, HttpStatus.OK);
    }

    @PostMapping("/completed")
    public ResponseEntity<?> completeMaintenance(@AuthenticationPrincipal User user,
            @RequestBody CompleteMaintenanceRequest request) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(maintenanceTrackingService.completeMaintenance(user, request), HttpStatus.CREATED);
    }
}