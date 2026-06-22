package com.capstone.controllers;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.integration.AutoDevListingsResponse;
import com.capstone.integration.VehicleDataProviderClient;
import com.capstone.support.IntegrationTestProperties;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class VinControllerListingsIntegrationTest {

  private static final String CAMRY_VIN = "4T1C11AK5LU123456";

  @MockitoBean private VehicleDataProviderClient vehicleDataProviderClient;

  @Autowired private MockMvc mockMvc;

  @DynamicPropertySource
  static void h2FlywaySeedProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2FlywaySeed("vin-listings-integration")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @Test
  void shouldReturnListingsWithPricingSummaryForOwnedVin() throws Exception {
    when(vehicleDataProviderClient.getListings(
            eq("2020"), eq("Toyota"), eq("Camry"), eq(1), eq("SE"), anyInt(), anyInt()))
        .thenReturn(providerResponse());

    Cookie sessionCookie = login();

    mockMvc
        .perform(get("/api/vin/{vin}/listings", CAMRY_VIN).cookie(sessionCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vin").value(CAMRY_VIN))
        .andExpect(jsonPath("$.year").value("2020"))
        .andExpect(jsonPath("$.make").value("Toyota"))
        .andExpect(jsonPath("$.model").value("Camry"))
        .andExpect(jsonPath("$.total").value(661))
        .andExpect(jsonPath("$.pricingSummary.minPrice").value(179148))
        .andExpect(jsonPath("$.pricingSummary.maxPrice").value(179148))
        .andExpect(jsonPath("$.pricingSummary.averagePrice").value(179148))
        .andExpect(jsonPath("$.pricingSummary.pricedListingCount").value(1))
        .andExpect(jsonPath("$.listings[0].vin").value("1FA6P8JZ1L5552492"))
        .andExpect(jsonPath("$.listings[0].price").value(179148));
  }

  @Test
  void shouldReturnNotFoundForVinNotOwnedByUser() throws Exception {
    Cookie sessionCookie = login();

    mockMvc
        .perform(get("/api/vin/{vin}/listings", "1FA6P8JZ1L5552492").cookie(sessionCookie))
        .andExpect(status().isNotFound());
  }

  private Cookie login() throws Exception {
    MockHttpServletResponse bootstrap =
        mockMvc.perform(get("/actuator/health")).andReturn().getResponse();

    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/auth/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-XSRF-TOKEN", readCsrf(bootstrap))
                    .cookie(bootstrap.getCookies())
                    .content(
                        """
                        {"email":"test.user@example.com","password":"Password123!"}
                        """))
            .andExpect(status().isOk())
            .andReturn();

    Cookie sessionCookie = findCookie(loginResult.getResponse().getCookies(), "__session");
    if (sessionCookie == null) {
      throw new IllegalStateException("Missing __session cookie after login");
    }
    return sessionCookie;
  }

  private static AutoDevListingsResponse providerResponse() {
    return new AutoDevListingsResponse(
        661,
        List.of(
            new AutoDevListingsResponse.Listing(
                "https://example.com/listings/1FA6P8JZ1L5552492",
                "1FA6P8JZ1L5552492",
                "2026-05-19 00:31:18",
                List.of(-96.844514, 32.971378),
                new AutoDevListingsResponse.Vehicle(
                    "1FA6P8JZ1L5552492",
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
                    179148,
                    8,
                    "Earth Motorcars",
                    "Carrollton",
                    "TX",
                    "75006",
                    "https://retail.photos.vin/1FA6P8JZ1L5552492-1.jpg",
                    "https://example.com/vdp",
                    "https://www.carfax.com/VehicleHistory/p/Report.cfx?vin=1FA6P8JZ1L5552492",
                    true,
                    false,
                    105),
                new AutoDevListingsResponse.History(false, 0, false, 0, "Vehicle Use"))));
  }

  private static Cookie findCookie(Cookie[] cookies, String name) {
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  private static String readCsrf(MockHttpServletResponse response) {
    Cookie[] cookies = response.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("XSRF-TOKEN".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    throw new IllegalStateException("Missing XSRF-TOKEN cookie");
  }
}
