package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.capstone.data.VinRepositoryJPA;
import com.capstone.integration.testsupport.VehicleDataFixtures;
import com.capstone.integration.testsupport.VehicleDataProviderMockSupport;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.dto.AddVinOutcome;
import com.capstone.models.dto.AddVinRequest;
import com.capstone.models.dto.AddVinResponse;
import com.capstone.support.IntegrationTestProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    properties = {
      "vehicle-data.autodev.base-url=https://api.auto.dev",
      "vehicle-data.vehicle-databases.base-url=https://api.vehicledatabases.com",
      "vehicle-data.autodev.api-key=test-autodev",
      "vehicle-data.vehicle-databases.api-key=test-vdb",
      "vehicle-data.http.read-timeout-ms=30000",
      "vehicle-data.http.connect-timeout-ms=5000",
      "vehicle-data.vehicle-databases.max-requests-per-second=2"
    })
class VehicleOnboardingProviderBurstIntegrationTest {

  private static final String VIN = VehicleDataFixtures.BURST_TEST_VIN;
  private static final long REQUEST_DELAY_MS = 300;
  private static final long MAX_ONBOARDING_MS = 8_000;

  @Autowired private VehicleOnboardingService vehicleOnboardingService;
  @Autowired private VinRepositoryJPA vinRepository;
  @Autowired private RestTemplate restTemplate;

  @DynamicPropertySource
  static void h2FlywaySeedProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2FlywaySeed("provider-burst")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  private MockRestServiceServer mockServer;
  private final AtomicInteger inFlight = new AtomicInteger();
  private final AtomicInteger maxInFlight = new AtomicInteger();
  private final CopyOnWriteArrayList<Long> requestStarts = new CopyOnWriteArrayList<>();
  private final AtomicInteger vdbInFlight = new AtomicInteger();
  private final AtomicInteger vdbMaxInFlight = new AtomicInteger();
  private final CopyOnWriteArrayList<Long> vdbRequestStarts = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() {
    mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    inFlight.set(0);
    maxInFlight.set(0);
    requestStarts.clear();
    vdbInFlight.set(0);
    vdbMaxInFlight.set(0);
    vdbRequestStarts.clear();
  }

  @Test
  void shouldOnboardNewVinWithinTimeoutDespiteParallelProviderLatency() {
    assertTrue(vinRepository.findById(VIN).isEmpty());

    VehicleDataProviderMockSupport.expectNewVinOnboardingCalls(
        mockServer,
        VIN,
        "2021",
        "Toyota",
        "4RUNNER",
        REQUEST_DELAY_MS,
        inFlight,
        maxInFlight,
        requestStarts,
        vdbInFlight,
        vdbMaxInFlight,
        vdbRequestStarts);

    long startMs = System.currentTimeMillis();
    AddVinOutcome outcome =
        vehicleOnboardingService.addVinToUser(testUser(), new AddVinRequest(VIN, 45_000, null));
    assertInstanceOf(AddVinOutcome.Completed.class, outcome);
    AddVinResponse response = ((AddVinOutcome.Completed) outcome).response();
    long durationMs = System.currentTimeMillis() - startMs;

    assertTrue(response.createdVehicleType());
    assertTrue(response.createdVin());
    assertTrue(response.createdAssociation());
    assertEquals(VIN, response.vin());
    assertEquals(2, response.availableImageUrls().size());
    assertTrue(
        durationMs < MAX_ONBOARDING_MS, "expected parallel fetch, took " + durationMs + "ms");
    assertTrue(durationMs >= REQUEST_DELAY_MS, "expected real latency, took " + durationMs + "ms");
    assertTrue(
        vdbMaxInFlight.get() <= 2,
        "Vehicle Databases calls should be throttled, peak=" + vdbMaxInFlight.get());
    assertEquals(5, vdbRequestStarts.size());
    assertNoMoreThanTwoVdbStartsPerSecond(vdbRequestStarts);
    assertEquals(7, requestStarts.size());

    long decodeStartMs = requestStarts.getFirst();
    long firstParallelStartMs =
        requestStarts.subList(1, requestStarts.size()).stream()
            .mapToLong(Long::longValue)
            .min()
            .orElseThrow();
    assertTrue(
        firstParallelStartMs >= decodeStartMs + REQUEST_DELAY_MS - 50,
        "supplemental calls should start after decodeVin completes");
    mockServer.verify();
  }

  private static void assertNoMoreThanTwoVdbStartsPerSecond(CopyOnWriteArrayList<Long> vdbStarts) {
    List<Long> sorted = new ArrayList<>(vdbStarts);
    Collections.sort(sorted);
    for (int i = 0; i < sorted.size(); i++) {
      long windowStart = sorted.get(i);
      int countInWindow = 0;
      for (long t : sorted) {
        if (t >= windowStart && t < windowStart + 1000) {
          countInWindow++;
        }
      }
      assertTrue(
          countInWindow <= 2,
          "expected at most 2 VDB HTTP starts per 1s window, saw "
              + countInWindow
              + " at "
              + windowStart);
    }
  }

  private static User testUser() {
    User user = new User();
    user.setUserId(1L);
    user.setUserEmail("driver@example.com");
    user.setRole(Role.USER);
    user.setEmailVerified(true);
    return user;
  }
}
