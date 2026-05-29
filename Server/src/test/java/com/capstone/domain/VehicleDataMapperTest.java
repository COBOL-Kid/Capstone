package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.capstone.integration.*;
import com.capstone.models.MaintMileage;
import com.capstone.models.MiscMaintCost;
import com.capstone.models.Recall;
import com.capstone.models.VehicleType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class VehicleDataMapperTest {

  private final VehicleDataMapper mapper = new VehicleDataMapper();

  @Test
  void shouldMapVehicleTypeFromProviderResponses() {
    VinDecodeResponse vinDecode = vinDecodeResponse();
    OwnerManualResponse ownerManual = ownerManualResponse();

    VehicleType vehicleType = mapper.toVehicleType(vinDecode, ownerManual);

    assertEquals("Chevrolet", vehicleType.getVehicleMake());
    assertEquals("Silverado 1500", vehicleType.getVehicleModel());
    assertEquals("ZR2", vehicleType.getVehicleTrim());
    assertEquals("2022", vehicleType.getVehicleYear());
    assertEquals("4x4 4dr Crew Cab 5.8 ft. SB", vehicleType.getVehicleStyle());
    assertEquals("3GCUDHEL3NG668790", vehicleType.getSourceVin());
    assertEquals("Mexico", vehicleType.getOrigin());
    assertEquals("Truck", vehicleType.getBody());
    assertEquals("5.3L V8 OHV 16V FFV", vehicleType.getEngineDescription());
    assertEquals("4WD", vehicleType.getDriveType());
    assertEquals("Automatic", vehicleType.getTransmissionStyle());
    assertEquals(
        "https://vhr.nyc3.cdn.digitaloceanspaces.com/owners-manual/toyota/2021_toyota_4runner_Toyota%202021%204Runner%20Owner%27s%20Manual%20OM35B41U.pdf",
        vehicleType.getOwnersManual());
  }

  @Test
  void shouldDeserializeVinDecodeResponseFromProviderJson() {
    String json =
        """
                {
                    "vin": "3GCUDHEL3NG668790",
                    "vinValid": true,
                    "wmi": "3GC",
                    "origin": "Mexico",
                    "squishVin": "3GCUDHELNG",
                    "checkDigit": "3",
                    "checksum": true,
                    "type": "Active",
                    "make": "Chevrolet",
                    "model": "Silverado 1500",
                    "trim": "ZR2",
                    "style": "4x4 4dr Crew Cab 5.8 ft. SB",
                    "body": "Truck",
                    "engine": "5.3L V8 OHV 16V FFV",
                    "drive": "4WD",
                    "transmission": "Automatic",
                    "vehicle": {
                        "vin": "3GCUDHEL3NG668790",
                        "year": 2022,
                        "make": "Chevrolet",
                        "model": "Silverado 1500",
                        "manufacturer": "General Motors de Mexico"
                    },
                    "photos": {
                        "hasRetailPhotos": true,
                        "hasWholesalePhotos": false,
                        "hasHistoricalPhotos": true,
                        "retailPhotoCount": 12
                    },
                    "ambiguous": false
                }
                """;

    VinDecodeResponse response = new ObjectMapper().readValue(json, VinDecodeResponse.class);

    assertEquals("3GCUDHEL3NG668790", response.vin());
    assertEquals(Boolean.TRUE, response.vinValid());
    assertEquals("Chevrolet", response.make());
    assertEquals("Silverado 1500", response.model());
    assertEquals("ZR2", response.trim());
    assertEquals("4x4 4dr Crew Cab 5.8 ft. SB", response.style());
    assertEquals("5.3L V8 OHV 16V FFV", response.engine());
    assertEquals(2022, response.vehicle().year());
    assertEquals(12, response.photos().retailPhotoCount());
  }

  @Test
  void shouldMapRepairEstimatesFromProviderResponse() {
    VehicleType vehicleType =
        new VehicleType("Dodge", "Durango", "R/T 4dr All-wheel Drive Automatic", "2014");
    RepairEstimatesResponse repairEstimates = repairEstimatesResponse();

    MaintenanceScheduleImport scheduleImport =
        mapper.toMaintenanceScheduleImport(vehicleType, repairEstimates);

    assertEquals(4, scheduleImport.maintMileages().size());
    assertEquals(1, scheduleImport.summaries().size());
    assertEquals(50000, scheduleImport.summaries().getFirst().getMileageDue());
    assertEquals(
        new BigDecimal("49.59"), scheduleImport.summaries().getFirst().getTotalPartsCost());
    assertEquals(
        new BigDecimal("48.40"), scheduleImport.summaries().getFirst().getTotalLaborCost());
    assertEquals(new BigDecimal("97.99"), scheduleImport.summaries().getFirst().getTotalCost());

    MaintMileage oilChange =
        scheduleImport.maintMileages().stream()
            .filter(row -> "Change - Engine oil".equals(row.getMaintDesc()))
            .findFirst()
            .orElseThrow();
    assertEquals(50000, oilChange.getMileageDue());
    assertFalse(oilChange.isInspect());
    assertEquals(1, oilChange.getPartLines().size());
    assertNotNull(oilChange.getLaborLine());
    assertEquals(
        "Change - Engine oil",
        oilChange.getPartLines().getFirst().getPartDesc(),
        "part description must come from the part line, not the labor line");
    assertEquals(new BigDecimal("41.44"), oilChange.getPartLines().getFirst().getTotalCost());
    assertEquals(new BigDecimal("15.40"), oilChange.getLaborLine().getTotalCost());

    MaintMileage batteryInspect =
        scheduleImport.maintMileages().stream()
            .filter(row -> "Inspect - Battery".equals(row.getMaintDesc()))
            .findFirst()
            .orElseThrow();
    assertTrue(batteryInspect.isInspect());
    assertTrue(batteryInspect.getPartLines().isEmpty());
    assertNotNull(batteryInspect.getLaborLine());
  }

  @Test
  void shouldNotCreateMaintMileageForPartWithoutMatchingLabor() {
    VehicleType vehicleType =
        new VehicleType("Dodge", "Durango", "R/T 4dr All-wheel Drive Automatic", "2014");
    RepairEstimatesResponse repairEstimates =
        new RepairEstimatesResponse(
            "success",
            new RepairEstimatesResponse.RepairEstimatesData(
                "1C4SDJCT2EC468620",
                2014,
                "Dodge",
                "Durango",
                "R/T 4dr All-wheel Drive Automatic",
                List.of(
                    new RepairEstimatesResponse.MileageInterval(
                        "50000",
                        List.of(
                            new RepairEstimatesResponse.EstimateItem(
                                List.of(
                                    new RepairEstimatesResponse.PartLine(
                                        "Orphan - Part only", new BigDecimal("10.00"), "USD")),
                                List.of(
                                    new RepairEstimatesResponse.LaborLine(
                                        "Change - Engine oil",
                                        new BigDecimal("0.28"),
                                        new BigDecimal("55"),
                                        new BigDecimal("15.40"),
                                        "USD")),
                                List.of()))))));

    MaintenanceScheduleImport scheduleImport =
        mapper.toMaintenanceScheduleImport(vehicleType, repairEstimates);

    assertEquals(1, scheduleImport.maintMileages().size());
    assertEquals("Change - Engine oil", scheduleImport.maintMileages().getFirst().getMaintDesc());
    assertTrue(scheduleImport.maintMileages().getFirst().getPartLines().isEmpty());
  }

  @Test
  void shouldCreateOneMileageSummaryPerIntervalWhenMultipleItemsPresent() {
    VehicleType vehicleType =
        new VehicleType("Dodge", "Durango", "R/T 4dr All-wheel Drive Automatic", "2014");
    List<RepairEstimatesResponse.TotalLine> intervalTotals =
        List.of(
            new RepairEstimatesResponse.TotalLine(
                "Total Parts Cost", new BigDecimal("49.59"), "USD"),
            new RepairEstimatesResponse.TotalLine(
                "Total Labor Cost", new BigDecimal("48.40"), "USD"),
            new RepairEstimatesResponse.TotalLine("Total Cost", new BigDecimal("97.99"), "USD"));
    RepairEstimatesResponse repairEstimates =
        new RepairEstimatesResponse(
            "success",
            new RepairEstimatesResponse.RepairEstimatesData(
                "1C4SDJCT2EC468620",
                2014,
                "Dodge",
                "Durango",
                "R/T 4dr All-wheel Drive Automatic",
                List.of(
                    new RepairEstimatesResponse.MileageInterval(
                        "50000",
                        List.of(
                            new RepairEstimatesResponse.EstimateItem(
                                List.of(
                                    new RepairEstimatesResponse.PartLine(
                                        "Change - Engine oil", new BigDecimal("41.44"), "USD")),
                                List.of(
                                    new RepairEstimatesResponse.LaborLine(
                                        "Change - Engine oil",
                                        new BigDecimal("0.28"),
                                        new BigDecimal("55"),
                                        new BigDecimal("15.40"),
                                        "USD")),
                                intervalTotals),
                            new RepairEstimatesResponse.EstimateItem(
                                List.of(
                                    new RepairEstimatesResponse.PartLine(
                                        "Replace - Oil filter", new BigDecimal("8.15"), "USD")),
                                List.of(
                                    new RepairEstimatesResponse.LaborLine(
                                        "Replace - Oil filter",
                                        new BigDecimal("0.10"),
                                        new BigDecimal("55"),
                                        new BigDecimal("5.50"),
                                        "USD")),
                                intervalTotals))))));

    MaintenanceScheduleImport scheduleImport =
        mapper.toMaintenanceScheduleImport(vehicleType, repairEstimates);

    assertEquals(1, scheduleImport.summaries().size());
    assertEquals(50000, scheduleImport.summaries().getFirst().getMileageDue());
    assertEquals(new BigDecimal("97.99"), scheduleImport.summaries().getFirst().getTotalCost());
  }

  @Test
  void shouldMapRepairTotalCostsFromProviderResponse() {
    VehicleType vehicleType = new VehicleType("Toyota", "4runner", "SRS Prem", "2021");
    RepairCostResponse repairCosts = repairCostResponse();

    MiscMaintCost firstCost = mapper.toMiscMaintCosts(vehicleType, repairCosts).getFirst();

    assertEquals("ABS Module Replacement", firstCost.getMaintTitle());
    assertNull(firstCost.getIndependentAvg());
    assertEquals(1396, firstCost.getDealerAvg());
    assertEquals(1470, firstCost.getDealerHigh());
    assertEquals(1322, firstCost.getDealerLow());
  }

  @Test
  void shouldMapRecallFromProviderResponse() {
    VehicleType vehicleType = new VehicleType("Honda", "Element", "EX", "2008");
    VehicleRecallsResponse recalls = recallResponse();

    Recall recall = mapper.toRecalls(vehicleType, recalls).getFirst();

    assertEquals("19V182000", recall.getNhtsaCampaignNumber());
    assertEquals("19V182000", recall.getCampaignId());
    assertEquals("EA15001", recall.getRecallNo());
    assertEquals("AIR BAGS:FRONTAL:DRIVER SIDE:INFLATOR MODULE", recall.getComponent());
    assertEquals("Honda (American Honda Motor Co.)", recall.getManufacturer());
    assertFalse(recall.isParkIt());
    assertFalse(recall.isParkOutside());
    assertFalse(recall.isOverTheAirUpdate());
    assertEquals("2008", recall.getModelYear());
    assertEquals("Honda", recall.getMake());
    assertEquals("Element", recall.getModel());
    assertEquals(2019, recall.getReportReceivedDate().getYear());
    assertEquals(6, recall.getReportReceivedDate().getMonthValue());
    assertEquals(3, recall.getReportReceivedDate().getDayOfMonth());
  }

  @Test
  void shouldSkipRecallsWithUnparseableDates() {
    VehicleType vehicleType = new VehicleType("Honda", "Element", "EX", "2008");
    VehicleRecallsResponse recalls =
        new VehicleRecallsResponse(
            "success",
            new VehicleRecallsResponse.VehicleRecallsData(
                "5J6YH28728L014142",
                "2008",
                "Honda",
                "Element",
                List.of(
                    new VehicleRecallsResponse.RecallEntry(
                        "19V501000",
                        "EA15001",
                        "not-a-date",
                        "AIR BAGS",
                        "Summary",
                        "Consequences",
                        "Remedy",
                        "Notes",
                        "Honda"),
                    new VehicleRecallsResponse.RecallEntry(
                        "19V182000",
                        "EA15002",
                        "06/03/2019",
                        "AIR BAGS",
                        "Summary",
                        "Consequences",
                        "Remedy",
                        "Notes",
                        "Honda"))));

    List<Recall> mapped = mapper.toRecalls(vehicleType, recalls);

    assertEquals(1, mapped.size());
    assertEquals("19V182000", mapped.getFirst().getNhtsaCampaignNumber());
  }

  @Test
  void shouldParseEuropeanRecallDateFormat() {
    VehicleType vehicleType = new VehicleType("Honda", "Element", "EX", "2008");
    VehicleRecallsResponse recalls =
        new VehicleRecallsResponse(
            "success",
            new VehicleRecallsResponse.VehicleRecallsData(
                "5J6YH28728L014142",
                "2008",
                "Honda",
                "Element",
                List.of(
                    new VehicleRecallsResponse.RecallEntry(
                        "19V501000",
                        "EA15001",
                        "27/06/2019",
                        "AIR BAGS",
                        "Summary",
                        "Consequences",
                        "Remedy",
                        "Notes",
                        "Honda"))));

    Recall recall = mapper.toRecalls(vehicleType, recalls).getFirst();

    assertEquals(2019, recall.getReportReceivedDate().getYear());
    assertEquals(6, recall.getReportReceivedDate().getMonthValue());
    assertEquals(27, recall.getReportReceivedDate().getDayOfMonth());
  }

  @Test
  void shouldDeserializeVehicleRecallsResponseFromProviderJson() {
    String json =
        """
                {
                  "status": "success",
                  "data": {
                    "vin": "5J6YH28728L014142",
                    "year": "2008",
                    "make": "Honda",
                    "model": "Element",
                    "recall": [
                      {
                        "campaign_id": "19V182000",
                        "recall_no": "EA15001",
                        "recall_date": "06/03/2019",
                        "component_affected": "AIR BAGS:FRONTAL:DRIVER SIDE:INFLATOR MODULE",
                        "summary": "Honda is recalling vehicles.",
                        "consequences": "Risk of injury.",
                        "remedy": "Dealers will replace the inflator.",
                        "notes": "Contact NHTSA.",
                        "manufacturer_name": "Honda (American Honda Motor Co.)"
                      }
                    ]
                  }
                }
                """;

    VehicleRecallsResponse response =
        new ObjectMapper().readValue(json, VehicleRecallsResponse.class);

    assertEquals("success", response.status());
    assertEquals("19V182000", response.data().recall().getFirst().campaignId());
    assertEquals("EA15001", response.data().recall().getFirst().recallNo());
    assertEquals("Honda", response.data().make());
  }

  private VinDecodeResponse vinDecodeResponse() {
    return new VinDecodeResponse(
        "3GCUDHEL3NG668790",
        true,
        "3GC",
        "Mexico",
        "3GCUDHELNG",
        "3",
        true,
        "Active",
        "Chevrolet",
        "Silverado 1500",
        "ZR2",
        "4x4 4dr Crew Cab 5.8 ft. SB",
        "Truck",
        "5.3L V8 OHV 16V FFV",
        "4WD",
        "Automatic",
        new VinDecodeResponse.Vehicle(
            "3GCUDHEL3NG668790", 2022, "Chevrolet", "Silverado 1500", "General Motors de Mexico"),
        new VinDecodeResponse.Photos(true, false, true, 12),
        false);
  }

  private OwnerManualResponse ownerManualResponse() {
    return new OwnerManualResponse(
        "success",
        "JTENU5JR6M5962554",
        new OwnerManualResponse.OwnerManualData(
            null,
            "2021",
            "Toyota",
            "4runner",
            "https://vhr.nyc3.cdn.digitaloceanspaces.com/owners-manual/toyota/2021_toyota_4runner_Toyota%202021%204Runner%20Owner%27s%20Manual%20OM35B41U.pdf"));
  }

  private RepairEstimatesResponse repairEstimatesResponse() {
    return new RepairEstimatesResponse(
        "success",
        new RepairEstimatesResponse.RepairEstimatesData(
            "1C4SDJCT2EC468620",
            2014,
            "Dodge",
            "Durango",
            "R/T 4dr All-wheel Drive Automatic",
            List.of(
                new RepairEstimatesResponse.MileageInterval(
                    "50000",
                    List.of(
                        new RepairEstimatesResponse.EstimateItem(
                            List.of(
                                new RepairEstimatesResponse.PartLine(
                                    "Change - Engine oil", new BigDecimal("41.44"), "USD"),
                                new RepairEstimatesResponse.PartLine(
                                    "Replace - Oil filter", new BigDecimal("8.15"), "USD")),
                            List.of(
                                new RepairEstimatesResponse.LaborLine(
                                    "Inspect - Battery",
                                    new BigDecimal("0.05"),
                                    new BigDecimal("55"),
                                    new BigDecimal("2.75"),
                                    "USD"),
                                new RepairEstimatesResponse.LaborLine(
                                    "Change - Engine oil",
                                    new BigDecimal("0.28"),
                                    new BigDecimal("55"),
                                    new BigDecimal("15.40"),
                                    "USD"),
                                new RepairEstimatesResponse.LaborLine(
                                    "Replace - Oil filter",
                                    new BigDecimal("0.10"),
                                    new BigDecimal("55"),
                                    new BigDecimal("5.50"),
                                    "USD"),
                                new RepairEstimatesResponse.LaborLine(
                                    "Rotate - Wheels & tires",
                                    new BigDecimal("0.25"),
                                    new BigDecimal("55"),
                                    new BigDecimal("13.75"),
                                    "USD")),
                            List.of(
                                new RepairEstimatesResponse.TotalLine(
                                    "Total Parts Cost", new BigDecimal("49.59"), "USD"),
                                new RepairEstimatesResponse.TotalLine(
                                    "Total Labor Cost", new BigDecimal("48.40"), "USD"),
                                new RepairEstimatesResponse.TotalLine(
                                    "Total Cost", new BigDecimal("97.99"), "USD"))))))));
  }

  private RepairCostResponse repairCostResponse() {
    return new RepairCostResponse(
        "success",
        new RepairCostResponse.RepairCostData(
            "JTENU5JR6M5962554",
            2021,
            "Toyota",
            "4runner",
            "USD",
            List.of(
                new RepairCostResponse.RepairItem(
                    "ABS Module Replacement",
                    "N/A",
                    new RepairCostResponse.Costs(
                        List.of(),
                        List.of(
                            new RepairCostResponse.CostLine("part", 949, 996, 902),
                            new RepairCostResponse.CostLine("labor", 447, 474, 420),
                            new RepairCostResponse.CostLine("total", 1396, 1470, 1322)))))));
  }

  private VehicleRecallsResponse recallResponse() {
    return new VehicleRecallsResponse(
        "success",
        new VehicleRecallsResponse.VehicleRecallsData(
            "5J6YH28728L014142",
            "2008",
            "Honda",
            "Element",
            List.of(
                new VehicleRecallsResponse.RecallEntry(
                    "19V182000",
                    "EA15001",
                    "06/03/2019",
                    "AIR BAGS:FRONTAL:DRIVER SIDE:INFLATOR MODULE",
                    "Summary",
                    "Consequences",
                    "Remedy",
                    "Notes",
                    "Honda (American Honda Motor Co.)"))));
  }
}
