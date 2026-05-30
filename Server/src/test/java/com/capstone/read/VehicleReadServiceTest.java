package com.capstone.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capstone.data.read.UserVinDetailRow;
import com.capstone.data.read.UserVinListRow;
import com.capstone.data.read.VehicleReadDao;
import com.capstone.data.read.VehicleReadDao.CompletedMaintenanceRow;
import com.capstone.data.read.VehicleReadDao.CompletedRecallRow;
import com.capstone.data.read.VehicleReadDao.MaintLaborLineRow;
import com.capstone.data.read.VehicleReadDao.MaintPartLineRow;
import com.capstone.data.read.VehicleReadDao.MaintSummaryRow;
import com.capstone.data.read.VehicleReadDao.MiscMaintCostRow;
import com.capstone.data.read.VehicleReadDao.UncompletedRecallRow;
import com.capstone.data.read.VehicleReadDao.UpcomingMaintRootRow;
import com.capstone.data.read.VehicleReadService;
import com.capstone.models.dto.VehicleDashboardResponse;
import com.capstone.models.dto.VehicleDetailResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleReadServiceTest {

  private static final long USER_ID = 1L;
  private static final String RAW_VIN = " jtenu5jr6m5962554 ";
  private static final String NORMALIZED_VIN = "JTENU5JR6M5962554";

  @Mock VehicleReadDao vehicleReadDao;

  @InjectMocks VehicleReadService vehicleReadService;

  @Test
  void findVehicleDetail_normalizesVinBeforeDaoCall() {
    UserVinDetailRow detailRow = userVinDetailRow();
    when(vehicleReadDao.findUserVinDetail(USER_ID, NORMALIZED_VIN))
        .thenReturn(Optional.of(detailRow));

    Optional<VehicleDetailResponse> result = vehicleReadService.findVehicleDetail(USER_ID, RAW_VIN);

    assertTrue(result.isPresent());
    assertEquals(NORMALIZED_VIN, result.get().vin());
    verify(vehicleReadDao).findUserVinDetail(USER_ID, NORMALIZED_VIN);
  }

  @Test
  void findDashboard_returnsEmptyWhenDetailMissing() {
    when(vehicleReadDao.findUserVinDetail(USER_ID, NORMALIZED_VIN)).thenReturn(Optional.empty());

    assertTrue(vehicleReadService.findDashboard(USER_ID, RAW_VIN).isEmpty());
  }

  @Test
  void findDashboard_normalizesVinAndAssemblesUpcomingMaintenance() {
    UserVinDetailRow detailRow = userVinDetailRow();
    when(vehicleReadDao.findUserVinDetail(USER_ID, NORMALIZED_VIN))
        .thenReturn(Optional.of(detailRow));
    when(vehicleReadDao.findCompletedMaintenance(USER_ID, NORMALIZED_VIN)).thenReturn(List.of());
    when(vehicleReadDao.findUpcomingMaintenanceRoots(
            eq(7L), eq(55_000), eq(USER_ID), eq(NORMALIZED_VIN)))
        .thenReturn(
            List.of(
                new UpcomingMaintRootRow(10L, 30_000, "Inspect - Transmission fluid", true),
                new UpcomingMaintRootRow(11L, 45_000, "Replace - Spark plugs", false)));
    when(vehicleReadDao.findLaborLines(List.of(10L, 11L)))
        .thenReturn(
            List.of(
                new MaintLaborLineRow(
                    10L,
                    new BigDecimal("0.40"),
                    new BigDecimal("85.00"),
                    new BigDecimal("34.00"),
                    "USD")));
    when(vehicleReadDao.findPartLines(List.of(10L, 11L)))
        .thenReturn(
            List.of(
                new MaintPartLineRow(
                    11L, "Replace - Spark plugs", new BigDecimal("48.00"), "USD")));
    when(vehicleReadDao.findMaintSummaries(eq(7L), eq(List.of(30_000, 45_000))))
        .thenReturn(
            List.of(
                new MaintSummaryRow(
                    30_000,
                    new BigDecimal("0.00"),
                    new BigDecimal("34.00"),
                    new BigDecimal("34.00"),
                    "USD"),
                new MaintSummaryRow(
                    45_000,
                    new BigDecimal("48.00"),
                    new BigDecimal("102.00"),
                    new BigDecimal("150.00"),
                    "USD")));
    when(vehicleReadDao.findUncompletedRecalls(USER_ID, NORMALIZED_VIN)).thenReturn(List.of());
    when(vehicleReadDao.findCompletedRecalls(USER_ID, NORMALIZED_VIN)).thenReturn(List.of());
    when(vehicleReadDao.findMiscCosts(7L)).thenReturn(List.of());

    VehicleDashboardResponse dashboard =
        vehicleReadService.findDashboard(USER_ID, RAW_VIN).orElseThrow();

    verify(vehicleReadDao).findUserVinDetail(USER_ID, NORMALIZED_VIN);
    assertEquals(2, dashboard.upcomingMaintenance().size());
    assertEquals(30_000, dashboard.upcomingMaintenance().getFirst().mileageDue());
    assertEquals(45_000, dashboard.upcomingMaintenance().get(1).mileageDue());
    assertEquals(1, dashboard.upcomingMaintenance().get(1).items().size());
    assertEquals(
        new BigDecimal("150.00"), dashboard.upcomingMaintenance().get(1).summary().totalCost());

    var inspectItem = dashboard.upcomingMaintenance().getFirst().items().getFirst();
    assertTrue(inspectItem.isInspect());
    assertEquals(new BigDecimal("34.00"), inspectItem.labor().totalCost());
    assertTrue(inspectItem.parts().isEmpty());
  }

  @Test
  void findDashboard_returnsNullSummaryWhenMaintSummaryMissing() {
    UserVinDetailRow detailRow = userVinDetailRow();
    when(vehicleReadDao.findUserVinDetail(USER_ID, NORMALIZED_VIN))
        .thenReturn(Optional.of(detailRow));
    when(vehicleReadDao.findCompletedMaintenance(USER_ID, NORMALIZED_VIN)).thenReturn(List.of());
    when(vehicleReadDao.findUpcomingMaintenanceRoots(
            eq(7L), eq(55_000), eq(USER_ID), eq(NORMALIZED_VIN)))
        .thenReturn(
            List.of(new UpcomingMaintRootRow(10L, 30_000, "Inspect - Transmission fluid", true)));
    when(vehicleReadDao.findLaborLines(List.of(10L))).thenReturn(List.of());
    when(vehicleReadDao.findPartLines(List.of(10L))).thenReturn(List.of());
    when(vehicleReadDao.findMaintSummaries(eq(7L), eq(List.of(30_000)))).thenReturn(List.of());
    when(vehicleReadDao.findUncompletedRecalls(USER_ID, NORMALIZED_VIN)).thenReturn(List.of());
    when(vehicleReadDao.findCompletedRecalls(USER_ID, NORMALIZED_VIN)).thenReturn(List.of());
    when(vehicleReadDao.findMiscCosts(7L)).thenReturn(List.of());

    var interval =
        vehicleReadService
            .findDashboard(USER_ID, NORMALIZED_VIN)
            .orElseThrow()
            .upcomingMaintenance()
            .getFirst();

    assertEquals(null, interval.summary());
  }

  @Test
  void findVehicleDetail_throwsWhenImageUrlsJsonIsInvalid() {
    UserVinDetailRow detailRow =
        new UserVinDetailRow(
            NORMALIZED_VIN,
            7L,
            "Toyota",
            "4RUNNER",
            "SRS Prem",
            "2021",
            "SUV",
            NORMALIZED_VIN,
            "Japan",
            "SUV",
            "V6",
            "Automatic",
            "4WD",
            "https://example.com/manual",
            45_000,
            "not-json",
            "https://example.com/a.jpg");
    when(vehicleReadDao.findUserVinDetail(USER_ID, NORMALIZED_VIN))
        .thenReturn(Optional.of(detailRow));

    assertThrows(
        IllegalStateException.class,
        () -> vehicleReadService.findVehicleDetail(USER_ID, NORMALIZED_VIN));
  }

  @Test
  void findVehicleDetail_parsesImageUrlsJson() {
    UserVinDetailRow detailRow =
        new UserVinDetailRow(
            NORMALIZED_VIN,
            7L,
            "Toyota",
            "4RUNNER",
            "SRS Prem",
            "2021",
            "SUV",
            NORMALIZED_VIN,
            "Japan",
            "SUV",
            "V6",
            "Automatic",
            "4WD",
            "https://example.com/manual",
            45_000,
            "[\"https://example.com/a.jpg\",\"https://example.com/b.jpg\"]",
            "https://example.com/a.jpg");
    when(vehicleReadDao.findUserVinDetail(USER_ID, NORMALIZED_VIN))
        .thenReturn(Optional.of(detailRow));

    VehicleDetailResponse detail =
        vehicleReadService.findVehicleDetail(USER_ID, NORMALIZED_VIN).orElseThrow();

    assertEquals(
        List.of("https://example.com/a.jpg", "https://example.com/b.jpg"),
        detail.availableImageUrls());
    assertEquals("https://example.com/a.jpg", detail.selectedImageUrl());
  }

  @Test
  void findVehiclesForUser_mapsListResponseWithoutImageUrlList() {
    when(vehicleReadDao.findUserVehicles(USER_ID))
        .thenReturn(
            List.of(
                new UserVinListRow(
                    NORMALIZED_VIN,
                    45_000,
                    7L,
                    "Toyota",
                    "4RUNNER",
                    "SRS Prem",
                    "2021",
                    "photo.jpg")));

    var vehicles = vehicleReadService.findVehiclesForUser(USER_ID);

    assertEquals(1, vehicles.size());
    assertEquals(NORMALIZED_VIN, vehicles.getFirst().vin());
    assertTrue(vehicles.getFirst().availableImageUrls().isEmpty());
    assertEquals("photo.jpg", vehicles.getFirst().selectedImageUrl());
  }

  @Test
  void findDashboard_mapsCompletedMaintenanceRecallsAndMiscCosts() {
    UserVinDetailRow detailRow = userVinDetailRow();
    when(vehicleReadDao.findUserVinDetail(USER_ID, NORMALIZED_VIN))
        .thenReturn(Optional.of(detailRow));
    when(vehicleReadDao.findCompletedMaintenance(USER_ID, NORMALIZED_VIN))
        .thenReturn(
            List.of(
                new CompletedMaintenanceRow(
                    99L,
                    LocalDate.of(2024, 1, 2),
                    44_000,
                    25.0,
                    "done",
                    3L,
                    "Inspect - Transmission fluid",
                    30_000)));
    when(vehicleReadDao.findUpcomingMaintenanceRoots(anyLong(), anyInt(), anyLong(), anyString()))
        .thenReturn(List.of());
    when(vehicleReadDao.findUncompletedRecalls(USER_ID, NORMALIZED_VIN))
        .thenReturn(
            List.of(
                new UncompletedRecallRow(
                    1L,
                    "23V123000",
                    LocalDate.of(2023, 3, 15),
                    "FUEL",
                    "summary",
                    "consequence",
                    "remedy")));
    when(vehicleReadDao.findCompletedRecalls(USER_ID, NORMALIZED_VIN))
        .thenReturn(
            List.of(
                new CompletedRecallRow(
                    50L,
                    LocalDate.of(2022, 9, 18),
                    "City Toyota",
                    0.0,
                    null,
                    2L,
                    "22V456000",
                    LocalDate.of(2022, 8, 1),
                    "ELECTRICAL",
                    "summary",
                    "consequence",
                    "remedy")));
    when(vehicleReadDao.findMiscCosts(7L))
        .thenReturn(
            List.of(
                new MiscMaintCostRow(
                    1L, "Oil Change", "Replace engine oil", 65, 95, 45, 110, 145, 85)));

    VehicleDashboardResponse dashboard =
        vehicleReadService.findDashboard(USER_ID, NORMALIZED_VIN).orElseThrow();

    assertEquals(1, dashboard.completedMaintenance().size());
    assertEquals(NORMALIZED_VIN, dashboard.completedMaintenance().getFirst().vin());
    assertEquals(1, dashboard.uncompletedRecalls().size());
    assertEquals(1, dashboard.completedRecalls().size());
    assertEquals("Oil Change", dashboard.miscMaintenanceCosts().getFirst().maintTitle());
  }

  private UserVinDetailRow userVinDetailRow() {
    return new UserVinDetailRow(
        NORMALIZED_VIN,
        7L,
        "Toyota",
        "4RUNNER",
        "SRS Prem",
        "2021",
        "SUV",
        NORMALIZED_VIN,
        "Japan",
        "SUV",
        "V6",
        "Automatic",
        "4WD",
        "https://example.com/manual",
        45_000,
        "[\"https://example.com/a.jpg\"]",
        "https://example.com/a.jpg");
  }
}
