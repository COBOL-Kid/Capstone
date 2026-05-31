package com.capstone.data.read;

import com.capstone.data.read.VehicleReadDao.CompletedMaintenanceRow;
import com.capstone.data.read.VehicleReadDao.CompletedRecallRow;
import com.capstone.data.read.VehicleReadDao.MaintLaborLineRow;
import com.capstone.data.read.VehicleReadDao.MaintPartLineRow;
import com.capstone.data.read.VehicleReadDao.MaintSummaryRow;
import com.capstone.data.read.VehicleReadDao.MiscMaintCostRow;
import com.capstone.data.read.VehicleReadDao.UncompletedRecallRow;
import com.capstone.data.read.VehicleReadDao.UpcomingMaintRootRow;
import com.capstone.domain.VinNormalizer;
import com.capstone.domain.WarrantyStatusCalculator;
import com.capstone.models.dto.CompletedMaintenanceResponse;
import com.capstone.models.dto.CompletedRecallResponse;
import com.capstone.models.dto.LaborCostResponse;
import com.capstone.models.dto.MileageCostSummaryResponse;
import com.capstone.models.dto.MiscMaintenanceCostResponse;
import com.capstone.models.dto.PartCostResponse;
import com.capstone.models.dto.RecallResponse;
import com.capstone.models.dto.UpcomingMaintenanceIntervalResponse;
import com.capstone.models.dto.UpcomingMaintenanceItemResponse;
import com.capstone.models.dto.UserVehicleResponse;
import com.capstone.models.dto.VehicleDashboardResponse;
import com.capstone.models.dto.VehicleDetailResponse;
import com.capstone.models.dto.VehicleWarrantyResponse;
import com.capstone.models.dto.WarrantyCoverageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleReadService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<LinkedHashMap<String, String>> COVERAGES_MAP =
      new TypeReference<>() {};

  private final VehicleReadDao vehicleReadDao;

  public VehicleReadService(VehicleReadDao vehicleReadDao) {
    this.vehicleReadDao = vehicleReadDao;
  }

  @Transactional(readOnly = true)
  public List<UserVehicleResponse> findVehiclesForUser(long userId) {
    return vehicleReadDao.findUserVehicles(userId).stream().map(this::toListResponse).toList();
  }

  @Transactional(readOnly = true)
  public Optional<VehicleDetailResponse> findVehicleDetail(long userId, String vin) {
    String normalizedVin = VinNormalizer.normalize(vin);
    return vehicleReadDao.findUserVinDetail(userId, normalizedVin).map(this::toDetailResponse);
  }

  @Transactional(readOnly = true)
  public Optional<VehicleDashboardResponse> findDashboard(long userId, String vin) {
    String normalizedVin = VinNormalizer.normalize(vin);
    Optional<UserVinDetailRow> detailRow = vehicleReadDao.findUserVinDetail(userId, normalizedVin);
    if (detailRow.isEmpty()) {
      return Optional.empty();
    }

    UserVinDetailRow row = detailRow.get();
    int threshold = row.currentMileage() + 10_000;
    long vehicleTypeId = row.vehicleTypeId();

    VehicleDetailResponse detail = toDetailResponse(row);
    List<CompletedMaintenanceResponse> completedMaintenance =
        vehicleReadDao.findCompletedMaintenance(userId, normalizedVin).stream()
            .map(r -> toCompletedMaintenanceResponse(normalizedVin, r))
            .toList();
    List<UpcomingMaintenanceIntervalResponse> upcomingMaintenance =
        buildUpcomingMaintenance(userId, normalizedVin, vehicleTypeId, threshold);
    List<RecallResponse> uncompletedRecalls =
        vehicleReadDao.findUncompletedRecalls(userId, normalizedVin).stream()
            .map(r -> toRecallResponse(normalizedVin, r))
            .toList();
    List<CompletedRecallResponse> completedRecalls =
        vehicleReadDao.findCompletedRecalls(userId, normalizedVin).stream()
            .map(r -> toCompletedRecallResponse(normalizedVin, r))
            .toList();
    List<MiscMaintenanceCostResponse> miscMaintenanceCosts =
        vehicleReadDao.findMiscCosts(vehicleTypeId).stream().map(this::toMiscCostResponse).toList();
    VehicleWarrantyResponse vehicleWarranty = findVehicleWarranty(row);

    return Optional.of(
        new VehicleDashboardResponse(
            detail,
            upcomingMaintenance,
            completedMaintenance,
            uncompletedRecalls,
            completedRecalls,
            miscMaintenanceCosts,
            vehicleWarranty));
  }

  private VehicleWarrantyResponse findVehicleWarranty(UserVinDetailRow row) {
    return vehicleReadDao
        .findVehicleWarranty(row.vehicleYear(), row.vehicleMake(), row.vehicleModel())
        .map(
            warrantyRow ->
                new VehicleWarrantyResponse(
                    warrantyRow.vehicleYear(),
                    warrantyRow.vehicleMake(),
                    warrantyRow.vehicleModel(),
                    parseCoverages(
                        warrantyRow.coverages(), row.vehicleYear(), row.currentMileage())))
        .orElse(null);
  }

  private List<WarrantyCoverageResponse> parseCoverages(
      String coveragesJson, String vehicleYear, int currentMileage) {
    if (coveragesJson == null || coveragesJson.isBlank()) {
      return List.of();
    }
    int modelYear = parseVehicleYear(vehicleYear);
    LocalDate today = LocalDate.now();
    try {
      Map<String, String> coverages = OBJECT_MAPPER.readValue(coveragesJson, COVERAGES_MAP);
      return coverages.entrySet().stream()
          .map(
              entry ->
                  WarrantyStatusCalculator.compute(
                      entry.getKey(), entry.getValue(), modelYear, currentMileage, today))
          .toList();
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to deserialize warranty coverages", ex);
    }
  }

  private static int parseVehicleYear(String vehicleYear) {
    try {
      return Integer.parseInt(vehicleYear.trim());
    } catch (NumberFormatException ex) {
      throw new IllegalStateException("Invalid vehicle year: " + vehicleYear, ex);
    }
  }

  private List<UpcomingMaintenanceIntervalResponse> buildUpcomingMaintenance(
      long userId, String vin, long vehicleTypeId, int threshold) {
    List<UpcomingMaintRootRow> roots =
        vehicleReadDao.findUpcomingMaintenanceRoots(vehicleTypeId, threshold, userId, vin);
    if (roots.isEmpty()) {
      return List.of();
    }

    List<Long> maintMileageIds = roots.stream().map(UpcomingMaintRootRow::maintMileageId).toList();
    Map<Long, MaintLaborLineRow> laborByMaintId =
        vehicleReadDao.findLaborLines(maintMileageIds).stream()
            .collect(
                Collectors.toMap(MaintLaborLineRow::maintMileageId, line -> line, (a, b) -> a));
    Map<Long, List<MaintPartLineRow>> partsByMaintId =
        vehicleReadDao.findPartLines(maintMileageIds).stream()
            .collect(Collectors.groupingBy(MaintPartLineRow::maintMileageId));

    List<Integer> mileageDues =
        roots.stream().map(UpcomingMaintRootRow::mileageDue).distinct().toList();
    Map<Integer, MaintSummaryRow> summariesByMileage =
        vehicleReadDao.findMaintSummaries(vehicleTypeId, mileageDues).stream()
            .collect(Collectors.toMap(MaintSummaryRow::mileageDue, s -> s, (a, b) -> a));

    Map<Integer, List<UpcomingMaintenanceItemResponse>> itemsByMileage = new LinkedHashMap<>();
    for (UpcomingMaintRootRow root : roots) {
      itemsByMileage
          .computeIfAbsent(root.mileageDue(), _ -> new ArrayList<>())
          .add(toUpcomingItem(root, laborByMaintId.get(root.maintMileageId()), partsByMaintId));
    }

    return itemsByMileage.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(
            entry -> {
              MaintSummaryRow summary = summariesByMileage.get(entry.getKey());
              return new UpcomingMaintenanceIntervalResponse(
                  entry.getKey(), toSummaryResponse(summary), entry.getValue());
            })
        .toList();
  }

  private UpcomingMaintenanceItemResponse toUpcomingItem(
      UpcomingMaintRootRow root,
      MaintLaborLineRow laborLine,
      Map<Long, List<MaintPartLineRow>> partsByMaintId) {
    LaborCostResponse labor = null;
    if (laborLine != null) {
      labor =
          new LaborCostResponse(
              laborLine.timeRequiredHours(),
              laborLine.hourlyRate(),
              laborLine.totalCost(),
              laborLine.currency());
    }
    List<PartCostResponse> parts =
        partsByMaintId.getOrDefault(root.maintMileageId(), List.of()).stream()
            .map(
                partLine ->
                    new PartCostResponse(
                        partLine.partDesc(), partLine.totalCost(), partLine.currency()))
            .toList();
    return new UpcomingMaintenanceItemResponse(
        root.maintMileageId(), root.maintDesc(), root.inspect(), labor, parts);
  }

  private MileageCostSummaryResponse toSummaryResponse(MaintSummaryRow summary) {
    if (summary == null) {
      return null;
    }
    return new MileageCostSummaryResponse(
        summary.totalPartsCost(),
        summary.totalLaborCost(),
        summary.totalCost(),
        summary.currency());
  }

  private UserVehicleResponse toListResponse(UserVinListRow row) {
    return new UserVehicleResponse(
        row.vin(),
        row.currentMileage(),
        row.vehicleTypeId(),
        row.make(),
        row.model(),
        row.trim(),
        row.year(),
        List.of(),
        row.selectedImageUrl());
  }

  private VehicleDetailResponse toDetailResponse(UserVinDetailRow row) {
    return new VehicleDetailResponse(
        row.vin(),
        row.vehicleTypeId(),
        row.vehicleMake(),
        row.vehicleModel(),
        row.vehicleTrim(),
        row.vehicleYear(),
        row.vehicleStyle(),
        row.sourceVin(),
        row.origin(),
        row.body(),
        row.engineDescription(),
        row.transmissionStyle(),
        row.driveType(),
        row.ownersManual(),
        row.currentMileage(),
        ImageUrlsParser.parse(row.availableImageUrlsJson()),
        row.selectedImageUrl());
  }

  private CompletedMaintenanceResponse toCompletedMaintenanceResponse(
      String vin, CompletedMaintenanceRow row) {
    return new CompletedMaintenanceResponse(
        row.completedMaintenanceId(),
        vin,
        row.maintMileageId(),
        row.completedDate(),
        row.mileageCompleted(),
        row.cost(),
        row.notes(),
        row.maintDesc(),
        row.mileageDue());
  }

  private RecallResponse toRecallResponse(String vin, UncompletedRecallRow row) {
    return new RecallResponse(
        row.recallId(),
        vin,
        row.nhtsaCampaignNumber(),
        row.reportReceivedDate(),
        row.component(),
        row.summary(),
        row.consequence(),
        row.remedy());
  }

  private CompletedRecallResponse toCompletedRecallResponse(String vin, CompletedRecallRow row) {
    return new CompletedRecallResponse(
        row.completedRecallId(),
        vin,
        row.recallId(),
        row.completedDate(),
        row.repairShop(),
        row.cost(),
        row.notes(),
        row.nhtsaCampaignNumber(),
        row.reportReceivedDate(),
        row.component(),
        row.summary(),
        row.consequence(),
        row.remedy());
  }

  private MiscMaintenanceCostResponse toMiscCostResponse(MiscMaintCostRow row) {
    return new MiscMaintenanceCostResponse(
        row.miscMaintCostId(),
        row.maintTitle(),
        row.maintDesc(),
        row.independentAvg(),
        row.independentHigh(),
        row.independentLow(),
        row.dealerAvg(),
        row.dealerHigh(),
        row.dealerLow());
  }
}
