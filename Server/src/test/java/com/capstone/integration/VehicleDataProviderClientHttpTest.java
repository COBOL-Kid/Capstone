package com.capstone.integration;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.capstone.integration.testsupport.VehicleDataFixtures;
import com.capstone.integration.testsupport.VehicleDataProviderMockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

class VehicleDataProviderClientHttpTest {

  private static final String VIN = VehicleDataFixtures.BURST_TEST_VIN;

  private RestTemplate restTemplate;
  private MockRestServiceServer mockServer;
  private VehicleDataProviderClient client;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    client =
        new VehicleDataProviderClient(
            restTemplate,
            new VehicleDatabasesRateLimiter(0),
            VehicleDataProviderMockSupport.AUTO_DEV_BASE,
            "auto-dev-key",
            "x-api-key",
            VehicleDataProviderMockSupport.VDB_BASE,
            "vdb-key",
            "x-authkey");
  }

  @AfterEach
  void verifyServer() {
    mockServer.verify();
  }

  @Test
  void shouldFetchVinDecodeOverHttp() {
    mockServer
        .expect(requestTo(VehicleDataProviderMockSupport.AUTO_DEV_BASE + "/vin/" + VIN))
        .andRespond(
            withSuccess(
                VehicleDataFixtures.read("autodev-vin-decode.json"), MediaType.APPLICATION_JSON));

    VinDecodeResponse response = client.decodeVin(VIN);

    assertEquals(VIN, response.vin());
    assertTrue(response.vinValid());
  }

  @Test
  void shouldFetchPhotosOverHttp() {
    mockServer
        .expect(requestTo(VehicleDataProviderMockSupport.AUTO_DEV_BASE + "/photos/" + VIN))
        .andRespond(
            withSuccess(
                VehicleDataFixtures.read("autodev-photos.json"), MediaType.APPLICATION_JSON));

    VehiclePhotosResponse response = client.getPhotos(VIN);

    assertEquals(2, response.retailPhotos().size());
  }

  @Test
  void shouldFetchListingsOverHttp() {
    mockServer
        .expect(
            requestTo(
                allOf(
                    startsWith(VehicleDataProviderMockSupport.AUTO_DEV_BASE + "/listings?"),
                    containsString("vehicle.make=Toyota"),
                    containsString("vehicle.model=4RUNNER"),
                    containsString("vehicle.year=2021"),
                    containsString("retailListing.miles=35000-200000"),
                    containsString("vehicle.trim=SRS%20Prem"),
                    containsString("page=1"),
                    containsString("includes=total"))))
        .andRespond(
            withSuccess(
                VehicleDataFixtures.read("autodev-listings.json"), MediaType.APPLICATION_JSON));

    AutoDevListingsResponse response =
        client.getListings("2021", "Toyota", "4RUNNER", 1, "SRS Prem", 35000, 200000);

    assertEquals(661, response.total());
    assertEquals(1, response.listings().size());
    assertEquals("1FA6P8JZ1L5552492", response.listings().getFirst().vin());
    assertEquals(179148, response.listings().getFirst().retailListing().price());
  }

  @Test
  void shouldFetchRepairEstimatesOverHttp() {
    mockServer
        .expect(requestTo(VehicleDataProviderMockSupport.VDB_BASE + "/repair-estimates/" + VIN))
        .andRespond(
            withSuccess(
                VehicleDataFixtures.read("vdb-repair-estimates.json"), MediaType.APPLICATION_JSON));

    RepairEstimatesResponse response = client.getRepairEstimates(VIN);

    assertEquals("success", response.status());
  }

  @Test
  void shouldFetchRepairCostsOverHttp() {
    mockServer
        .expect(requestTo(VehicleDataProviderMockSupport.VDB_BASE + "/vehicle-repairs/v2/" + VIN))
        .andRespond(
            withSuccess(
                VehicleDataFixtures.read("vdb-repair-costs.json"), MediaType.APPLICATION_JSON));

    RepairCostResponse response = client.getRepairCosts(VIN);

    assertEquals("success", response.status());
  }

  @Test
  void shouldFetchRecallsOverHttp() {
    mockServer
        .expect(requestTo(VehicleDataProviderMockSupport.VDB_BASE + "/vehicle-recalls/" + VIN))
        .andRespond(
            withSuccess(
                VehicleDataFixtures.read("vdb-vehicle-recalls.json"), MediaType.APPLICATION_JSON));

    VehicleRecallsResponse response = client.getRecalls(VIN);

    assertEquals("success", response.status());
  }

  @Test
  void shouldFetchOwnerManualOverHttp() {
    mockServer
        .expect(requestTo(VehicleDataProviderMockSupport.VDB_BASE + "/owner-manual/" + VIN))
        .andRespond(
            withSuccess(
                VehicleDataFixtures.read("vdb-owner-manual.json"), MediaType.APPLICATION_JSON));

    OwnerManualResponse response = client.getOwnerManual(VIN);

    assertEquals("success", response.status());
  }

  @Test
  void shouldFetchTrimOptionsWithNormalizedModelOverHttp() {
    String url =
        UriComponentsBuilder.fromUriString(VehicleDataProviderMockSupport.VDB_BASE)
            .path("/repair-estimates/options/trim/{year}/{make}/{model}")
            .buildAndExpand("2019", "Mazda", "CX 3")
            .encode()
            .toUriString();
    mockServer
        .expect(requestTo(url))
        .andRespond(
            withSuccess(
                """
                {"status":"success","data":{"year":"2019","make":"Mazda","model":"CX 3","trims":["Sport","Touring"]}}
                """,
                MediaType.APPLICATION_JSON));

    TrimOptionsResponse response = client.getTrimOptions("2019", "Mazda", "CX-3");

    assertEquals("success", response.status());
    assertEquals(2, response.data().trims().size());
  }

  @Test
  void shouldFetchVehicleWarrantyOverHttp() {
    mockServer
        .expect(
            requestTo(
                VehicleDataProviderMockSupport.VDB_BASE + "/vehicle-warranty/2021/Toyota/4RUNNER"))
        .andRespond(
            withSuccess(
                VehicleDataFixtures.read("vdb-vehicle-warranty.json"), MediaType.APPLICATION_JSON));

    VehicleWarrantyResponse response = client.getVehicleWarranty("2021", "Toyota", "4RUNNER");

    assertEquals("success", response.status());
    assertEquals("4RUNNER", response.data().model());
  }

  @Test
  void shouldRethrowServerErrorFromVehicleDatabases() {
    mockServer
        .expect(requestTo(VehicleDataProviderMockSupport.VDB_BASE + "/repair-estimates/" + VIN))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    HttpServerErrorException ex =
        assertThrows(HttpServerErrorException.class, () -> client.getRepairEstimates(VIN));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
  }
}
