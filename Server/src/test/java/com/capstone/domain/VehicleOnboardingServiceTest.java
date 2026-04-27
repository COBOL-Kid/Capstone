package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import com.capstone.data.MaintCostRepositoryJPA;
import com.capstone.data.MaintMileageRepositoryJPA;
import com.capstone.data.RecallRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VehicleTypeRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.domain.dto.AddVinRequest;
import com.capstone.integration.vehicledatabases.VehicleDataProviderClient;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;

class VehicleOnboardingServiceTest {

    @Test
    void shouldLinkExistingVinWithoutCallingProvider() {
        VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
        VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
        UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
        RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
        MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
        MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
        VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
        VehicleDataMapper mapper = new VehicleDataMapper();
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> invocation.getArgument(0,
                org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));

        VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
        vehicleType.setVehicleTypeId(7L);
        Vin vin = new Vin("JTENU5JR6M5962554", 12000, vehicleType);
        User user = new User();
        user.setUserId(1L);
        user.setUserEmail("driver@example.com");
        user.setRole(Role.USER);

        when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));
        when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554")).thenReturn(Optional.empty());
        when(userVinRepository.save(any(UserVin.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleOnboardingService service = new VehicleOnboardingService(vinRepository, vehicleTypeRepository,
                userVinRepository, recallRepository, maintMileageRepository, maintCostRepository, providerClient, mapper,
                transactionTemplate);

        var response = service.addVinToUser(user, new AddVinRequest("jtenu5jr6m5962554", 45000));

        assertEquals("JTENU5JR6M5962554", response.vin());
        assertEquals(45000, response.currentMileage());
        assertFalse(response.createdVin());
        assertFalse(response.createdVehicleType());
        assertTrue(response.createdAssociation());
        verify(providerClient, never()).decodeVin(any());
    }
}