package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.CompletedRecallRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.data.read.VehicleReadService;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;
import com.capstone.models.dto.UpdateMileageRequest;
import com.capstone.models.dto.UserVehicleResponse;
import com.capstone.models.dto.VehicleDetailResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VinServiceTest {

  @Test
  void shouldUpdateMileageForUserVin() {
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VehicleReadService vehicleReadService = mock(VehicleReadService.class);
    VinService service =
        vinService(mock(VinRepositoryJPA.class), userVinRepository, vehicleReadService);
    UserVin userVin = userVin(vin());
    VehicleDetailResponse detail = vehicleDetailResponse();

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(vehicleReadService.findVehicleDetail(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(detail));

    var response =
        service.updateMileage(1L, " jtenu5jr6m5962554 ", new UpdateMileageRequest(52000));

    assertTrue(response.isPresent());
    assertEquals(52000, response.get().currentMileage());
    assertEquals(52000, userVin.getCurrentMileage());
    verify(userVinRepository).save(userVin);
  }

  @Test
  void shouldUpdateSelectedImageWhenUrlIsAvailable() {
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VehicleReadService vehicleReadService = mock(VehicleReadService.class);
    VinService service =
        vinService(mock(VinRepositoryJPA.class), userVinRepository, vehicleReadService);
    UserVin userVin = userVin(vin());
    UserVehicleResponse vehicle =
        new UserVehicleResponse(
            "JTENU5JR6M5962554",
            45000,
            7L,
            "Toyota",
            "4RUNNER",
            "SRS Prem",
            "2021",
            List.of(),
            "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg");

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(vehicleReadService.findVehiclesForUser(1L)).thenReturn(List.of(vehicle));

    var response =
        service.updateSelectedImage(
            1L,
            " jtenu5jr6m5962554 ",
            "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg");

    assertTrue(response.isPresent());
    assertEquals(
        "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg",
        response.get().selectedImageUrl());
    verify(userVinRepository).save(userVin);
  }

  @Test
  void shouldRejectSelectedImageOutsideAvailableImages() {
    UserVin userVin = userVin(vin());
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VinService service =
        vinService(mock(VinRepositoryJPA.class), userVinRepository, mock(VehicleReadService.class));

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                service.updateSelectedImage(
                    1L, "JTENU5JR6M5962554", "https://example.com/not-available.jpg"));

    assertEquals("Selected image URL must be one of the available vehicle images", ex.getMessage());
  }

  @Test
  void shouldDeleteVinWhenItExists() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    CompletedMaintenanceRepositoryJPA completedMaintenanceRepository =
        mock(CompletedMaintenanceRepositoryJPA.class);
    CompletedRecallRepositoryJPA completedRecallRepository =
        mock(CompletedRecallRepositoryJPA.class);
    VinService service =
        new VinService(
            vinRepository,
            userVinRepository,
            completedMaintenanceRepository,
            completedRecallRepository,
            mock(VehicleReadService.class));
    UserVin userVin = userVin(vin());

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));

    assertTrue(service.deleteVin(1L, "JTENU5JR6M5962554"));
    verify(completedMaintenanceRepository).deleteAllForUserVin(1L, "JTENU5JR6M5962554");
    verify(completedRecallRepository).deleteAllForUserVin(1L, "JTENU5JR6M5962554");
    verify(userVinRepository).deleteForUserVin(1L, "JTENU5JR6M5962554");
    verify(vinRepository).deleteOrphanedVins(List.of("JTENU5JR6M5962554"));
  }

  @Test
  void shouldReturnFalseWhenDeletingNonExistentVin() {
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VinService service =
        vinService(mock(VinRepositoryJPA.class), userVinRepository, mock(VehicleReadService.class));

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554")).thenReturn(Optional.empty());

    assertFalse(service.deleteVin(1L, "JTENU5JR6M5962554"));
    verify(userVinRepository, never()).deleteForUserVin(any(), any());
  }

  private VinService vinService(
      VinRepositoryJPA vinRepository,
      UserVinRepositoryJPA userVinRepository,
      VehicleReadService vehicleReadService) {
    return new VinService(
        vinRepository,
        userVinRepository,
        mock(CompletedMaintenanceRepositoryJPA.class),
        mock(CompletedRecallRepositoryJPA.class),
        vehicleReadService);
  }

  private Vin vin() {
    VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
    vehicleType.setVehicleTypeId(7L);
    return new Vin("JTENU5JR6M5962554", vehicleType);
  }

  private UserVin userVin(Vin vin) {
    User user = new User();
    user.setUserId(1L);
    UserVin userVin = new UserVin(user, vin, 45000);
    userVin.setAvailableImageUrls(
        List.of(
            "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg",
            "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg"));
    userVin.setSelectedImageUrl("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg");
    return userVin;
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
        52000,
        List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg"),
        "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg");
  }
}
