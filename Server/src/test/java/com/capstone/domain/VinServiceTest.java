package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;

class VinServiceTest {

    @Test
    void shouldDelegateFindVinsByUserId() {
        VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
        VinService service = new VinService(vinRepository);
        Vin vin = vin();

        when(vinRepository.getVinsByUserId(1L)).thenReturn(List.of(vin));

        assertEquals(List.of(vin), service.findVinsByUserId(1L));
    }

    @Test
    void shouldNormalizeVinBeforeLookup() {
        VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
        VinService service = new VinService(vinRepository);
        Vin vin = vin();

        when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));

        assertEquals(Optional.of(vin), service.findByVin(" jtenu5jr6m5962554 "));
        verify(vinRepository).findById("JTENU5JR6M5962554");
    }

    @Test
    void shouldReturnEmptyForBlankVinWithoutRepositoryLookup() {
        VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
        VinService service = new VinService(vinRepository);

        assertTrue(service.findByVin("  ").isEmpty());
        verify(vinRepository, never()).findById(any());
    }

    private Vin vin() {
        VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
        vehicleType.setVehicleTypeId(7L);
        return new Vin("JTENU5JR6M5962554", 45000, vehicleType);
    }
}
