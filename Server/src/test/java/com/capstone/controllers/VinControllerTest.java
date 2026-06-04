package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.VehicleDashboardService;
import com.capstone.domain.VehicleOnboardingService;
import com.capstone.domain.VinService;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.dto.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class VinControllerTest {

  @Test
  void shouldRequireAuthenticationForCurrentUserVinsAndAddVin() {
    VinController controller =
        controller(
            mock(VinService.class),
            mock(VehicleOnboardingService.class),
            mock(VehicleDashboardService.class));

    assertEquals(HttpStatus.UNAUTHORIZED, controller.getCurrentUserVins(null).getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller
            .addVin(null, new AddVinRequest("JTENU5JR6M5962554", 45000, null))
            .getStatusCode());
  }

  @Test
  void shouldReturnNoContentWhenCurrentUserHasNoVins() {
    VinService vinService = mock(VinService.class);
    VinController controller =
        controller(
            vinService, mock(VehicleOnboardingService.class), mock(VehicleDashboardService.class));
    AuthenticatedUser user = user();

    when(vinService.findVinsByUserId(1L)).thenReturn(List.of());

    assertEquals(HttpStatus.NO_CONTENT, controller.getCurrentUserVins(user).getStatusCode());
  }

  @Test
  void shouldReturnCurrentUserVins() {
    VinService vinService = mock(VinService.class);
    VinController controller =
        controller(
            vinService, mock(VehicleOnboardingService.class), mock(VehicleDashboardService.class));
    AuthenticatedUser user = user();
    UserVehicleResponse vehicle = userVehicleResponse();

    when(vinService.findVinsByUserId(1L)).thenReturn(List.of(vehicle));

    var response = controller.getCurrentUserVins(user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(List.of(vehicle), response.getBody());
  }

  @Test
  void shouldReturnVehicleDashboardForCurrentUser() {
    VehicleDashboardService dashboardService = mock(VehicleDashboardService.class);
    VinController controller =
        controller(mock(VinService.class), mock(VehicleOnboardingService.class), dashboardService);
    AuthenticatedUser user = user();
    VehicleDashboardResponse dashboard =
        new VehicleDashboardResponse(
            vehicleDetailResponse(), List.of(), List.of(), List.of(), List.of(), List.of(), null);

    when(dashboardService.findDashboardForUser(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(dashboard));
    when(dashboardService.findDashboardForUser(1L, "MISSINGVIN1234567"))
        .thenReturn(Optional.empty());

    var foundResponse = controller.getVehicleDashboard(user, "JTENU5JR6M5962554");
    var missingResponse = controller.getVehicleDashboard(user, "MISSINGVIN1234567");

    assertEquals(HttpStatus.OK, foundResponse.getStatusCode());
    assertEquals(dashboard, foundResponse.getBody());
    assertEquals(HttpStatus.NOT_FOUND, missingResponse.getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller.getVehicleDashboard(null, "JTENU5JR6M5962554").getStatusCode());
  }

  @Test
  void shouldReturnVehicleDetailForCurrentUser() {
    VinService vinService = mock(VinService.class);
    VinController controller =
        controller(
            vinService, mock(VehicleOnboardingService.class), mock(VehicleDashboardService.class));
    AuthenticatedUser user = user();
    VehicleDetailResponse detail = vehicleDetailResponse();

    when(vinService.findVehicleDetailForUser(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(detail));
    when(vinService.findVehicleDetailForUser(1L, "MISSINGVIN1234567")).thenReturn(Optional.empty());

    var foundResponse = controller.getVin(user, "JTENU5JR6M5962554");
    var missingResponse = controller.getVin(user, "MISSINGVIN1234567");

    assertEquals(HttpStatus.OK, foundResponse.getStatusCode());
    assertEquals(detail, foundResponse.getBody());
    assertEquals(HttpStatus.NOT_FOUND, missingResponse.getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED, controller.getVin(null, "JTENU5JR6M5962554").getStatusCode());
  }

  @Test
  void shouldUpdateMileageForCurrentUserVin() {
    VinService vinService = mock(VinService.class);
    VinController controller =
        controller(
            vinService, mock(VehicleOnboardingService.class), mock(VehicleDashboardService.class));
    AuthenticatedUser user = user();
    VehicleDetailResponse detail = vehicleDetailResponse();
    UpdateMileageRequest request = new UpdateMileageRequest(52000);

    when(vinService.updateMileage(1L, "JTENU5JR6M5962554", request))
        .thenReturn(Optional.of(detail));
    when(vinService.updateMileage(1L, "MISSINGVIN1234567", request)).thenReturn(Optional.empty());

    var updatedResponse = controller.updateMileage(user, "JTENU5JR6M5962554", request);
    var missingResponse = controller.updateMileage(user, "MISSINGVIN1234567", request);

    assertEquals(HttpStatus.OK, updatedResponse.getStatusCode());
    assertEquals(detail, updatedResponse.getBody());
    assertEquals(HttpStatus.NOT_FOUND, missingResponse.getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller.updateMileage(null, "JTENU5JR6M5962554", request).getStatusCode());
  }

  @Test
  void shouldReturnTrimSelectionRequiredWhenOnboardingNeedsTrim() {
    VehicleOnboardingService onboardingService = mock(VehicleOnboardingService.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    VinController controller =
        new VinController(
            mock(VinService.class),
            onboardingService,
            mock(VehicleDashboardService.class),
            userRepository);
    AuthenticatedUser authUser = user();
    User user = new User();
    user.setUserId(1L);
    AddVinRequest request = new AddVinRequest("JTENU5JR6M5962554", 45000, null);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(onboardingService.addVinToUser(user, request))
        .thenReturn(new AddVinOutcome.TrimSelectionRequired("2021", "Toyota", "4RUNNER"));

    var response = controller.addVin(authUser, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertInstanceOf(AddVinTrimSelectionRequiredResponse.class, response.getBody());
    assertEquals(
        AddVinTrimSelectionRequiredResponse.of("2021", "Toyota", "4RUNNER"), response.getBody());
    verify(onboardingService).addVinToUser(user, request);
  }

  @Test
  void shouldReturnTrimOptionsForAuthenticatedUser() {
    VehicleOnboardingService onboardingService = mock(VehicleOnboardingService.class);
    VinController controller =
        controller(mock(VinService.class), onboardingService, mock(VehicleDashboardService.class));
    AuthenticatedUser user = user();

    when(onboardingService.getTrimOptions("2021", "Toyota", "4RUNNER"))
        .thenReturn(List.of("SRS Prem", "Limited"));

    var response = controller.getTrimOptions(user, "2021", "Toyota", "4RUNNER");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(new TrimOptionsDto(List.of("SRS Prem", "Limited")), response.getBody());
    verify(onboardingService).getTrimOptions("2021", "Toyota", "4RUNNER");
  }

  @Test
  void shouldRequireYearMakeAndModelForTrimOptions() {
    VinController controller =
        controller(
            mock(VinService.class),
            mock(VehicleOnboardingService.class),
            mock(VehicleDashboardService.class));
    AuthenticatedUser user = user();

    assertEquals(
        HttpStatus.BAD_REQUEST,
        controller.getTrimOptions(user, "", "Toyota", "4RUNNER").getStatusCode());
    assertEquals(
        HttpStatus.BAD_REQUEST,
        controller.getTrimOptions(user, "2021", " ", "4RUNNER").getStatusCode());
    assertEquals(
        HttpStatus.BAD_REQUEST,
        controller.getTrimOptions(user, "2021", "Toyota", null).getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller.getTrimOptions(null, "2021", "Toyota", "4RUNNER").getStatusCode());
  }

  @Test
  void shouldReturnCreatedOnlyWhenAddVinCreatesAssociation() {
    VehicleOnboardingService onboardingService = mock(VehicleOnboardingService.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    VinController controller =
        new VinController(
            mock(VinService.class),
            onboardingService,
            mock(VehicleDashboardService.class),
            userRepository);
    AuthenticatedUser authUser = user();
    User user = new User();
    user.setUserId(1L);
    AddVinRequest request = new AddVinRequest("JTENU5JR6M5962554", 45000, null);
    AddVinResponse created =
        new AddVinResponse(
            "JTENU5JR6M5962554",
            45000,
            7L,
            "Toyota",
            "4RUNNER",
            "SRS Prem",
            "2021",
            List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg"),
            "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg",
            false,
            false,
            true);
    AddVinResponse existing =
        new AddVinResponse(
            "JTENU5JR6M5962554",
            32000,
            7L,
            "Toyota",
            "4RUNNER",
            "SRS Prem",
            "2021",
            List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg"),
            "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg",
            false,
            false,
            false);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(onboardingService.addVinToUser(user, request))
        .thenReturn(new AddVinOutcome.Completed(created), new AddVinOutcome.Completed(existing));

    var createdResponse = controller.addVin(authUser, request);
    var existingResponse = controller.addVin(authUser, request);

    assertEquals(HttpStatus.CREATED, createdResponse.getStatusCode());
    assertEquals(created, createdResponse.getBody());
    assertEquals(HttpStatus.OK, existingResponse.getStatusCode());
    assertEquals(existing, existingResponse.getBody());
    verify(onboardingService, times(2)).addVinToUser(user, request);
  }

  @Test
  void shouldUpdateSelectedPhotoForCurrentUserVin() {
    VinService vinService = mock(VinService.class);
    VinController controller =
        controller(
            vinService, mock(VehicleOnboardingService.class), mock(VehicleDashboardService.class));
    AuthenticatedUser user = user();
    UserVehicleResponse vehicle = userVehicleResponse();
    UpdateVehiclePhotoRequest request =
        new UpdateVehiclePhotoRequest("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg");

    when(vinService.updateSelectedImage(1L, "JTENU5JR6M5962554", request.selectedImageUrl()))
        .thenReturn(Optional.of(vehicle));
    when(vinService.updateSelectedImage(1L, "MISSINGVIN1234567", request.selectedImageUrl()))
        .thenReturn(Optional.empty());

    var updatedResponse = controller.updateSelectedPhoto(user, "JTENU5JR6M5962554", request);
    var missingResponse = controller.updateSelectedPhoto(user, "MISSINGVIN1234567", request);

    assertEquals(HttpStatus.OK, updatedResponse.getStatusCode());
    assertEquals(vehicle, updatedResponse.getBody());
    assertEquals(HttpStatus.NOT_FOUND, missingResponse.getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED,
        controller.updateSelectedPhoto(null, "JTENU5JR6M5962554", request).getStatusCode());
  }

  @Test
  void shouldDeleteVinForCurrentUser() {
    VinService vinService = mock(VinService.class);
    VinController controller =
        controller(
            vinService, mock(VehicleOnboardingService.class), mock(VehicleDashboardService.class));
    AuthenticatedUser user = user();

    when(vinService.deleteVin(1L, "JTENU5JR6M5962554")).thenReturn(true);
    when(vinService.deleteVin(1L, "MISSINGVIN1234567")).thenReturn(false);

    var deletedResponse = controller.deleteVin(user, "JTENU5JR6M5962554");
    var missingResponse = controller.deleteVin(user, "MISSINGVIN1234567");

    assertEquals(HttpStatus.NO_CONTENT, deletedResponse.getStatusCode());
    assertEquals(HttpStatus.NOT_FOUND, missingResponse.getStatusCode());
    assertEquals(
        HttpStatus.UNAUTHORIZED, controller.deleteVin(null, "JTENU5JR6M5962554").getStatusCode());
  }

  private VinController controller(
      VinService vinService,
      VehicleOnboardingService onboardingService,
      VehicleDashboardService dashboardService) {
    return new VinController(
        vinService, onboardingService, dashboardService, mock(UserRepositoryJPA.class));
  }

  private AuthenticatedUser user() {
    return new AuthenticatedUser(1L, "driver@example.com", Role.USER);
  }

  private VehicleDetailResponse vehicleDetailResponse() {
    return new VehicleDetailResponse(
        "JTENU5JR6M5962554",
        7L,
        "Toyota",
        "4RUNNER",
        "SRS Prem",
        "2021",
        "SUV",
        "JTENU5JR6M5962554",
        "Japan",
        "SUV",
        "V6",
        "Automatic",
        "4WD",
        "https://example.com/manual",
        45000,
        List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg"),
        "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg");
  }

  private UserVehicleResponse userVehicleResponse() {
    return new UserVehicleResponse(
        "JTENU5JR6M5962554",
        45000,
        7L,
        "Toyota",
        "4RUNNER",
        "SRS Prem",
        "2021",
        List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg"),
        "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg");
  }
}
