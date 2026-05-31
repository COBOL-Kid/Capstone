package com.capstone.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.data.read.VehicleReadDao;
import com.capstone.data.read.VehicleReadService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:vehicle-read-dao;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=true",
      "spring.flyway.target=1"
    })
class VehicleReadDaoTest {

  @Autowired VehicleReadDao vehicleReadDao;
  @Autowired VehicleReadService vehicleReadService;

  @Test
  void exercisesEveryVehicleReadQueryAgainstSeedData() {
    long userId = 1L;
    String camryVin = "4T1C11AK5LU123456";
    String civicVin = "2HGFC2F59JH543210";

    assertFalse(vehicleReadDao.findUserVehicles(userId).isEmpty());

    var camry = vehicleReadDao.findUserVinDetail(userId, camryVin).orElseThrow();
    assertEquals("4T1C11AK5LU123456", camry.vin());
    assertEquals("Sedan", camry.body());
    assertEquals(
        "[\"https://images.unsplash.com/photo-1621007947382-bcb49c457f54\",\"https://images.unsplash.com/photo-1609521263047-f8f205293f24\"]",
        camry.availableImageUrlsJson());
    assertFalse(vehicleReadDao.findCompletedMaintenance(userId, camryVin).isEmpty());

    int threshold = camry.currentMileage() + 10_000;
    var upcomingRoots =
        vehicleReadDao.findUpcomingMaintenanceRoots(
            camry.vehicleTypeId(), threshold, userId, camryVin);
    assertFalse(upcomingRoots.isEmpty());
    assertTrue(upcomingRoots.stream().anyMatch(VehicleReadDao.UpcomingMaintRootRow::inspect));

    List<Long> maintIds =
        upcomingRoots.stream().map(VehicleReadDao.UpcomingMaintRootRow::maintMileageId).toList();
    assertFalse(vehicleReadDao.findLaborLines(maintIds).isEmpty());

    List<Integer> mileageDues =
        upcomingRoots.stream()
            .map(VehicleReadDao.UpcomingMaintRootRow::mileageDue)
            .distinct()
            .toList();
    assertFalse(vehicleReadDao.findMaintSummaries(camry.vehicleTypeId(), mileageDues).isEmpty());

    assertFalse(vehicleReadDao.findUncompletedRecalls(userId, camryVin).isEmpty());
    assertFalse(vehicleReadDao.findCompletedRecalls(userId, camryVin).isEmpty());
    assertFalse(vehicleReadDao.findMiscCosts(camry.vehicleTypeId()).isEmpty());

    var camryWarranty = vehicleReadDao.findVehicleWarranty("2020", "Toyota", "Camry").orElseThrow();
    assertFalse(camryWarranty.coverages().isBlank());

    var partLines = vehicleReadDao.findPartLines(List.of(4L));
    assertFalse(partLines.isEmpty());
    assertEquals("Replace - Spark plugs", partLines.getFirst().partDesc());

    var dashboard = vehicleReadService.findDashboard(userId, camryVin).orElseThrow();
    assertEquals("2020", dashboard.vehicleWarranty().vehicleYear());
    assertFalse(dashboard.vehicleWarranty().coverages().isEmpty());
    assertFalse(dashboard.upcomingMaintenance().isEmpty());
    assertTrue(
        dashboard.upcomingMaintenance().stream()
            .flatMap(interval -> interval.items().stream())
            .anyMatch(item -> item.isInspect()));

    assertTrue(vehicleReadDao.findUserVinDetail(userId, civicVin).isPresent());
    assertTrue(vehicleReadService.findDashboard(userId, civicVin).isPresent());
  }

  @Test
  void returnsEmptyForUnknownUserOrVin() {
    assertTrue(vehicleReadDao.findUserVinDetail(99L, "4T1C11AK5LU123456").isEmpty());
    assertTrue(vehicleReadDao.findUserVinDetail(1L, "UNKNOWNVIN1234567").isEmpty());
    assertTrue(vehicleReadService.findDashboard(1L, "UNKNOWNVIN1234567").isEmpty());
  }

  @Test
  void returnsEmptyListsWhenMaintenanceIdsAreEmpty() {
    assertTrue(vehicleReadDao.findPartLines(List.of()).isEmpty());
    assertTrue(vehicleReadDao.findLaborLines(List.of()).isEmpty());
    assertTrue(vehicleReadDao.findMaintSummaries(7L, List.of()).isEmpty());
  }
}
