package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.capstone.data.VinRepositoryJPA;
import com.capstone.integration.testsupport.VehicleDataProviderMockSupport;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.dto.AddVinRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:provider-429;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.flyway.enabled=true",
      "spring.flyway.target=1",
      "vehicle-data.autodev.base-url=https://api.auto.dev",
      "vehicle-data.vehicle-databases.base-url=https://api.vehicledatabases.com",
      "vehicle-data.autodev.api-key=test-autodev",
      "vehicle-data.vehicle-databases.api-key=test-vdb",
      "vehicle-data.vehicle-databases.max-requests-per-second=0",
      "vehicle-data.http.read-timeout-ms=30000",
      "vehicle-data.http.connect-timeout-ms=5000"
    })
class VehicleOnboardingProvider429IntegrationTest {

  @Autowired private VehicleOnboardingService vehicleOnboardingService;
  @Autowired private VinRepositoryJPA vinRepository;
  @Autowired private RestTemplate restTemplate;

  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
  }

  @Test
  void shouldFailOnboardingWhenProviderReturns429() {
    String rateLimitedVin = "RATE4290000000001";
    assertTrue(vinRepository.findById(rateLimitedVin).isEmpty());

    VehicleDataProviderMockSupport.expectDecodeWithRateLimitedPrefetch(mockServer, rateLimitedVin);

    assertThrows(
        RestClientResponseException.class,
        () ->
            vehicleOnboardingService.addVinToUser(
                testUser(), new AddVinRequest(rateLimitedVin, 45_000, null)));
    assertTrue(vinRepository.findById(rateLimitedVin).isEmpty());
    mockServer.verify();
  }

  private static User testUser() {
    User user = new User();
    user.setUserId(1L);
    user.setUserEmail("driver@example.com");
    user.setRole(Role.USER);
    return user;
  }
}
