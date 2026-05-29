package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.capstone.models.dto.*;
import com.capstone.read.VehicleReadService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VehicleDashboardServiceTest {

  @Test
  void shouldReturnEmptyWhenVehicleNotFoundForUser() {
    VehicleReadService vehicleReadService = mock(VehicleReadService.class);
    VehicleDashboardService service = new VehicleDashboardService(vehicleReadService);

    when(vehicleReadService.findDashboard(1L, "MISSINGVIN1234567")).thenReturn(Optional.empty());

    assertTrue(service.findDashboardForUser(1L, "MISSINGVIN1234567").isEmpty());
  }

  @Test
  void shouldDelegateDashboardLoadToReadService() {
    VehicleReadService vehicleReadService = mock(VehicleReadService.class);
    VehicleDashboardService service = new VehicleDashboardService(vehicleReadService);
    VehicleDashboardResponse dashboard =
        new VehicleDashboardResponse(
            vehicleDetailResponse(), List.of(), List.of(), List.of(), List.of(), List.of());

    when(vehicleReadService.findDashboard(1L, "JTENU5JR6M5962554"))
        .thenReturn(Optional.of(dashboard));

    Optional<VehicleDashboardResponse> result =
        service.findDashboardForUser(1L, "JTENU5JR6M5962554");

    assertTrue(result.isPresent());
    assertEquals(dashboard, result.get());
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
}
