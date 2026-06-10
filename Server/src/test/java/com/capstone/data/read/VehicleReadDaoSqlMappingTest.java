package com.capstone.data.read;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
          sql.contains(" AS " + label)
              || sql.contains(" AS `" + label + "`")
              || sql.contains(" AS \"" + label + "\""),
          () ->
              recordType.getSimpleName()
                  + "."
                  + component.getName()
                  + " requires result column '"
                  + label
                  + "'");
    }
  }

  private static Stream<Arguments> sqlFragments() {
    return Stream.of(
        Arguments.of(UserVinDetailRow.class, VehicleReadDao.USER_VIN_DETAIL_SELECT),
        Arguments.of(UserVinListRow.class, VehicleReadDao.USER_VIN_LIST_SELECT),
        Arguments.of(
            VehicleReadDao.CompletedMaintenanceRow.class,
            VehicleReadDao.COMPLETED_MAINTENANCE_SELECT),
        Arguments.of(
            VehicleReadDao.UpcomingMaintRootRow.class, VehicleReadDao.UPCOMING_MAINT_ROOT_SELECT),
        Arguments.of(VehicleReadDao.MaintPartLineRow.class, VehicleReadDao.MAINT_PART_LINE_SELECT),
        Arguments.of(
            VehicleReadDao.MaintLaborLineRow.class, VehicleReadDao.MAINT_LABOR_LINE_SELECT),
        Arguments.of(VehicleReadDao.MaintSummaryRow.class, VehicleReadDao.MAINT_SUMMARY_SELECT),
        Arguments.of(
            VehicleReadDao.UncompletedRecallRow.class, VehicleReadDao.UNCOMPLETED_RECALL_SELECT),
        Arguments.of(
            VehicleReadDao.CompletedRecallRow.class, VehicleReadDao.COMPLETED_RECALL_SELECT),
        Arguments.of(VehicleReadDao.MiscMaintCostRow.class, VehicleReadDao.MISC_MAINT_COST_SELECT));
  }
}
