package com.capstone.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.domain.VinService;
import com.capstone.domain.VehicleOnboardingService;
import com.capstone.domain.dto.AddVinRequest;
import com.capstone.domain.dto.AddVinResponse;
import com.capstone.models.User;
import com.capstone.models.Vin;

@RestController
@RequestMapping("/api/vin")
public class VinController {

    VinService vinService;
    VehicleOnboardingService vehicleOnboardingService;

    public VinController(VinService vinService, VehicleOnboardingService vehicleOnboardingService) {
        this.vinService = vinService;
        this.vehicleOnboardingService = vehicleOnboardingService;
    }

    @GetMapping
    public ResponseEntity<?> getCurrentUserVins(@AuthenticationPrincipal User user) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        var vinList = vinService.findVinsByUserId(user.getUserId());
        if (vinList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(vinList, HttpStatus.OK);
    }

    @GetMapping("/{vin}")
    public ResponseEntity<?> getVin(@PathVariable String vin) {
        return vinService.findByVin(vin)
                .<ResponseEntity<?>>map(foundVin -> new ResponseEntity<Vin>(foundVin, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<?> addVin(@AuthenticationPrincipal User user, @RequestBody AddVinRequest request) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        AddVinResponse response = vehicleOnboardingService.addVinToUser(user, request);
        return new ResponseEntity<>(response, response.createdAssociation() ? HttpStatus.CREATED : HttpStatus.OK);
    }
}
