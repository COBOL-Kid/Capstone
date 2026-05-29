package com.capstone.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  private static final long USER_ID = 1L;
  private static final String CAMRY_VIN = "4T1C11AK5LU123456";
  private static final String CIVIC_VIN = "2HGFC2F59JH543210";

  @Autowired VehicleReadDao vehicleReadDao;

  @Test
  void findUserVinDetail_returnsSeededVehicleFields() {
    var detail = vehicleReadDao.findUserVinDetail(USER_ID, CAMRY_VIN);

    assertTrue(detail.isPresent());
    assertEquals(CAMRY_VIN, detail.get().vin());
    assertEquals("Sedan", detail.get().body());
    assertEquals(
        "[\"https://images.unsplash.com/photo-1621007947382-bcb49c457f54\",\"https://images.unsplash.com/photo-1609521263047-f8f205293f24\"]",
        detail.get().availableImageUrlsJson());
  }

  @Test
  void findUserVehicles_returnsSeededVehiclesForUser() {
    var vehicles = vehicleReadDao.findUserVehicles(USER_ID);

    assertFalse(vehicles.isEmpty());
    assertTrue(vehicles.stream().anyMatch(v -> v.vin().equals(CAMRY_VIN)));
    assertTrue(vehicles.stream().anyMatch(v -> v.vin().equals(CIVIC_VIN)));
  }

  @Test
  void findUpcomingMaintenanceRoots_excludesCompletedItems() {
    var camry = vehicleReadDao.findUserVinDetail(USER_ID, CAMRY_VIN).orElseThrow();
    int threshold = camry.currentMileage() + 10_000;

    var roots =
        vehicleReadDao.findUpcomingMaintenanceRoots(
            camry.vehicleTypeId(), threshold, USER_ID, CAMRY_VIN);

    assertEquals(
        List.of(3L, 4L),
        roots.stream().map(VehicleReadDao.UpcomingMaintRootRow::maintMileageId).toList());
    assertTrue(roots.stream().anyMatch(VehicleReadDao.UpcomingMaintRootRow::inspect));
  }

  @Test
  void findUncompletedRecalls_excludesCompletedRecall() {
    var recalls = vehicleReadDao.findUncompletedRecalls(USER_ID, CAMRY_VIN);

    assertEquals(
        List.of(1L), recalls.stream().map(VehicleReadDao.UncompletedRecallRow::recallId).toList());
  }

  @Test
  void findCompletedMaintenance_returnsSeededRows() {
    var completed = vehicleReadDao.findCompletedMaintenance(USER_ID, CAMRY_VIN);

    assertEquals(2, completed.size());
    assertTrue(
        completed.stream()
            .anyMatch(
                row ->
                    row.maintMileageId() == 1L && row.maintDesc().equals("Change - Engine oil")));
  }

  @Test
  void findCompletedRecalls_returnsSeededRows() {
    var completed = vehicleReadDao.findCompletedRecalls(USER_ID, CAMRY_VIN);

    assertEquals(1, completed.size());
    assertEquals(2L, completed.getFirst().recallId());
  }

  @Test
  void findMiscCosts_returnsSeededRowsForVehicleType() {
    var camry = vehicleReadDao.findUserVinDetail(USER_ID, CAMRY_VIN).orElseThrow();

    var miscCosts = vehicleReadDao.findMiscCosts(camry.vehicleTypeId());

    assertFalse(miscCosts.isEmpty());
    assertTrue(miscCosts.stream().anyMatch(row -> row.maintTitle().equals("Oil Change")));
  }

  @Test
  void findPartLines_returnsEmptyForEmptyIdCollection() {
    assertTrue(vehicleReadDao.findPartLines(List.of()).isEmpty());
  }

  @Test
  void findLaborLines_returnsEmptyForEmptyIdCollection() {
    assertTrue(vehicleReadDao.findLaborLines(List.of()).isEmpty());
  }

  @Test
  void findMaintSummaries_returnsEmptyForEmptyMileageDues() {
    var camry = vehicleReadDao.findUserVinDetail(USER_ID, CAMRY_VIN).orElseThrow();

    assertTrue(vehicleReadDao.findMaintSummaries(camry.vehicleTypeId(), List.of()).isEmpty());
  }

  @Test
  void findPartLines_returnsSeededPartDescriptions() {
    var partLines = vehicleReadDao.findPartLines(List.of(4L));

    assertFalse(partLines.isEmpty());
    assertEquals("Replace - Spark plugs", partLines.getFirst().partDesc());
  }

  @Test
  void findMaintSummaries_returnsSeededSummariesForMileageDues() {
    var camry = vehicleReadDao.findUserVinDetail(USER_ID, CAMRY_VIN).orElseThrow();
    int threshold = camry.currentMileage() + 10_000;
    var upcomingRoots =
        vehicleReadDao.findUpcomingMaintenanceRoots(
            camry.vehicleTypeId(), threshold, USER_ID, CAMRY_VIN);
    List<Integer> mileageDues =
        upcomingRoots.stream()
            .map(VehicleReadDao.UpcomingMaintRootRow::mileageDue)
            .distinct()
            .toList();

    var summaries = vehicleReadDao.findMaintSummaries(camry.vehicleTypeId(), mileageDues);

    assertFalse(summaries.isEmpty());
    assertTrue(summaries.stream().anyMatch(summary -> summary.mileageDue() == 45_000));
  }

  @Test
  void findLaborLines_returnsSeededLaborForUpcomingMaintenance() {
    var camry = vehicleReadDao.findUserVinDetail(USER_ID, CAMRY_VIN).orElseThrow();
    int threshold = camry.currentMileage() + 10_000;
    var upcomingRoots =
        vehicleReadDao.findUpcomingMaintenanceRoots(
            camry.vehicleTypeId(), threshold, USER_ID, CAMRY_VIN);
    List<Long> maintIds =
        upcomingRoots.stream().map(VehicleReadDao.UpcomingMaintRootRow::maintMileageId).toList();

    assertFalse(vehicleReadDao.findLaborLines(maintIds).isEmpty());
  }
}
