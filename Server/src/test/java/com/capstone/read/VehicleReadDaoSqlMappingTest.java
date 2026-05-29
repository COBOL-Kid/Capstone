package com.capstone.read;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Guards JDBC record mapping: every SELECT must expose {@code AS} labels that match record
 * component snake_case names.
 */
class VehicleReadDaoSqlMappingTest {

  @ParameterizedTest
  @MethodSource("sqlFragments")
  void selectListsAliasForEveryRecordComponent(Class<?> recordType, String sql) {
    for (RecordComponent component : recordType.getRecordComponents()) {
      String label = JdbcRecordColumns.toSnakeCase(component.getName());
      assertTrue(
          sql.contains(" AS " + label) || sql.contains(" AS `" + label + "`"),
          () ->
              recordType.getSimpleName()
                  + "."
                  + component.getName()
                  + " requires result column '"
                  + label
                  + "'");
    }
  }

  private static Stream<Arguments> sqlFragments() throws Exception {
    return Stream.of(
        Arguments.of(UserVinDetailRow.class, readSqlConstant("USER_VIN_DETAIL_SELECT")),
        Arguments.of(
            UserVinListRow.class,
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
            """),
        Arguments.of(
            VehicleReadDao.CompletedMaintenanceRow.class,
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
            """),
        Arguments.of(
            VehicleReadDao.UpcomingMaintRootRow.class,
            """
            SELECT
              m.maint_mileage_id AS maint_mileage_id,
              m.mileage_due AS mileage_due,
              m.maint_desc AS maint_desc,
              m.is_inspect AS inspect
            """),
        Arguments.of(
            VehicleReadDao.MaintPartLineRow.class,
            """
            SELECT
              maint_mileage_id AS maint_mileage_id,
              part_desc AS part_desc,
              total_cost AS total_cost,
              currency AS currency
            """),
        Arguments.of(
            VehicleReadDao.MaintLaborLineRow.class,
            """
            SELECT
              maint_mileage_id AS maint_mileage_id,
              time_required_hours AS time_required_hours,
              hourly_rate AS hourly_rate,
              total_cost AS total_cost,
              currency AS currency
            """),
        Arguments.of(
            VehicleReadDao.MaintSummaryRow.class,
            """
            SELECT
              mileage_due AS mileage_due,
              total_parts_cost AS total_parts_cost,
              total_labor_cost AS total_labor_cost,
              total_cost AS total_cost,
              currency AS currency
            """),
        Arguments.of(
            VehicleReadDao.UncompletedRecallRow.class,
            """
            SELECT
              r.recall_id AS recall_id,
              r.nhtsa_campaign_number AS nhtsa_campaign_number,
              r.report_received_date AS report_received_date,
              r.component AS component,
              r.summary AS summary,
              r.consequence AS consequence,
              r.remedy AS remedy
            """),
        Arguments.of(
            VehicleReadDao.CompletedRecallRow.class,
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
            """),
        Arguments.of(
            VehicleReadDao.MiscMaintCostRow.class,
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
            """));
  }

  private static String readSqlConstant(String fieldName) throws Exception {
    Field field = VehicleReadDao.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return (String) field.get(null);
  }
}
