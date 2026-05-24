package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.CompletedRecallRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VinServiceTest {

  @Test
  void shouldDelegateFindVinsByUserId() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VinService service =
        new VinService(
            vinRepository,
            userVinRepository,
            mock(CompletedMaintenanceRepositoryJPA.class),
            mock(CompletedRecallRepositoryJPA.class));
    Vin vin = vin();
    UserVin userVin = userVin(vin);

    when(userVinRepository.findAllForUser(1L)).thenReturn(List.of(userVin));

    var responses = service.findVinsByUserId(1L);

    assertEquals(1, responses.size());
    assertEquals(vin.getVin(), responses.getFirst().vin());
    assertEquals(45000, responses.getFirst().currentMileage());
    assertEquals(
        "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg",
        responses.getFirst().selectedImageUrl());
  }

  @Test
  void shouldReturnVehicleDetailForUserVin() {
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VinService service =
        new VinService(
            mock(VinRepositoryJPA.class),
            userVinRepository,
            mock(CompletedMaintenanceRepositoryJPA.class),
            mock(CompletedRecallRepositoryJPA.class));
    UserVin userVin = userVin(vin());
    VehicleType vehicleType = userVin.getVin().getVehicleType();
    vehicleType.setVehicleStyle("SUV");
    vehicleType.setSourceVin("JTENU5JR6M5962554");
    vehicleType.setOrigin("Japan");
    vehicleType.setBody("SUV");
    vehicleType.setEngineDescription("V6");
    vehicleType.setTransmissionStyle("Automatic");
    vehicleType.setDriveType("4WD");
    vehicleType.setOwnersManual("https://example.com/manual");

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));

    var response = service.findVehicleDetailForUser(1L, " jtenu5jr6m5962554 ");

    assertTrue(response.isPresent());
    assertEquals("JTENU5JR6M5962554", response.get().vin());
    assertEquals(7L, response.get().vehicleTypeId());
    assertEquals("Toyota", response.get().vehicleMake());
    assertEquals("4RUNNER", response.get().vehicleModel());
    assertEquals("SRS Prem", response.get().vehicleTrim());
    assertEquals("2021", response.get().vehicleYear());
    assertEquals("SUV", response.get().vehicleStyle());
    assertEquals(45000, response.get().currentMileage());
    assertEquals(
        "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg",
        response.get().selectedImageUrl());
  }

  @Test
  void shouldUpdateMileageForUserVin() {
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VinService service =
        new VinService(
            mock(VinRepositoryJPA.class),
            userVinRepository,
            mock(CompletedMaintenanceRepositoryJPA.class),
            mock(CompletedRecallRepositoryJPA.class));
    UserVin userVin = userVin(vin());

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(userVinRepository.save(userVin)).thenReturn(userVin);

    var response =
        service.updateMileage(
            1L, " jtenu5jr6m5962554 ", new com.capstone.models.dto.UpdateMileageRequest(52000));

    assertTrue(response.isPresent());
    assertEquals(52000, response.get().currentMileage());
    assertEquals(52000, userVin.getCurrentMileage());
  }

  @Test
  void shouldUpdateSelectedImageWhenUrlIsAvailable() {
    VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VinService service =
        new VinService(
            vinRepository,
            userVinRepository,
            mock(CompletedMaintenanceRepositoryJPA.class),
            mock(CompletedRecallRepositoryJPA.class));
    UserVin userVin = userVin(vin());

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));
    when(userVinRepository.save(userVin)).thenReturn(userVin);

    var response =
        service.updateSelectedImage(
            1L,
            " jtenu5jr6m5962554 ",
            "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg");

    assertTrue(response.isPresent());
    assertEquals(
        "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg",
        response.get().selectedImageUrl());
  }

  @Test
  void shouldRejectSelectedImageOutsideAvailableImages() {
    UserVin userVin = userVin(vin());
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VinService service =
        new VinService(
            mock(VinRepositoryJPA.class),
            userVinRepository,
            mock(CompletedMaintenanceRepositoryJPA.class),
            mock(CompletedRecallRepositoryJPA.class));

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
            completedRecallRepository);
    UserVin userVin = userVin(vin());

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(userVin));

    boolean result = service.deleteVin(1L, "JTENU5JR6M5962554");

    assertTrue(result);
    verify(completedMaintenanceRepository).deleteAllForUserVin(1L, "JTENU5JR6M5962554");
    verify(completedRecallRepository).deleteAllForUserVin(1L, "JTENU5JR6M5962554");
    verify(userVinRepository).deleteForUserVin(1L, "JTENU5JR6M5962554");
    verify(vinRepository).deleteOrphanedVins(List.of("JTENU5JR6M5962554"));
  }

  @Test
  void shouldReturnFalseWhenDeletingNonExistentVin() {
    UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
    VinService service =
        new VinService(
            mock(VinRepositoryJPA.class),
            userVinRepository,
            mock(CompletedMaintenanceRepositoryJPA.class),
            mock(CompletedRecallRepositoryJPA.class));

    when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554")).thenReturn(Optional.empty());

    boolean result = service.deleteVin(1L, "JTENU5JR6M5962554");

    assertFalse(result);
    verify(userVinRepository, never()).deleteForUserVin(any(), any());
  }

  private Vin vin() {
    VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
    vehicleType.setVehicleTypeId(7L);
    return new Vin("JTENU5JR6M5962554", 45000, vehicleType);
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
}
