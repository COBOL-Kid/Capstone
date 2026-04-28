package com.capstone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.capstone.domain.VehicleOnboardingService;
import com.capstone.domain.VinService;
import com.capstone.domain.dto.AddVinRequest;
import com.capstone.domain.dto.AddVinResponse;
import com.capstone.models.User;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;

class VinControllerTest {

    @Test
    void shouldRequireAuthenticationForCurrentUserVinsAndAddVin() {
        VinController controller = new VinController(mock(VinService.class), mock(VehicleOnboardingService.class));

        assertEquals(HttpStatus.UNAUTHORIZED, controller.getCurrentUserVins(null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                controller.addVin(null, new AddVinRequest("JTENU5JR6M5962554", 45000)).getStatusCode());
    }

    @Test
    void shouldReturnNoContentWhenCurrentUserHasNoVins() {
        VinService vinService = mock(VinService.class);
        VinController controller = new VinController(vinService, mock(VehicleOnboardingService.class));
        User user = user();

        when(vinService.findVinsByUserId(1L)).thenReturn(List.of());

        assertEquals(HttpStatus.NO_CONTENT, controller.getCurrentUserVins(user).getStatusCode());
    }

    @Test
    void shouldReturnCurrentUserVins() {
        VinService vinService = mock(VinService.class);
        VinController controller = new VinController(vinService, mock(VehicleOnboardingService.class));
        User user = user();
        Vin vin = vin();

        when(vinService.findVinsByUserId(1L)).thenReturn(List.of(vin));

        var response = controller.getCurrentUserVins(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(vin), response.getBody());
    }

    @Test
    void shouldReturnVinLookupStatus() {
        VinService vinService = mock(VinService.class);
        VinController controller = new VinController(vinService, mock(VehicleOnboardingService.class));
        Vin vin = vin();

        when(vinService.findByVin("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));
        when(vinService.findByVin("missing")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.OK, controller.getVin("JTENU5JR6M5962554").getStatusCode());
        assertEquals(vin, controller.getVin("JTENU5JR6M5962554").getBody());
        assertEquals(HttpStatus.NOT_FOUND, controller.getVin("missing").getStatusCode());
    }

    @Test
    void shouldReturnCreatedOnlyWhenAddVinCreatesAssociation() {
        VehicleOnboardingService onboardingService = mock(VehicleOnboardingService.class);
        VinController controller = new VinController(mock(VinService.class), onboardingService);
        User user = user();
        AddVinRequest request = new AddVinRequest("JTENU5JR6M5962554", 45000);
        AddVinResponse created = new AddVinResponse("JTENU5JR6M5962554", 45000, 7L, "Toyota", "4RUNNER",
                "SRS Prem", "2021", false, false, true);
        AddVinResponse existing = new AddVinResponse("JTENU5JR6M5962554", 32000, 7L, "Toyota", "4RUNNER",
                "SRS Prem", "2021", false, false, false);

        when(onboardingService.addVinToUser(user, request)).thenReturn(created, existing);

        var createdResponse = controller.addVin(user, request);
        var existingResponse = controller.addVin(user, request);

        assertEquals(HttpStatus.CREATED, createdResponse.getStatusCode());
        assertEquals(created, createdResponse.getBody());
        assertEquals(HttpStatus.OK, existingResponse.getStatusCode());
        assertEquals(existing, existingResponse.getBody());
        verify(onboardingService, times(2)).addVinToUser(user, request);
    }

    private User user() {
        User user = new User();
        user.setUserId(1L);
        user.setUserEmail("driver@example.com");
        return user;
    }

    private Vin vin() {
        VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
        vehicleType.setVehicleTypeId(7L);
        return new Vin("JTENU5JR6M5962554", 45000, vehicleType);
    }
}
