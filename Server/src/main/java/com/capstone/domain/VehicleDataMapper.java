package com.capstone.domain;

import com.capstone.integration.*;
import com.capstone.models.MaintCost;
import com.capstone.models.MaintMileage;
import com.capstone.models.Recall;
import com.capstone.models.VehicleType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class VehicleDataMapper {

    private static final DateTimeFormatter RECALL_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public VehicleType toVehicleType(VinDecodeResponse vinDecodeResponse, OwnerManualResponse ownerManualResponse) {
        VinDecodeResponse response = requireResponse(vinDecodeResponse);
        VehicleType vehicleType = new VehicleType(required(make(response)), required(model(response)),
                required(response.trim()), required(year(response)));
        vehicleType.setVehicleStyle(required(response.style()));
        vehicleType.setSourceVin(clean(firstPresent(response.vin(), vehicleVin(response))));
        vehicleType.setOrigin(clean(response.origin()));
        vehicleType.setBody(clean(response.body()));
        vehicleType.setEngineDescription(clean(response.engine()));
        vehicleType.setDriveType(clean(response.drive()));
        vehicleType.setTransmissionStyle(clean(response.transmission()));
        if (ownerManualResponse != null && ownerManualResponse.data() != null) {
            vehicleType.setOwnersManual(clean(ownerManualResponse.data().path()));
        }
        return vehicleType;
    }

    public VehicleIdentity toVehicleIdentity(VinDecodeResponse vinDecodeResponse) {
        VinDecodeResponse response = requireResponse(vinDecodeResponse);
        return new VehicleIdentity(required(year(response)), required(make(response)), required(model(response)),
                required(response.trim()), required(response.style()));
    }

    public List<MaintMileage> toMaintMileages(VehicleType vehicleType,
                                              MaintenanceScheduleResponse maintenanceScheduleResponse) {
        List<MaintMileage> maintMileages = new ArrayList<>();
        if (maintenanceScheduleResponse == null || maintenanceScheduleResponse.data() == null
                || maintenanceScheduleResponse.data().maintenance() == null) {
            return maintMileages;
        }
        for (MaintenanceScheduleResponse.MaintenanceInterval interval : maintenanceScheduleResponse.data()
                .maintenance()) {
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
        if (repairCostResponse == null || repairCostResponse.data() == null
                || repairCostResponse.data().repair() == null) {
            return maintCosts;
        }
        for (RepairCostResponse.RepairItem repair : repairCostResponse.data().repair()) {
            if (repair == null || clean(repair.title()) == null) {
                continue;
            }
            MaintCost maintCost = new MaintCost(vehicleType, clean(repair.title()),
                    nullableDescription(repair.description()));
            RepairCostResponse.CostLine independentTotal = totalCost(
                    repair.costs() != null ? repair.costs().independent() : null);
            RepairCostResponse.CostLine dealerTotal = totalCost(
                    repair.costs() != null ? repair.costs().dealer() : null);
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

    public List<Recall> toRecalls(VehicleType vehicleType, RecallProviderResponse recallResponse) {
        List<Recall> recalls = new ArrayList<>();
        if (recallResponse == null || recallResponse.data() == null) {
            return recalls;
        }
        for (RecallProviderResponse.RecallItem item : recallResponse.data()) {
            if (item == null) {
                continue;
            }
            String nhtsaCampaignNumber = clean(item.nhtsaCampaignNumber());
            if (nhtsaCampaignNumber == null) {
                continue;
            }
            Recall recall = new Recall(vehicleType, nhtsaCampaignNumber, parseRecallDate(item.reportReceivedDate()),
                    requiredText(item.component()), requiredText(item.summary()), requiredText(item.consequence()),
                    requiredText(item.remedy()));
            recall.setParkIt(Boolean.TRUE.equals(item.parkIt()));
            recall.setParkOutside(Boolean.TRUE.equals(item.parkOutside()));
            recall.setOverTheAirUpdate(Boolean.TRUE.equals(item.overTheAirUpdate()));
            recall.setNotes(clean(item.notes()));
            recall.setManufacturer(clean(item.manufacturer()));
            recall.setModelYear(clean(item.modelYear()));
            recall.setMake(clean(item.make()));
            recall.setModel(clean(item.model()));
            recalls.add(recall);
        }
        return recalls;
    }

    private VinDecodeResponse requireResponse(VinDecodeResponse vinDecodeResponse) {
        if (vinDecodeResponse == null) {
            throw new IllegalArgumentException("VIN decode response is missing");
        }
        return vinDecodeResponse;
    }

    private String year(VinDecodeResponse vinDecodeResponse) {
        if (vinDecodeResponse.vehicle() == null || vinDecodeResponse.vehicle().year() == null) {
            return null;
        }
        return vinDecodeResponse.vehicle().year().toString();
    }

    private String make(VinDecodeResponse vinDecodeResponse) {
        return firstPresent(vinDecodeResponse.make(),
                vinDecodeResponse.vehicle() != null ? vinDecodeResponse.vehicle().make() : null);
    }

    private String model(VinDecodeResponse vinDecodeResponse) {
        return firstPresent(vinDecodeResponse.model(),
                vinDecodeResponse.vehicle() != null ? vinDecodeResponse.vehicle().model() : null);
    }

    private String vehicleVin(VinDecodeResponse vinDecodeResponse) {
        return vinDecodeResponse.vehicle() != null ? vinDecodeResponse.vehicle().vin() : null;
    }

    private String firstPresent(String primary, String fallback) {
        String cleanPrimary = clean(primary);
        return cleanPrimary != null ? cleanPrimary : clean(fallback);
    }

    private RepairCostResponse.CostLine totalCost(List<RepairCostResponse.CostLine> costLines) {
        if (costLines == null) {
            return null;
        }
        return costLines.stream()
                .filter(costLine -> costLine != null && "total".equalsIgnoreCase(clean(costLine.name()))).findFirst()
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

    public record VehicleIdentity(String year, String make, String model, String trim, String style) {
    }
}
