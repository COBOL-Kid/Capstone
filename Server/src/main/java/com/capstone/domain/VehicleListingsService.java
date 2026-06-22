package com.capstone.domain;

import com.capstone.data.read.VehicleReadService;
import com.capstone.integration.AutoDevListingsResponse;
import com.capstone.integration.VehicleDataProviderClient;
import com.capstone.models.dto.VehicleDetailResponse;
import com.capstone.models.dto.VehicleListingHistoryResponse;
import com.capstone.models.dto.VehicleListingResponse;
import com.capstone.models.dto.VehicleListingsPricingSummary;
import com.capstone.models.dto.VehicleListingsResponse;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class VehicleListingsService {

  static final int LISTINGS_MILEAGE_TOLERANCE = 10_000;
  static final int LISTINGS_MILES_UPPER_BOUND = 200_000;
  static final int LISTINGS_HIGH_MILEAGE_WINDOW_MIN = 190_000;
  private static final int LISTINGS_PAGE = 1;

  private final VehicleReadService vehicleReadService;
  private final VehicleDataProviderClient vehicleDataProviderClient;

  public VehicleListingsService(
      VehicleReadService vehicleReadService, VehicleDataProviderClient vehicleDataProviderClient) {
    this.vehicleReadService = vehicleReadService;
    this.vehicleDataProviderClient = vehicleDataProviderClient;
  }

  @Transactional(readOnly = true)
  public Optional<VehicleListingsResponse> findListingsForUserVin(long userId, String vin) {
    return vehicleReadService.findVehicleDetail(userId, vin).map(this::fetchAndMapListings);
  }

  private VehicleListingsResponse fetchAndMapListings(VehicleDetailResponse detail) {
    AutoDevListingsResponse providerResponse;
    int minMiles;
    int maxMiles = LISTINGS_MILES_UPPER_BOUND;
    if (detail.currentMileage() > LISTINGS_MILES_UPPER_BOUND) {
      minMiles = LISTINGS_HIGH_MILEAGE_WINDOW_MIN;
    } else {
      minMiles = Math.max(0, detail.currentMileage() - LISTINGS_MILEAGE_TOLERANCE);
    }

    try {
      providerResponse =
          vehicleDataProviderClient.getListings(
              detail.vehicleYear(),
              detail.vehicleMake(),
              detail.vehicleModel(),
              LISTINGS_PAGE,
              detail.vehicleTrim(),
              minMiles,
              maxMiles);
    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
        throw new TooManyRequestsException(
            "Vehicle listings are temporarily unavailable. Please try again shortly.");
      }
      throw ex;
    }

    List<VehicleListingResponse> listings =
        providerResponse == null
            ? List.of()
            : providerResponse.listings().stream().map(this::toListingResponse).toList();

    return new VehicleListingsResponse(
        detail.vin(),
        detail.vehicleYear(),
        detail.vehicleMake(),
        detail.vehicleModel(),
        providerResponse != null ? providerResponse.total() : null,
        computePricingSummary(listings),
        listings);
  }

  private VehicleListingsPricingSummary computePricingSummary(
      List<VehicleListingResponse> listings) {
    List<Integer> prices =
        listings.stream()
            .map(VehicleListingResponse::price)
            .filter(Objects::nonNull)
            .sorted()
            .toList();

    if (prices.isEmpty()) {
      return new VehicleListingsPricingSummary(null, null, null, 0);
    }

    int minPrice = prices.getFirst();
    int maxPrice = prices.getLast();
    long priceSum = prices.stream().mapToLong(Integer::longValue).sum();
    int averagePrice = (int) Math.round((double) priceSum / prices.size());

    return new VehicleListingsPricingSummary(minPrice, maxPrice, averagePrice, prices.size());
  }

  private VehicleListingResponse toListingResponse(AutoDevListingsResponse.Listing listing) {
    AutoDevListingsResponse.Vehicle vehicle = listing.vehicle();
    AutoDevListingsResponse.RetailListing retail = listing.retailListing();
    AutoDevListingsResponse.History history = listing.history();

    String year = vehicle != null && vehicle.year() != null ? String.valueOf(vehicle.year()) : null;
    String make = vehicle != null ? vehicle.make() : null;
    String model = vehicle != null ? vehicle.model() : null;
    String style = vehicle != null ? vehicle.style() : null;

    VehicleListingHistoryResponse historyResponse = null;
    if (history != null) {
      historyResponse =
          new VehicleListingHistoryResponse(
              history.accidents(),
              history.accidentCount(),
              history.oneOwner(),
              history.ownerCount(),
              history.usageType());
    }

    return new VehicleListingResponse(
        listing.vin(),
        listing.createdAt(),
        year,
        make,
        model,
        style,
        retail != null ? retail.price() : null,
        retail != null ? retail.miles() : null,
        retail != null ? retail.dealer() : null,
        retail != null ? retail.city() : null,
        retail != null ? retail.state() : null,
        retail != null ? retail.zip() : null,
        retail != null ? retail.primaryImage() : null,
        retail != null ? retail.vdp() : null,
        retail != null ? retail.carfaxUrl() : null,
        retail != null ? retail.used() : null,
        retail != null ? retail.cpo() : null,
        retail != null ? retail.photoCount() : null,
        listing.latitude(),
        listing.longitude(),
        historyResponse);
  }
}
