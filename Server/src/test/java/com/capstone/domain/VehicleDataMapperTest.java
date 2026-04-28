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

        assertEquals("Toyota", vehicleType.getVehicleMake());
        assertEquals("4RUNNER", vehicleType.getVehicleModel());
        assertEquals("SRS Prem", vehicleType.getVehicleTrim());
        assertEquals("2021", vehicleType.getVehicleYear());
        assertEquals("6", vehicleType.getEngineCylinders());
        assertEquals("4WD/4-Wheel Drive/4x4", vehicleType.getDriveType());
        assertEquals("https://vhr.nyc3.cdn.digitaloceanspaces.com/owners-manual/toyota/2021_toyota_4runner_Toyota%202021%204Runner%20Owner%27s%20Manual%20OM35B41U.pdf",
                vehicleType.getOwnersManual());
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
        return new VinDecodeResponse("success", new VinDecodeResponse.VinDecodeData(
            new VinDecodeResponse.Intro("JTENU5JR6M5962554"),
            new VinDecodeResponse.Basic("Toyota", "4RUNNER", "2021", "SRS Prem",
                "Sport Utility Vehicle (SUV)/Multi-Purpose Vehicle (MPV)",
                "Multipurpose Passenger Vehicle (MPV)", "5", "", "5"),
            new VinDecodeResponse.Engine("6", "4.0", "1GR-FE V-Shaped", "4000.0", "V-Shaped", ""),
            new VinDecodeResponse.Manufacturer("Toyota Motor Corporation", "Aichi", "Japan", "Tahara"),
            new VinDecodeResponse.Transmission("Automatic"),
            new VinDecodeResponse.Restraint("Manual Seat Belt"),
            new VinDecodeResponse.Dimensions("Class 2E: 6,001 - 7,000 lb (2,722 - 3,175 kg)"),
            new VinDecodeResponse.Drivetrain("4WD/4-Wheel Drive/4x4"),
            new VinDecodeResponse.Fuel("Gasoline", "")));
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
