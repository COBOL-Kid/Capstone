package com.capstone.controllers;

import static com.capstone.models.dto.VinValidation.VIN_MESSAGE;
import static com.capstone.models.dto.VinValidation.VIN_PATTERN;

import com.capstone.domain.VehicleOnboardingService;
import com.capstone.domain.VinService;
import com.capstone.models.User;
import com.capstone.models.dto.AddVinRequest;
import com.capstone.models.dto.AddVinResponse;
import com.capstone.models.dto.UpdateMileageRequest;
import com.capstone.models.dto.UpdateVehiclePhotoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vin")
@Validated
public class VinController {

  final VinService vinService;
  final VehicleOnboardingService vehicleOnboardingService;

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
  public ResponseEntity<?> getVin(
      @AuthenticationPrincipal User user,
      @PathVariable("vin")
          @Pattern(
              regexp = VIN_PATTERN,
              flags = Pattern.Flag.CASE_INSENSITIVE,
              message = VIN_MESSAGE)
          String vin) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return vinService
        .findVehicleDetailForUser(user.getUserId(), vin)
        .<ResponseEntity<?>>map(detail -> new ResponseEntity<>(detail, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PatchMapping("/{vin}/mileage")
  public ResponseEntity<?> updateMileage(
      @AuthenticationPrincipal User user,
      @PathVariable("vin")
          @Pattern(
              regexp = VIN_PATTERN,
              flags = Pattern.Flag.CASE_INSENSITIVE,
              message = VIN_MESSAGE)
          String vin,
      @Valid @RequestBody UpdateMileageRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return vinService
        .updateMileage(user.getUserId(), vin, request)
        .<ResponseEntity<?>>map(detail -> new ResponseEntity<>(detail, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  public ResponseEntity<?> addVin(
      @AuthenticationPrincipal User user, @Valid @RequestBody AddVinRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    AddVinResponse response = vehicleOnboardingService.addVinToUser(user, request);
    return new ResponseEntity<>(
        response, response.createdAssociation() ? HttpStatus.CREATED : HttpStatus.OK);
  }

  @PatchMapping("/{vin}/photo")
  public ResponseEntity<?> updateSelectedPhoto(
      @AuthenticationPrincipal User user,
      @PathVariable("vin")
          @Pattern(
              regexp = VIN_PATTERN,
              flags = Pattern.Flag.CASE_INSENSITIVE,
              message = VIN_MESSAGE)
          String vin,
      @Valid @RequestBody UpdateVehiclePhotoRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return vinService
        .updateSelectedImage(user.getUserId(), vin, request.selectedImageUrl())
        .<ResponseEntity<?>>map(response -> new ResponseEntity<>(response, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @DeleteMapping("/{vin}")
  public ResponseEntity<?> deleteVin(
      @AuthenticationPrincipal User user,
      @PathVariable("vin")
          @Pattern(
              regexp = VIN_PATTERN,
              flags = Pattern.Flag.CASE_INSENSITIVE,
              message = VIN_MESSAGE)
          String vin) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    boolean deleted = vinService.deleteVin(user.getUserId(), vin);
    if (deleted) {
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
