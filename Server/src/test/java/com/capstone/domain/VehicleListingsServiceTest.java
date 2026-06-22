package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.capstone.data.read.VehicleReadService;
import com.capstone.integration.AutoDevListingsResponse;
import com.capstone.integration.VehicleDataProviderClient;
import com.capstone.models.dto.VehicleDetailResponse;
import com.capstone.models.dto.VehicleListingsResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class VehicleListingsServiceTest {

  @Test
  void shouldReturnListingsForOwnedVin() {
    VehicleReadService readService = mock(VehicleReadService.class);
    VehicleDataProviderClient client = mock(VehicleDataProviderClient.class);
    VehicleListingsService service = new VehicleListingsService(readService, client);
    VehicleDetailResponse detail = vehicleDetail("SRS Prem", 45000);

    when(readService.findVehicleDetail(1L, "JTENU5JR6M5962554")).thenReturn(Optional.of(detail));
    when(client.getListings("2021", "Toyota", "4RUNNER", 1, "SRS Prem", 35000, 200000))
        .thenReturn(providerResponse());

    Optional<VehicleListingsResponse> result =
        service.findListingsForUserVin(1L, "JTENU5JR6M5962554");

    assertTrue(result.isPresent());
    VehicleListingsResponse listings = result.get();
    assertEquals("JTENU5JR6M5962554", listings.vin());
    assertEquals("2021", listings.year());
    assertEquals("Toyota", listings.make());
    assertEquals("4RUNNER", listings.model());
    assertEquals(661, listings.total());
    assertEquals(1, listings.listings().size());
    assertEquals("1FA6P8JZ1L5552492", listings.listings().getFirst().vin());
    assertEquals(179148, listings.listings().getFirst().price());
    assertEquals(32.971378, listings.listings().getFirst().latitude());
    assertEquals(-96.844514, listings.listings().getFirst().longitude());
    assertEquals(179148, listings.pricingSummary().minPrice());
    assertEquals(179148, listings.pricingSummary().maxPrice());
    assertEquals(179148, listings.pricingSummary().averagePrice());
    assertEquals(1, listings.pricingSummary().pricedListingCount());
    verify(client).getListings("2021", "Toyota", "4RUNNER", 1, "SRS Prem", 35000, 200000);
  }

  @Test
  void shouldComputePricingSummaryAcrossMultipleListings() {
    VehicleReadService readService = mock(VehicleReadService.class);
    VehicleDataProviderClient client = mock(VehicleDataProviderClient.class);
    VehicleListingsService service = new VehicleListingsService(readService, client);
    VehicleDetailResponse detail = vehicleDetail("SRS Prem", 45000);

    when(readService.findVehicleDetail(1L, "JTENU5JR6M5962554")).thenReturn(Optional.of(detail));
    when(client.getListings("2021", "Toyota", "4RUNNER", 1, "SRS Prem", 35000, 200000))
        .thenReturn(
            new AutoDevListingsResponse(
                3,
                List.of(
                    listingWithPrice("VIN00000000000001", 20000),
                    listingWithPrice("VIN00000000000002", 30000),
                    listingWithPrice("VIN00000000000003", 40000))));

    VehicleListingsResponse result =
        service.findListingsForUserVin(1L, "JTENU5JR6M5962554").orElseThrow();

    assertEquals(20000, result.pricingSummary().minPrice());
    assertEquals(40000, result.pricingSummary().maxPrice());
    assertEquals(30000, result.pricingSummary().averagePrice());
    assertEquals(3, result.pricingSummary().pricedListingCount());
  }

  @Test
  void shouldOmitTrimFilterWhenTrimIsBlank() {
    VehicleReadService readService = mock(VehicleReadService.class);
    VehicleDataProviderClient client = mock(VehicleDataProviderClient.class);
    VehicleListingsService service = new VehicleListingsService(readService, client);
    VehicleDetailResponse detail = vehicleDetail("  ", 45000);

    when(readService.findVehicleDetail(1L, "JTENU5JR6M5962554")).thenReturn(Optional.of(detail));
    when(client.getListings("2021", "Toyota", "4RUNNER", 1, "  ", 35000, 200000))
        .thenReturn(providerResponse());

    service.findListingsForUserVin(1L, "JTENU5JR6M5962554");

    verify(client).getListings("2021", "Toyota", "4RUNNER", 1, "  ", 35000, 200000);
  }

  @Test
  void shouldUseHighMileageWindowWhenCurrentMileageExceedsUpperBound() {
    VehicleReadService readService = mock(VehicleReadService.class);
    VehicleDataProviderClient client = mock(VehicleDataProviderClient.class);
    VehicleListingsService service = new VehicleListingsService(readService, client);
    VehicleDetailResponse detail = vehicleDetail("SRS Prem", 250_000);

    when(readService.findVehicleDetail(1L, "JTENU5JR6M5962554")).thenReturn(Optional.of(detail));
    when(client.getListings("2021", "Toyota", "4RUNNER", 1, "SRS Prem", 190_000, 200_000))
        .thenReturn(providerResponse());

    service.findListingsForUserVin(1L, "JTENU5JR6M5962554");

    verify(client).getListings("2021", "Toyota", "4RUNNER", 1, "SRS Prem", 190_000, 200_000);
  }

  @Test
  void shouldReturnEmptyWhenVinIsNotOwned() {
    VehicleReadService readService = mock(VehicleReadService.class);
    VehicleDataProviderClient client = mock(VehicleDataProviderClient.class);
    VehicleListingsService service = new VehicleListingsService(readService, client);

    when(readService.findVehicleDetail(1L, "MISSINGVIN1234567")).thenReturn(Optional.empty());

    assertTrue(service.findListingsForUserVin(1L, "MISSINGVIN1234567").isEmpty());
    verifyNoInteractions(client);
  }

  @Test
  void shouldReturnEmptyListingsWhenProviderReturnsNull() {
    VehicleReadService readService = mock(VehicleReadService.class);
    VehicleDataProviderClient client = mock(VehicleDataProviderClient.class);
    VehicleListingsService service = new VehicleListingsService(readService, client);
    VehicleDetailResponse detail = vehicleDetail("SRS Prem", 45000);

    when(readService.findVehicleDetail(1L, "JTENU5JR6M5962554")).thenReturn(Optional.of(detail));
    when(client.getListings("2021", "Toyota", "4RUNNER", 1, "SRS Prem", 35000, 200000))
        .thenReturn(null);

    VehicleListingsResponse result =
        service.findListingsForUserVin(1L, "JTENU5JR6M5962554").orElseThrow();

    assertNull(result.total());
    assertTrue(result.listings().isEmpty());
    assertNull(result.pricingSummary().minPrice());
    assertNull(result.pricingSummary().maxPrice());
    assertNull(result.pricingSummary().averagePrice());
    assertEquals(0, result.pricingSummary().pricedListingCount());
  }

  @Test
  void shouldMapProviderRateLimitToTooManyRequestsException() {
    VehicleReadService readService = mock(VehicleReadService.class);
    VehicleDataProviderClient client = mock(VehicleDataProviderClient.class);
    VehicleListingsService service = new VehicleListingsService(readService, client);
    VehicleDetailResponse detail = vehicleDetail("SRS Prem", 45000);

    when(readService.findVehicleDetail(1L, "JTENU5JR6M5962554")).thenReturn(Optional.of(detail));
    when(client.getListings("2021", "Toyota", "4RUNNER", 1, "SRS Prem", 35000, 200000))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null));

    TooManyRequestsException ex =
        assertThrows(
            TooManyRequestsException.class,
            () -> service.findListingsForUserVin(1L, "JTENU5JR6M5962554"));

    assertTrue(ex.getMessage().contains("temporarily unavailable"));
  }

  private static VehicleDetailResponse vehicleDetail(String trim, int currentMileage) {
    return new VehicleDetailResponse(
        "JTENU5JR6M5962554",
        7L,
        "Toyota",
        "4RUNNER",
        trim,
        "2021",
        "SUV",
        "JTENU5JR6M5962554",
        "Japan",
        "SUV",
        "V6",
        "Automatic",
        "4WD",
        "https://example.com/manual",
        currentMileage,
        List.of("https://example.com/photos/retail/JTENU5JR6M5962554-1.jpg"),
        "https://example.com/photos/retail/JTENU5JR6M5962554-1.jpg");
  }

  private static AutoDevListingsResponse providerResponse() {
    return new AutoDevListingsResponse(661, List.of(listingWithPrice("1FA6P8JZ1L5552492", 179148)));
  }

  private static AutoDevListingsResponse.Listing listingWithPrice(String vin, int price) {
    return new AutoDevListingsResponse.Listing(
        "https://example.com/listings/" + vin,
        vin,
        "2026-05-19 00:31:18",
        List.of(-96.844514, 32.971378),
        new AutoDevListingsResponse.Vehicle(
            vin,
            2020,
            "Ford",
            "Mustang",
            "GT Premium 2dr Coupe",
            "Car",
            "5.2L 8Cyl Gasoline",
            "RWD",
            "Manual",
            "White",
            "Black"),
        new AutoDevListingsResponse.RetailListing(
            price,
            8,
            "Earth Motorcars",
            "Carrollton",
            "TX",
            "75006",
            "https://retail.photos.vin/" + vin + "-1.jpg",
            "https://example.com/vdp",
            "https://www.carfax.com/VehicleHistory/p/Report.cfx?vin=" + vin,
            true,
            false,
            105),
        new AutoDevListingsResponse.History(false, 0, false, 0, "Vehicle Use"));
  }
}
