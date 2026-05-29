package com.capstone.data.read;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-only JDBC queries for vehicle dashboards.
 *
 * <p>Every {@code .query(SomeRecord.class)} call requires result column labels to match record
 * component names in snake_case (e.g. {@code availableImageUrlsJson} → {@code
 * available_image_urls_json}). When a table column name differs, alias it explicitly ({@code
 * is_inspect AS inspect}).
 */
@Repository
public class VehicleReadDao {

  private static final String USER_VIN_DETAIL_SELECT =
      """
      SELECT
        v.vin_num AS vin,
        vt.vehicle_type_id AS vehicle_type_id,
        vt.vehicle_make AS vehicle_make,
        vt.vehicle_model AS vehicle_model,
        vt.vehicle_trim AS vehicle_trim,
        vt.vehicle_year AS vehicle_year,
        vt.vehicle_style AS vehicle_style,
        vt.source_vin AS source_vin,
        vt.origin AS origin,
        vt.`body` AS body,
        vt.engine_description AS engine_description,
        vt.transmission_style AS transmission_style,
        vt.drive_type AS drive_type,
        vt.owners_manual AS owners_manual,
        uv.current_mileage AS current_mileage,
        uv.available_image_urls AS available_image_urls_json,
        uv.selected_image_url AS selected_image_url
      FROM user_vin uv
      JOIN vin v ON v.vin_num = uv.vin_num
      JOIN vehicle_type vt ON vt.vehicle_type_id = v.vehicle_type_id
      WHERE uv.user_id = :userId AND uv.vin_num = :vin
      """;

  private final JdbcClient jdbcClient;

  public VehicleReadDao(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public Optional<UserVinDetailRow> findUserVinDetail(long userId, String vin) {
    return jdbcClient
        .sql(USER_VIN_DETAIL_SELECT)
        .param("userId", userId)
        .param("vin", vin)
        .query(UserVinDetailRow.class)
        .optional();
  }

  public List<UserVinListRow> findUserVehicles(long userId) {
    return jdbcClient
        .sql(
            """
            SELECT
              v.vin_num AS vin,
              uv.current_mileage AS current_mileage,
              vt.vehicle_type_id AS vehicle_type_id,
              vt.vehicle_make AS make,
              vt.vehicle_model AS model,
              vt.vehicle_trim AS `trim`,
              vt.vehicle_year AS `year`,
              uv.selected_image_url AS selected_image_url
            FROM user_vin uv
            JOIN vin v ON v.vin_num = uv.vin_num
            JOIN vehicle_type vt ON vt.vehicle_type_id = v.vehicle_type_id
            WHERE uv.user_id = :userId
            ORDER BY vt.vehicle_make, vt.vehicle_model, v.vin_num
            """)
        .param("userId", userId)
        .query(UserVinListRow.class)
        .list();
  }

  public List<CompletedMaintenanceRow> findCompletedMaintenance(long userId, String vin) {
    return jdbcClient
        .sql(
            """
            SELECT
              cm.completed_maintenance_id AS completed_maintenance_id,
              cm.completed_date AS completed_date,
              cm.mileage_completed AS mileage_completed,
              cm.cost AS cost,
              cm.notes AS notes,
              mm.maint_mileage_id AS maint_mileage_id,
              mm.maint_desc AS maint_desc,
              mm.mileage_due AS mileage_due
            FROM completed_maintenance cm
            JOIN maint_mileage mm ON mm.maint_mileage_id = cm.maint_mileage_id
            WHERE cm.user_id = :userId AND cm.vin_num = :vin
            ORDER BY cm.completed_date DESC, mm.mileage_due ASC
            """)
        .param("userId", userId)
        .param("vin", vin)
        .query(CompletedMaintenanceRow.class)
        .list();
  }

  public List<UpcomingMaintRootRow> findUpcomingMaintenanceRoots(
      long vehicleTypeId, int threshold, long userId, String vin) {
    return jdbcClient
        .sql(
            """
            SELECT
              m.maint_mileage_id AS maint_mileage_id,
              m.mileage_due AS mileage_due,
              m.maint_desc AS maint_desc,
              m.is_inspect AS inspect
            FROM maint_mileage m
            WHERE m.vehicle_type_id = :vehicleTypeId
              AND m.mileage_due <= :threshold
              AND NOT EXISTS (
                SELECT 1 FROM completed_maintenance cm
                WHERE cm.maint_mileage_id = m.maint_mileage_id
                  AND cm.user_id = :userId
                  AND cm.vin_num = :vin
              )
            ORDER BY m.mileage_due ASC, m.maint_desc ASC
            """)
        .param("vehicleTypeId", vehicleTypeId)
        .param("threshold", threshold)
        .param("userId", userId)
        .param("vin", vin)
        .query(UpcomingMaintRootRow.class)
        .list();
  }

  public List<MaintPartLineRow> findPartLines(Collection<Long> maintMileageIds) {
    if (maintMileageIds.isEmpty()) {
      return List.of();
    }
    return jdbcClient
        .sql(
            """
            SELECT
              maint_mileage_id AS maint_mileage_id,
              part_desc AS part_desc,
              total_cost AS total_cost,
              currency AS currency
            FROM maint_part_line
            WHERE maint_mileage_id IN (:ids)
            ORDER BY maint_mileage_id, maint_part_line_id
            """)
        .param("ids", maintMileageIds)
        .query(MaintPartLineRow.class)
        .list();
  }

  public List<MaintLaborLineRow> findLaborLines(Collection<Long> maintMileageIds) {
    if (maintMileageIds.isEmpty()) {
      return List.of();
    }
    return jdbcClient
        .sql(
            """
            SELECT
              maint_mileage_id AS maint_mileage_id,
              time_required_hours AS time_required_hours,
              hourly_rate AS hourly_rate,
              total_cost AS total_cost,
              currency AS currency
            FROM maint_labor_line
            WHERE maint_mileage_id IN (:ids)
            """)
        .param("ids", maintMileageIds)
        .query(MaintLaborLineRow.class)
        .list();
  }

  public List<MaintSummaryRow> findMaintSummaries(
      long vehicleTypeId, Collection<Integer> mileageDues) {
    if (mileageDues.isEmpty()) {
      return List.of();
    }
    return jdbcClient
        .sql(
            """
            SELECT
              mileage_due AS mileage_due,
              total_parts_cost AS total_parts_cost,
              total_labor_cost AS total_labor_cost,
              total_cost AS total_cost,
              currency AS currency
            FROM maint_mileage_summary
            WHERE vehicle_type_id = :vehicleTypeId
              AND mileage_due IN (:mileageDues)
            """)
        .param("vehicleTypeId", vehicleTypeId)
        .param("mileageDues", mileageDues)
        .query(MaintSummaryRow.class)
        .list();
  }

  public List<UncompletedRecallRow> findUncompletedRecalls(long userId, String vin) {
    return jdbcClient
        .sql(
            """
            SELECT
              r.recall_id AS recall_id,
              r.nhtsa_campaign_number AS nhtsa_campaign_number,
              r.report_received_date AS report_received_date,
              r.component AS component,
              r.summary AS summary,
              r.consequence AS consequence,
              r.remedy AS remedy
            FROM recall r
            JOIN vin v ON v.vehicle_type_id = r.vehicle_type_id
            JOIN user_vin uv ON uv.vin_num = v.vin_num
            WHERE uv.user_id = :userId
              AND uv.vin_num = :vin
              AND NOT EXISTS (
                SELECT 1 FROM completed_recall cr
                WHERE cr.user_id = :userId
                  AND cr.vin_num = :vin
                  AND cr.recall_id = r.recall_id
              )
            ORDER BY r.report_received_date DESC, r.nhtsa_campaign_number ASC
            """)
        .param("userId", userId)
        .param("vin", vin)
        .query(UncompletedRecallRow.class)
        .list();
  }

  public List<CompletedRecallRow> findCompletedRecalls(long userId, String vin) {
    return jdbcClient
        .sql(
            """
            SELECT
              cr.completed_recall_id AS completed_recall_id,
              cr.completed_date AS completed_date,
              cr.repair_shop AS repair_shop,
              cr.cost AS cost,
              cr.notes AS notes,
              r.recall_id AS recall_id,
              r.nhtsa_campaign_number AS nhtsa_campaign_number,
              r.report_received_date AS report_received_date,
              r.component AS component,
              r.summary AS summary,
              r.consequence AS consequence,
              r.remedy AS remedy
            FROM completed_recall cr
            JOIN recall r ON r.recall_id = cr.recall_id
            WHERE cr.user_id = :userId AND cr.vin_num = :vin
            ORDER BY cr.completed_date DESC
            """)
        .param("userId", userId)
        .param("vin", vin)
        .query(CompletedRecallRow.class)
        .list();
  }

  public List<MiscMaintCostRow> findMiscCosts(long vehicleTypeId) {
    return jdbcClient
        .sql(
            """
            SELECT
              misc_maint_cost_id AS misc_maint_cost_id,
              maint_title AS maint_title,
              maint_desc AS maint_desc,
              independent_avg AS independent_avg,
              independent_high AS independent_high,
              independent_low AS independent_low,
              dealer_avg AS dealer_avg,
              dealer_high AS dealer_high,
              dealer_low AS dealer_low
            FROM misc_maint_cost
            WHERE vehicle_type_id = :vehicleTypeId
            ORDER BY maint_title ASC
            """)
        .param("vehicleTypeId", vehicleTypeId)
        .query(MiscMaintCostRow.class)
        .list();
  }

  public record CompletedMaintenanceRow(
      long completedMaintenanceId,
      LocalDate completedDate,
      int mileageCompleted,
      Double cost,
      String notes,
      long maintMileageId,
      String maintDesc,
      int mileageDue) {
    public CompletedMaintenanceRow(
        long completedMaintenanceId,
        Date completedDate,
        int mileageCompleted,
        Double cost,
        String notes,
        long maintMileageId,
        String maintDesc,
        int mileageDue) {
      this(
          completedMaintenanceId,
          completedDate != null ? completedDate.toLocalDate() : null,
          mileageCompleted,
          cost,
          notes,
          maintMileageId,
          maintDesc,
          mileageDue);
    }
  }

  public record UpcomingMaintRootRow(
      long maintMileageId, int mileageDue, String maintDesc, boolean inspect) {}

  public record MaintPartLineRow(
      long maintMileageId, String partDesc, BigDecimal totalCost, String currency) {}

  public record MaintLaborLineRow(
      long maintMileageId,
      BigDecimal timeRequiredHours,
      BigDecimal hourlyRate,
      BigDecimal totalCost,
      String currency) {}

  public record MaintSummaryRow(
      int mileageDue,
      BigDecimal totalPartsCost,
      BigDecimal totalLaborCost,
      BigDecimal totalCost,
      String currency) {}

  public record UncompletedRecallRow(
      long recallId,
      String nhtsaCampaignNumber,
      LocalDate reportReceivedDate,
      String component,
      String summary,
      String consequence,
      String remedy) {
    public UncompletedRecallRow(
        long recallId,
        String nhtsaCampaignNumber,
        Date reportReceivedDate,
        String component,
        String summary,
        String consequence,
        String remedy) {
      this(
          recallId,
          nhtsaCampaignNumber,
          reportReceivedDate != null ? reportReceivedDate.toLocalDate() : null,
          component,
          summary,
          consequence,
          remedy);
    }
  }

  public record CompletedRecallRow(
      long completedRecallId,
      LocalDate completedDate,
      String repairShop,
      Double cost,
      String notes,
      long recallId,
      String nhtsaCampaignNumber,
      LocalDate reportReceivedDate,
      String component,
      String summary,
      String consequence,
      String remedy) {
    public CompletedRecallRow(
        long completedRecallId,
        Date completedDate,
        String repairShop,
        Double cost,
        String notes,
        long recallId,
        String nhtsaCampaignNumber,
        Date reportReceivedDate,
        String component,
        String summary,
        String consequence,
        String remedy) {
      this(
          completedRecallId,
          completedDate != null ? completedDate.toLocalDate() : null,
          repairShop,
          cost,
          notes,
          recallId,
          nhtsaCampaignNumber,
          reportReceivedDate != null ? reportReceivedDate.toLocalDate() : null,
          component,
          summary,
          consequence,
          remedy);
    }
  }

  public record MiscMaintCostRow(
      long miscMaintCostId,
      String maintTitle,
      String maintDesc,
      Integer independentAvg,
      Integer independentHigh,
      Integer independentLow,
      Integer dealerAvg,
      Integer dealerHigh,
      Integer dealerLow) {}
}
