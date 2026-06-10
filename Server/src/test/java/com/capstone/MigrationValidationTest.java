package com.capstone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    properties = {
      "spring.profiles.active=test",
      "spring.datasource.url=jdbc:h2:mem:migration-validation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.flyway.enabled=true",
      "spring.flyway.target=1"
    })
class MigrationValidationTest {

  private final DataSource dataSource;

  @Autowired
  MigrationValidationTest(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Test
  void v1SchemaIncludesUserVinPhotoColumns() {
    Integer columnCount =
        new JdbcTemplate(dataSource)
            .queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE lower(table_name) = 'user_vin'
                  AND lower(column_name) IN ('available_image_urls', 'selected_image_url')
                """,
                Integer.class);

    assertEquals(2, columnCount);
  }

  @Test
  void v1SchemaHardensUserDetailColumns() {
    Integer columnCount =
        new JdbcTemplate(dataSource)
            .queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE lower(table_name) = 'user_detail'
                  AND lower(column_name) IN ('created_at', 'updated_at')
                """,
                Integer.class);
    Integer emailLength =
        new JdbcTemplate(dataSource)
            .queryForObject(
                """
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE lower(table_name) = 'user_detail'
                  AND lower(column_name) = 'user_email'
                """,
                Integer.class);

    assertEquals(2, columnCount);
    assertEquals(254, emailLength);
  }

  @Test
  void v1SchemaAddsReadPerformanceIndexes() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    assertEquals(
        1,
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.indexes
            WHERE lower(table_name) = 'maint_mileage'
              AND lower(index_name) = 'idx_maint_mileage_vehicle_type_mileage'
            """,
            Integer.class));
    assertEquals(
        1,
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.indexes
            WHERE lower(table_name) = 'recall'
              AND lower(index_name) = 'idx_recall_vehicle_type'
            """,
            Integer.class));
  }
}
