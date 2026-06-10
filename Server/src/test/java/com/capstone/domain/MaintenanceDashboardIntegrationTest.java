package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.data.read.VehicleReadService;
import com.capstone.models.dto.CompleteMaintenanceRequest;
import com.capstone.models.dto.VehicleDashboardResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "spring.profiles.active=test",
      "spring.datasource.url=jdbc:h2:mem:maintenance-dashboard;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.flyway.enabled=true",
      "spring.flyway.target=1"
    })
class MaintenanceDashboardIntegrationTest {

  private static final String CAMRY_VIN = "4T1C11AK5LU123456";
  private static final long TEST_USER_ID = 1L;

  @Autowired MaintenanceTrackingService maintenanceTrackingService;
  @Autowired VehicleReadService vehicleReadService;

  @Test
  void completedMaintenanceAppearsOnVehicleDashboardAfterMarkingComplete() {
    VehicleDashboardResponse before =
        vehicleReadService.findDashboard(TEST_USER_ID, CAMRY_VIN).orElseThrow();
    int completedBefore = before.completedMaintenance().size();
    long upcomingItemCountBefore =
        before.upcomingMaintenance().stream().mapToLong(interval -> interval.items().size()).sum();

    maintenanceTrackingService.completeMaintenance(
        TEST_USER_ID,
        new CompleteMaintenanceRequest(
            CAMRY_VIN, 3L, LocalDate.of(2026, 5, 28), 45_200, 34.0, null));

    VehicleDashboardResponse after =
        vehicleReadService.findDashboard(TEST_USER_ID, CAMRY_VIN).orElseThrow();

    assertEquals(completedBefore + 1, after.completedMaintenance().size());
    assertTrue(
        after.completedMaintenance().stream().anyMatch(item -> item.maintMileageId().equals(3L)));
    long upcomingItemCountAfter =
        after.upcomingMaintenance().stream().mapToLong(interval -> interval.items().size()).sum();
    assertEquals(upcomingItemCountBefore - 1, upcomingItemCountAfter);
  }
}
