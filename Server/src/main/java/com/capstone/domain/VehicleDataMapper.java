package com.capstone.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.capstone.integration.vehicledatabases.MaintenanceScheduleResponse;
import com.capstone.integration.vehicledatabases.OwnerManualResponse;
import com.capstone.integration.vehicledatabases.RecallResponse;
import com.capstone.integration.vehicledatabases.RepairCostResponse;
import com.capstone.integration.vehicledatabases.VinDecodeResponse;
import com.capstone.models.MaintCost;
import com.capstone.models.MaintMileage;
import com.capstone.models.Recall;
import com.capstone.models.VehicleType;

@Component
public class VehicleDataMapper {

    private static final DateTimeFormatter RECALL_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public VehicleType toVehicleType(VinDecodeResponse vinDecodeResponse, OwnerManualResponse ownerManualResponse) {
        VinDecodeResponse.VinDecodeData data = requireData(vinDecodeResponse);
        VinDecodeResponse.Basic basic = data.basic();
        if (basic == null) {
            throw new IllegalArgumentException("VIN decode response is missing basic vehicle details");
        }

        VehicleType vehicleType = new VehicleType(required(basic.make()), required(basic.model()), required(basic.trim()),
                required(basic.year()));
        vehicleType.setDoors(clean(basic.doors()));
        vehicleType.setVehicleSize(clean(basic.vehicleSize()));
        vehicleType.setSeatingCapacity(clean(basic.seatingCapacity()));

        if (data.engine() != null) {
            vehicleType.setEngineCylinders(clean(data.engine().cylinders()));
            vehicleType.setEngineSize(clean(data.engine().engineSize()));
            vehicleType.setEngineDescription(clean(data.engine().engineDescription()));
            vehicleType.setEngineCapacity(clean(data.engine().engineCapacity()));
            vehicleType.setEngineConfiguration(clean(data.engine().engineConfiguration()));
            vehicleType.setElectrificationLevel(clean(data.engine().electrificationLevel()));
        }
        if (data.manufacturer() != null) {
            vehicleType.setManufacturerName(clean(data.manufacturer().manufacturer()));
            vehicleType.setManufacturerRegion(clean(data.manufacturer().region()));
            vehicleType.setManufacturerCountry(clean(data.manufacturer().country()));
            vehicleType.setPlantCity(clean(data.manufacturer().plantCity()));
        }
        if (data.transmission() != null) {
            vehicleType.setTransmissionStyle(clean(data.transmission().transmissionStyle()));
        }
        if (data.restraint() != null) {
            vehicleType.setRestraintDetails(clean(data.restraint().others()));
        }
        if (data.dimensions() != null) {
            vehicleType.setGvwr(clean(data.dimensions().gvwr()));
        }
        if (data.drivetrain() != null) {
            vehicleType.setDriveType(clean(data.drivetrain().driveType()));
        }
        if (data.fuel() != null) {
            vehicleType.setFuelType(clean(data.fuel().fuelType()));
            vehicleType.setSecondaryFuelType(clean(data.fuel().secondaryFuelType()));
        }
        if (ownerManualResponse != null && ownerManualResponse.data() != null) {
            vehicleType.setOwnersManual(clean(ownerManualResponse.data().path()));
        }
        return vehicleType;
    }

    public VehicleIdentity toVehicleIdentity(VinDecodeResponse vinDecodeResponse) {
        VinDecodeResponse.Basic basic = requireData(vinDecodeResponse).basic();
        if (basic == null) {
            throw new IllegalArgumentException("VIN decode response is missing basic vehicle details");
        }
        return new VehicleIdentity(required(basic.year()), required(basic.make()), required(basic.model()),
                required(basic.trim()));
    }

    public List<MaintMileage> toMaintMileages(VehicleType vehicleType,
            MaintenanceScheduleResponse maintenanceScheduleResponse) {
        List<MaintMileage> maintMileages = new ArrayList<>();
        if (maintenanceScheduleResponse == null || maintenanceScheduleResponse.data() == null
                || maintenanceScheduleResponse.data().maintenance() == null) {
            return maintMileages;
        }
        for (MaintenanceScheduleResponse.MaintenanceInterval interval : maintenanceScheduleResponse.data().maintenance()) {
            if (interval == null || interval.mileage() == null || interval.mileage().miles() == null
                    || interval.serviceItems() == null) {
                continue;
            }
            for (String serviceItem : interval.serviceItems()) {
                String description = clean(serviceItem);
                if (description != null) {
                    maintMileages.add(new MaintMileage(vehicleType, interval.mileage().miles(), description));
                }
            }
        }
        return maintMileages;
    }

    public List<MaintCost> toMaintCosts(VehicleType vehicleType, RepairCostResponse repairCostResponse) {
        List<MaintCost> maintCosts = new ArrayList<>();
        if (repairCostResponse == null || repairCostResponse.data() == null || repairCostResponse.data().repair() == null) {
            return maintCosts;
        }
        for (RepairCostResponse.RepairItem repair : repairCostResponse.data().repair()) {
            if (repair == null || clean(repair.title()) == null) {
                continue;
            }
            MaintCost maintCost = new MaintCost(vehicleType, clean(repair.title()), nullableDescription(repair.description()));
            RepairCostResponse.CostLine independentTotal = totalCost(repair.costs() != null ? repair.costs().independent() : null);
            RepairCostResponse.CostLine dealerTotal = totalCost(repair.costs() != null ? repair.costs().dealer() : null);
            if (independentTotal != null) {
                maintCost.setIndependentAvg(independentTotal.average());
                maintCost.setIndependentHigh(independentTotal.high());
                maintCost.setIndependentLow(independentTotal.low());
            }
            if (dealerTotal != null) {
                maintCost.setDealerAvg(dealerTotal.average());
                maintCost.setDealerHigh(dealerTotal.high());
                maintCost.setDealerLow(dealerTotal.low());
            }
            maintCosts.add(maintCost);
        }
        return maintCosts;
    }

    public List<Recall> toRecalls(VehicleType vehicleType, RecallResponse recallResponse) {
        List<Recall> recalls = new ArrayList<>();
        if (recallResponse == null || recallResponse.data() == null || recallResponse.data().recall() == null) {
            return recalls;
        }
        for (RecallResponse.RecallItem item : recallResponse.data().recall()) {
            if (item == null || clean(item.campaignId()) == null) {
                continue;
            }
            Recall recall = new Recall(vehicleType, clean(item.campaignId()), parseRecallDate(item.recallDate()),
                    requiredText(item.componentAffected()), requiredText(item.summary()), requiredText(item.consequences()),
                    requiredText(item.remedy()));
            recall.setRecallNo(clean(item.recallNo()));
            recall.setNotes(clean(item.notes()));
            recall.setManufacturerName(clean(item.manufacturerName()));
            recalls.add(recall);
        }
        return recalls;
    }

    private VinDecodeResponse.VinDecodeData requireData(VinDecodeResponse vinDecodeResponse) {
        if (vinDecodeResponse == null || vinDecodeResponse.data() == null) {
            throw new IllegalArgumentException("VIN decode response is missing data");
        }
        return vinDecodeResponse.data();
    }

    private RepairCostResponse.CostLine totalCost(List<RepairCostResponse.CostLine> costLines) {
        if (costLines == null) {
            return null;
        }
        return costLines.stream()
                .filter(costLine -> costLine != null && "total".equalsIgnoreCase(clean(costLine.name())))
                .findFirst()
                .orElse(null);
    }

    private LocalDate parseRecallDate(String recallDate) {
        String cleanDate = clean(recallDate);
        if (cleanDate == null) {
            throw new IllegalArgumentException("Recall date is required");
        }
        return LocalDate.parse(cleanDate, RECALL_DATE_FORMAT);
    }

    private String required(String value) {
        String cleanValue = clean(value);
        if (cleanValue == null) {
            return "UNKNOWN";
        }
        return cleanValue;
    }

    private String requiredText(String value) {
        String cleanValue = clean(value);
        return cleanValue != null ? cleanValue : "Unknown";
    }

    private String nullableDescription(String value) {
        String cleanValue = clean(value);
        if (cleanValue == null || "N/A".equalsIgnoreCase(cleanValue)) {
            return null;
        }
        return cleanValue;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record VehicleIdentity(String year, String make, String model, String trim) {
    }
}