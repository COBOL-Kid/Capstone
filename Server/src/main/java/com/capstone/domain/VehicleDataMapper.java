package com.capstone.domain;

import com.capstone.integration.*;
import com.capstone.models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class VehicleDataMapper {

  private static final Logger log = LoggerFactory.getLogger(VehicleDataMapper.class);

  private static final DateTimeFormatter RECALL_DATE_US = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter RECALL_DATE_EU = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private static final String TOTAL_PARTS_COST = "Total Parts Cost";
  private static final String TOTAL_LABOR_COST = "Total Labor Cost";
  private static final String TOTAL_COST = "Total Cost";
  private static final Pattern INSPECT_TYPE_PATTERN = Pattern.compile("(?i)^inspect(\\s|-|$)");

  public VehicleType toVehicleType(
      VinDecodeResponse vinDecodeResponse, OwnerManualResponse ownerManualResponse) {
    VinDecodeResponse response = requireResponse(vinDecodeResponse);
    VehicleType vehicleType =
        new VehicleType(
            required(make(response)),
            required(model(response)),
            required(response.trim()),
            required(year(response)));
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
    return new VehicleIdentity(
        required(year(response)),
        required(make(response)),
        required(model(response)),
        required(response.trim()),
        required(response.style()));
  }

  public MaintenanceScheduleImport toMaintenanceScheduleImport(
      VehicleType vehicleType, RepairEstimatesResponse repairEstimatesResponse) {
    List<MaintMileage> maintMileages = new ArrayList<>();
    List<MaintMileageSummary> summaries = new ArrayList<>();
    if (repairEstimatesResponse == null
        || repairEstimatesResponse.data() == null
        || repairEstimatesResponse.data().data() == null) {
      return new MaintenanceScheduleImport(maintMileages, summaries);
    }
    for (RepairEstimatesResponse.MileageInterval interval : repairEstimatesResponse.data().data()) {
      if (interval == null || interval.mileage() == null || interval.items() == null) {
        continue;
      }
      int mileageDue = parseMileage(interval.mileage());
      for (RepairEstimatesResponse.EstimateItem item : interval.items()) {
        if (item == null) {
          continue;
        }
        maintMileages.addAll(toMaintMileagesForItem(vehicleType, mileageDue, item));
      }
      MaintMileageSummary summary =
          toIntervalMileageSummary(vehicleType, mileageDue, interval.items());
      if (summary != null) {
        summaries.add(summary);
      }
    }
    return new MaintenanceScheduleImport(maintMileages, summaries);
  }

  public List<MiscMaintCost> toMiscMaintCosts(
      VehicleType vehicleType, RepairCostResponse repairCostResponse) {
    List<MiscMaintCost> miscMaintCosts = new ArrayList<>();
    if (repairCostResponse == null
        || repairCostResponse.data() == null
        || repairCostResponse.data().repair() == null) {
      return miscMaintCosts;
    }
    for (RepairCostResponse.RepairItem repair : repairCostResponse.data().repair()) {
      if (repair == null || clean(repair.title()) == null) {
        continue;
      }
      MiscMaintCost miscMaintCost =
          new MiscMaintCost(
              vehicleType, clean(repair.title()), nullableDescription(repair.description()));
      RepairCostResponse.CostLine independentTotal =
          totalCost(repair.costs() != null ? repair.costs().independent() : null);
      RepairCostResponse.CostLine dealerTotal =
          totalCost(repair.costs() != null ? repair.costs().dealer() : null);
      if (independentTotal != null) {
        miscMaintCost.setIndependentAvg(independentTotal.average());
        miscMaintCost.setIndependentHigh(independentTotal.high());
        miscMaintCost.setIndependentLow(independentTotal.low());
      }
      if (dealerTotal != null) {
        miscMaintCost.setDealerAvg(dealerTotal.average());
        miscMaintCost.setDealerHigh(dealerTotal.high());
        miscMaintCost.setDealerLow(dealerTotal.low());
      }
      miscMaintCosts.add(miscMaintCost);
    }
    return miscMaintCosts;
  }

  public Optional<VehicleWarranty> toVehicleWarranty(
      VehicleWarrantyResponse warrantyResponse, String year, String make, String model) {
    if (warrantyResponse == null
        || warrantyResponse.data() == null
        || warrantyResponse.data().warranty() == null
        || warrantyResponse.data().warranty().isEmpty()) {
      return Optional.empty();
    }
    String canonicalYear = required(clean(year));
    String canonicalMake = required(clean(make));
    String canonicalModel = required(clean(model));
    VehicleWarranty vehicleWarranty =
        new VehicleWarranty(canonicalYear, canonicalMake, canonicalModel);
    for (Map.Entry<String, String> entry : warrantyResponse.data().warranty().entrySet()) {
      String coverageName = clean(entry.getKey());
      String coverageValue = clean(entry.getValue());
      if (coverageName == null || coverageValue == null) {
        continue;
      }
      vehicleWarranty.addCoverage(coverageName, coverageValue);
    }
    if (vehicleWarranty.getCoverages().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(vehicleWarranty);
  }

  public List<Recall> toRecalls(VehicleType vehicleType, VehicleRecallsResponse recallResponse) {
    List<Recall> recalls = new ArrayList<>();
    if (recallResponse == null
        || recallResponse.data() == null
        || recallResponse.data().recall() == null) {
      return recalls;
    }
    VehicleRecallsResponse.VehicleRecallsData wrapper = recallResponse.data();
    for (VehicleRecallsResponse.RecallEntry item : wrapper.recall()) {
      if (item == null) {
        continue;
      }
      String nhtsaCampaignNumber = clean(item.campaignId());
      if (nhtsaCampaignNumber == null) {
        continue;
      }
      Optional<LocalDate> recallDate = parseRecallDate(item.recallDate());
      if (recallDate.isEmpty()) {
        log.warn(
            "Skipping recall {} for vehicle type {}: unparseable date '{}'",
            nhtsaCampaignNumber,
            vehicleType.getVehicleTypeId(),
            item.recallDate());
        continue;
      }
      Recall recall =
          new Recall(
              vehicleType,
              nhtsaCampaignNumber,
              recallDate.get(),
              requiredText(item.componentAffected()),
              requiredText(item.summary()),
              requiredText(item.consequences()),
              requiredText(item.remedy()));
      recall.setRecallNo(clean(item.recallNo()));
      recall.setParkIt(false);
      recall.setParkOutside(false);
      recall.setOverTheAirUpdate(false);
      recall.setNotes(clean(item.notes()));
      recall.setManufacturer(clean(item.manufacturerName()));
      recall.setModelYear(clean(wrapper.year()));
      recall.setMake(clean(wrapper.make()));
      recall.setModel(clean(wrapper.model()));
      recalls.add(recall);
    }
    return recalls;
  }

  private List<MaintMileage> toMaintMileagesForItem(
      VehicleType vehicleType, int mileageDue, RepairEstimatesResponse.EstimateItem item) {
    List<MaintMileage> rows = new ArrayList<>();
    if (item.labor() == null) {
      return rows;
    }
    for (RepairEstimatesResponse.LaborLine laborLine : item.labor()) {
      if (laborLine == null || clean(laborLine.type()) == null || laborLine.totalCost() == null) {
        continue;
      }
      String type = clean(laborLine.type());
      MaintMileage maintMileage = new MaintMileage(vehicleType, mileageDue, type);
      maintMileage.setInspect(isInspectType(type));
      maintMileage.setLaborLine(
          new MaintLaborLine(
              maintMileage,
              laborLine.timeRequiredHours(),
              laborLine.hourlyRate(),
              laborLine.totalCost(),
              laborLine.currency()));
      if (item.parts() != null) {
        for (RepairEstimatesResponse.PartLine partLine : item.parts()) {
          if (partLine == null || partLine.totalCost() == null) {
            continue;
          }
          String partType = clean(partLine.type());
          if (partType == null || !type.equals(partType)) {
            continue;
          }
          maintMileage
              .getPartLines()
              .add(
                  new MaintPartLine(
                      maintMileage, partType, partLine.totalCost(), partLine.currency()));
        }
      }
      rows.add(maintMileage);
    }
    return rows;
  }

  private boolean isInspectType(String type) {
    return type != null && INSPECT_TYPE_PATTERN.matcher(type).find();
  }

  private MaintMileageSummary toIntervalMileageSummary(
      VehicleType vehicleType, int mileageDue, List<RepairEstimatesResponse.EstimateItem> items) {
    if (items == null) {
      return null;
    }
    for (RepairEstimatesResponse.EstimateItem item : items) {
      if (item == null) {
        continue;
      }
      MaintMileageSummary summary = toMileageSummary(vehicleType, mileageDue, item.total());
      if (summary != null) {
        return summary;
      }
    }
    return null;
  }

  private MaintMileageSummary toMileageSummary(
      VehicleType vehicleType, int mileageDue, List<RepairEstimatesResponse.TotalLine> totals) {
    if (totals == null || totals.isEmpty()) {
      return null;
    }
    BigDecimal partsCost = null;
    BigDecimal laborCost = null;
    BigDecimal totalCost = null;
    String currency = "USD";
    for (RepairEstimatesResponse.TotalLine line : totals) {
      if (line == null || line.type() == null || line.totalCost() == null) {
        continue;
      }
      if (line.currency() != null && !line.currency().isBlank()) {
        currency = line.currency();
      }
      switch (line.type()) {
        case TOTAL_PARTS_COST -> partsCost = line.totalCost();
        case TOTAL_LABOR_COST -> laborCost = line.totalCost();
        case TOTAL_COST -> totalCost = line.totalCost();
        default -> {}
      }
    }
    if (partsCost == null || laborCost == null || totalCost == null) {
      return null;
    }
    return new MaintMileageSummary(
        vehicleType, mileageDue, partsCost, laborCost, totalCost, currency);
  }

  private int parseMileage(String mileage) {
    String cleanMileage = clean(mileage);
    if (cleanMileage == null) {
      throw new IllegalArgumentException("Mileage is required");
    }
    return Integer.parseInt(cleanMileage);
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
    return firstPresent(
        vinDecodeResponse.make(),
        vinDecodeResponse.vehicle() != null ? vinDecodeResponse.vehicle().make() : null);
  }

  private String model(VinDecodeResponse vinDecodeResponse) {
    return firstPresent(
        vinDecodeResponse.model(),
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
        .filter(costLine -> costLine != null && "total".equalsIgnoreCase(clean(costLine.name())))
        .findFirst()
        .orElse(null);
  }

  private Optional<LocalDate> parseRecallDate(String recallDate) {
    String cleanDate = clean(recallDate);
    if (cleanDate == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(cleanDate, RECALL_DATE_US));
    } catch (Exception ignored) {
      try {
        return Optional.of(LocalDate.parse(cleanDate, RECALL_DATE_EU));
      } catch (Exception ex) {
        return Optional.empty();
      }
    }
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

  public record VehicleIdentity(
      String year, String make, String model, String trim, String style) {}
}
