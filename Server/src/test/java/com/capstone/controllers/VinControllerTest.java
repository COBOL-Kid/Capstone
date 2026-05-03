package com.capstone.controllers;

import com.capstone.domain.VehicleOnboardingService;
import com.capstone.domain.VinService;
import com.capstone.models.User;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;
import com.capstone.models.dto.AddVinRequest;
import com.capstone.models.dto.AddVinResponse;
import com.capstone.models.dto.UpdateVehiclePhotoRequest;
import com.capstone.models.dto.UserVehicleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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
        UserVehicleResponse vehicle = userVehicleResponse();

        when(vinService.findVinsByUserId(1L)).thenReturn(List.of(vehicle));

        var response = controller.getCurrentUserVins(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(vehicle), response.getBody());
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
        AddVinResponse created = new AddVinResponse("JTENU5JR6M5962554", 45000, 7L, "Toyota", "4RUNNER", "SRS Prem",
                "2021", List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg"),
                "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg", false, false, true);
        AddVinResponse existing = new AddVinResponse("JTENU5JR6M5962554", 32000, 7L, "Toyota", "4RUNNER", "SRS Prem",
                "2021", List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg"),
                "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg", false, false, false);

        when(onboardingService.addVinToUser(user, request)).thenReturn(created, existing);

        var createdResponse = controller.addVin(user, request);
        var existingResponse = controller.addVin(user, request);

        assertEquals(HttpStatus.CREATED, createdResponse.getStatusCode());
        assertEquals(created, createdResponse.getBody());
        assertEquals(HttpStatus.OK, existingResponse.getStatusCode());
        assertEquals(existing, existingResponse.getBody());
        verify(onboardingService, times(2)).addVinToUser(user, request);
    }

    @Test
    void shouldUpdateSelectedPhotoForCurrentUserVin() {
        VinService vinService = mock(VinService.class);
        VinController controller = new VinController(vinService, mock(VehicleOnboardingService.class));
        User user = user();
        UserVehicleResponse vehicle = userVehicleResponse();
        UpdateVehiclePhotoRequest request = new UpdateVehiclePhotoRequest(
                "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg");

        when(vinService.updateSelectedImage(1L, "JTENU5JR6M5962554", request.selectedImageUrl()))
                .thenReturn(Optional.of(vehicle));
        when(vinService.updateSelectedImage(1L, "MISSINGVIN1234567", request.selectedImageUrl()))
                .thenReturn(Optional.empty());

        var updatedResponse = controller.updateSelectedPhoto(user, "JTENU5JR6M5962554", request);
        var missingResponse = controller.updateSelectedPhoto(user, "MISSINGVIN1234567", request);

        assertEquals(HttpStatus.OK, updatedResponse.getStatusCode());
        assertEquals(vehicle, updatedResponse.getBody());
        assertEquals(HttpStatus.NOT_FOUND, missingResponse.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                controller.updateSelectedPhoto(null, "JTENU5JR6M5962554", request).getStatusCode());
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

    private UserVehicleResponse userVehicleResponse() {
        return new UserVehicleResponse("JTENU5JR6M5962554", 45000, 7L, "Toyota", "4RUNNER", "SRS Prem", "2021",
                List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg"),
                "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg");
    }
}
