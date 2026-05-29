package com.capstone.controllers;

import static com.capstone.models.dto.VinValidation.VIN_MESSAGE;
import static com.capstone.models.dto.VinValidation.VIN_PATTERN;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.VehicleDashboardService;
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
  final VehicleDashboardService vehicleDashboardService;
  final UserRepositoryJPA userRepository;

  public VinController(
      VinService vinService,
      VehicleOnboardingService vehicleOnboardingService,
      VehicleDashboardService vehicleDashboardService,
      UserRepositoryJPA userRepository) {
    this.vinService = vinService;
    this.vehicleOnboardingService = vehicleOnboardingService;
    this.vehicleDashboardService = vehicleDashboardService;
    this.userRepository = userRepository;
  }

  @GetMapping
  public ResponseEntity<?> getCurrentUserVins(@AuthenticationPrincipal AuthenticatedUser user) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    var vinList = vinService.findVinsByUserId(user.userId());
    if (vinList.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    return new ResponseEntity<>(vinList, HttpStatus.OK);
  }

  @GetMapping("/{vin}/dashboard")
  public ResponseEntity<?> getVehicleDashboard(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable("vin")
          @Pattern(
              regexp = VIN_PATTERN,
              flags = Pattern.Flag.CASE_INSENSITIVE,
              message = VIN_MESSAGE)
          String vin) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return vehicleDashboardService
        .findDashboardForUser(user.userId(), vin)
        .<ResponseEntity<?>>map(dashboard -> new ResponseEntity<>(dashboard, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @GetMapping("/{vin}")
  public ResponseEntity<?> getVin(
      @AuthenticationPrincipal AuthenticatedUser user,
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
        .findVehicleDetailForUser(user.userId(), vin)
        .<ResponseEntity<?>>map(detail -> new ResponseEntity<>(detail, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PatchMapping("/{vin}/mileage")
  public ResponseEntity<?> updateMileage(
      @AuthenticationPrincipal AuthenticatedUser user,
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
        .updateMileage(user.userId(), vin, request)
        .<ResponseEntity<?>>map(detail -> new ResponseEntity<>(detail, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  public ResponseEntity<?> addVin(
      @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody AddVinRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    User entity =
        userRepository
            .findById(user.userId())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    AddVinResponse response = vehicleOnboardingService.addVinToUser(entity, request);
    return new ResponseEntity<>(
        response, response.createdAssociation() ? HttpStatus.CREATED : HttpStatus.OK);
  }

  @PatchMapping("/{vin}/photo")
  public ResponseEntity<?> updateSelectedPhoto(
      @AuthenticationPrincipal AuthenticatedUser user,
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
        .updateSelectedImage(user.userId(), vin, request.selectedImageUrl())
        .<ResponseEntity<?>>map(response -> new ResponseEntity<>(response, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @DeleteMapping("/{vin}")
  public ResponseEntity<?> deleteVin(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable("vin")
          @Pattern(
              regexp = VIN_PATTERN,
              flags = Pattern.Flag.CASE_INSENSITIVE,
              message = VIN_MESSAGE)
          String vin) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    boolean deleted = vinService.deleteVin(user.userId(), vin);
    if (deleted) {
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
