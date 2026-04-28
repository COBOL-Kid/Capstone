package com.capstone;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:migration-validation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
		"spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
		"spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.flyway.enabled=true",
		"spring.flyway.target=1"})
class MigrationValidationTest {

	private final DataSource dataSource;

	@Autowired
	MigrationValidationTest(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Test
	void contextLoadsWithBaselineMigration() {
	}

	@Test
	void photoMigrationAddsUserVinPhotoColumns() {
		new ResourceDatabasePopulator(new ClassPathResource("db/migration/V3__add_user_vehicle_photos.sql"))
				.execute(dataSource);

		Integer columnCount = new JdbcTemplate(dataSource).queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.columns
				WHERE lower(table_name) = 'user_vin'
				  AND lower(column_name) IN ('available_image_urls', 'selected_image_url')
				""", Integer.class);

		assertEquals(2, columnCount);
	}
}
