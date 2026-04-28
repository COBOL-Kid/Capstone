package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.capstone.integration.vehicledatabases.MaintenanceScheduleResponse;
import com.capstone.integration.vehicledatabases.OwnerManualResponse;
import com.capstone.integration.vehicledatabases.RecallResponse;
import com.capstone.integration.vehicledatabases.RepairCostResponse;
import com.capstone.integration.vehicledatabases.VinDecodeResponse;
import com.capstone.models.MaintCost;
import com.capstone.models.Recall;
import com.capstone.models.VehicleType;
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
        assertEquals("https://vhr.nyc3.cdn.digitaloceanspaces.com/owners-manual/toyota/2021_toyota_4runner_Toyota%202021%204Runner%20Owner%27s%20Manual%20OM35B41U.pdf",
                vehicleType.getOwnersManual());
    }

    @Test
    void shouldDeserializeVinDecodeResponseFromProviderJson() throws Exception {
        String json = """
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
    void shouldMapMaintenanceScheduleRowsFromProviderResponse() {
        VehicleType vehicleType = new VehicleType("Toyota", "4runner", "SR5 Premium 4dr 4x4 Automatic", "2021");
        MaintenanceScheduleResponse maintenance = maintenanceScheduleResponse();

        var rows = mapper.toMaintMileages(vehicleType, maintenance);

        assertEquals(3, rows.size());
        assertEquals(5000, rows.get(0).getMileageDue());
        assertEquals("Check Driver's Floor Mat", rows.get(0).getMaintDesc());
    }

    @Test
    void shouldMapRepairTotalCostsFromProviderResponse() {
        VehicleType vehicleType = new VehicleType("Toyota", "4runner", "SRS Prem", "2021");
        RepairCostResponse repairCosts = repairCostResponse();

        MaintCost firstCost = mapper.toMaintCosts(vehicleType, repairCosts).get(0);

        assertEquals("ABS Module Replacement", firstCost.getMaintTitle());
        assertNull(firstCost.getIndependentAvg());
        assertEquals(1396, firstCost.getDealerAvg());
        assertEquals(1470, firstCost.getDealerHigh());
        assertEquals(1322, firstCost.getDealerLow());
    }

    @Test
    void shouldMapRecallFromProviderResponse() {
        VehicleType vehicleType = new VehicleType("Toyota", "4runner", "SRS Prem", "2021");
        RecallResponse recalls = recallResponse();

        Recall recall = mapper.toRecalls(vehicleType, recalls).get(0);

        assertEquals("25V239000", recall.getNhtsaCampaignNumber());
        assertEquals("25V239000", recall.getCampaignId());
        assertEquals("POWER TRAIN:AUTOMATIC TRANSMISSION:CONTROL MODULE:SOFTWARE", recall.getComponent());
        assertEquals("Ford Motor Company", recall.getManufacturer());
        assertFalse(recall.isParkIt());
        assertFalse(recall.isParkOutside());
        assertFalse(recall.isOverTheAirUpdate());
        assertEquals("2025", recall.getModelYear());
        assertEquals("FORD", recall.getMake());
        assertEquals("EXPLORER", recall.getModel());
        assertEquals(2025, recall.getReportReceivedDate().getYear());
        assertEquals(11, recall.getReportReceivedDate().getMonthValue());
        assertEquals(4, recall.getReportReceivedDate().getDayOfMonth());
    }

    @Test
    void shouldDeserializeRecallResponseFromProviderJson() throws Exception {
        String json = """
                {
                  "data": [
                    {
                      "manufacturer": "Ford Motor Company",
                      "nhtsaCampaignNumber": "25V239000",
                      "parkIt": false,
                      "parkOutSide": false,
                      "overTheAirUpdate": false,
                      "reportReceivedDate": "11/04/2025",
                      "component": "POWER TRAIN:AUTOMATIC TRANSMISSION:CONTROL MODULE:SOFTWARE",
                      "summary": "Ford Motor Company (Ford) is recalling certain 2025 Explorer vehicles.",
                      "consequence": "A damaged park system can increase the risk of a crash.",
                      "remedy": "Dealers will update the powertrain control module software, free of charge.",
                      "notes": "Owners may also contact NHTSA.",
                      "modelYear": "2025",
                      "make": "FORD",
                      "model": "EXPLORER"
                    }
                  ]
                }
                """;

        RecallResponse response = new ObjectMapper().readValue(json, RecallResponse.class);

        assertEquals(1, response.data().size());
        assertEquals("25V239000", response.data().get(0).nhtsaCampaignNumber());
        assertEquals(Boolean.FALSE, response.data().get(0).parkOutside());
        assertEquals("POWER TRAIN:AUTOMATIC TRANSMISSION:CONTROL MODULE:SOFTWARE", response.data().get(0).component());
        assertEquals("Ford Motor Company", response.data().get(0).manufacturer());
    }

    private VinDecodeResponse vinDecodeResponse() {
        return new VinDecodeResponse("3GCUDHEL3NG668790", true, "3GC", "Mexico", "3GCUDHELNG", "3", true,
                "Active", "Chevrolet", "Silverado 1500", "ZR2", "4x4 4dr Crew Cab 5.8 ft. SB", "Truck",
                "5.3L V8 OHV 16V FFV", "4WD", "Automatic",
                new VinDecodeResponse.Vehicle("3GCUDHEL3NG668790", 2022, "Chevrolet", "Silverado 1500",
                        "General Motors de Mexico"),
                new VinDecodeResponse.Photos(true, false, true, 12), false);
    }

    private OwnerManualResponse ownerManualResponse() {
        return new OwnerManualResponse("success", new OwnerManualResponse.OwnerManualData("JTENU5JR6M5962554", "2021",
                "Toyota", "4runner",
                "https://vhr.nyc3.cdn.digitaloceanspaces.com/owners-manual/toyota/2021_toyota_4runner_Toyota%202021%204Runner%20Owner%27s%20Manual%20OM35B41U.pdf"));
    }

    private MaintenanceScheduleResponse maintenanceScheduleResponse() {
        return new MaintenanceScheduleResponse("success", new MaintenanceScheduleResponse.MaintenanceScheduleData(
                "JTENU5JR6M5962554", 2021, "Toyota", "4runner", "SR5 Premium 4dr 4x4 Automatic",
                List.of(new MaintenanceScheduleResponse.MaintenanceInterval(
                        new MaintenanceScheduleResponse.Mileage(5000, 8000),
                        List.of("Check Driver's Floor Mat", "Inspect Brake System", "Replace Engine Oil & Filter")))));
    }

    private RepairCostResponse repairCostResponse() {
        return new RepairCostResponse("success", new RepairCostResponse.RepairCostData("JTENU5JR6M5962554", 2021,
                "Toyota", "4runner", "USD", List.of(new RepairCostResponse.RepairItem("ABS Module Replacement", "N/A",
                        new RepairCostResponse.Costs(List.of(), List.of(
                                new RepairCostResponse.CostLine("part", 949, 996, 902),
                                new RepairCostResponse.CostLine("labor", 447, 474, 420),
                                new RepairCostResponse.CostLine("total", 1396, 1470, 1322)))))));
    }

    private RecallResponse recallResponse() {
        return new RecallResponse(List.of(new RecallResponse.RecallItem("Ford Motor Company", "25V239000", false,
                false, false, "11/04/2025", "POWER TRAIN:AUTOMATIC TRANSMISSION:CONTROL MODULE:SOFTWARE", "Summary",
                "Consequence", "Remedy", "Notes", "2025", "FORD", "EXPLORER")));
    }
}
