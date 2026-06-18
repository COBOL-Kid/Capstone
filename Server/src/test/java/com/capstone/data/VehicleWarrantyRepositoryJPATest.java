package com.capstone.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.models.VehicleWarranty;
import com.capstone.models.VehicleWarrantyId;
import com.capstone.support.IntegrationTestProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class VehicleWarrantyRepositoryJPATest {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

  @DynamicPropertySource
  static void h2FlywaySeedProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2FlywaySeed("vehicle-warranty-repo")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @Autowired private VehicleWarrantyRepositoryJPA vehicleWarrantyRepository;
  @Autowired private JdbcClient jdbcClient;

  @Test
  @Transactional
  void saveCoveragesPersistsJsonObject() throws Exception {
    Map<String, String> coverages = new LinkedHashMap<>();
    coverages.put("Warranty - Basic (months/miles)", "60/60,000");
    coverages.put("Warranty - Powertrain (months/miles)", "60/60,000");

    VehicleWarranty warranty = new VehicleWarranty("2017", "Hyundai", "SONATA");
    warranty.setCoverages(coverages);
    vehicleWarrantyRepository.saveAndFlush(warranty);

    String coveragesJson =
        jdbcClient
            .sql(
                """
                SELECT CAST(coverages AS VARCHAR)
                FROM vehicle_warranty
                WHERE vehicle_year = :year
                  AND vehicle_make = :make
                  AND vehicle_model = :model
                """)
            .param("year", "2017")
            .param("make", "Hyundai")
            .param("model", "SONATA")
            .query(String.class)
            .single();

    assertTrue(coveragesJson.contains("Warranty - Basic (months/miles)"));
    assertEquals(coverages, JSON.readValue(normalizeCoveragesJson(coveragesJson), STRING_MAP));
  }

  private static String normalizeCoveragesJson(String coveragesJson) throws Exception {
    String trimmed = coveragesJson.trim();
    if (trimmed.startsWith("\"")) {
      return JSON.readValue(trimmed, String.class);
    }
    return trimmed;
  }

  @Test
  @Transactional
  void saveUsesVehicleWarrantyIdCompositeKey() {
    VehicleWarranty warranty = new VehicleWarranty("2019", "Mazda", "CX-3");
    warranty.addCoverage("Warranty - Basic (months/miles)", "36/36,000");
    vehicleWarrantyRepository.saveAndFlush(warranty);

    assertTrue(
        vehicleWarrantyRepository.existsById(new VehicleWarrantyId("2019", "Mazda", "CX-3")));
  }
}
