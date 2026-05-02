package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.capstone.data.MaintCostRepositoryJPA;
import com.capstone.data.MaintMileageRepositoryJPA;
import com.capstone.data.RecallRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VehicleTypeRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.dto.AddVinRequest;
import com.capstone.integration.VinDecodeResponse;
import com.capstone.integration.VehicleDataProviderClient;
import com.capstone.integration.VehiclePhotosResponse;
import com.capstone.models.Role;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;

class VehicleOnboardingServiceTest {

	private static final String PHOTO_1 = "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg";
	private static final String PHOTO_2 = "https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg";

	@Test
	void shouldLinkExistingVinAndPopulatePhotosWithoutCallingVinDecode() {
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
		UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
		RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
		MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
		MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
		VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
		VehicleDataMapper mapper = new VehicleDataMapper();
		TransactionTemplate transactionTemplate = transactionTemplate();

		VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
		vehicleType.setVehicleTypeId(7L);
		Vin vin = new Vin("JTENU5JR6M5962554", 12000, vehicleType);
		User user = user();

		when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));
		when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554")).thenReturn(Optional.empty());
		when(userVinRepository.save(any(UserVin.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(photosResponse());

		VehicleOnboardingService service = new VehicleOnboardingService(vinRepository, vehicleTypeRepository,
				userVinRepository, recallRepository, maintMileageRepository, maintCostRepository, providerClient,
				mapper, transactionTemplate);

		var response = service.addVinToUser(user, new AddVinRequest("jtenu5jr6m5962554", 45000));

		assertEquals("JTENU5JR6M5962554", response.vin());
		assertEquals(45000, response.currentMileage());
		assertFalse(response.createdVin());
		assertFalse(response.createdVehicleType());
		assertTrue(response.createdAssociation());
		assertEquals(List.of(PHOTO_1, PHOTO_2), response.availableImageUrls());
		assertEquals(PHOTO_1, response.selectedImageUrl());
		verify(providerClient, never()).decodeVin(any());
	}

	@Test
	void shouldReturnExistingAssociationWithoutChangingMileage() {
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
		UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
		RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
		MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
		MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
		VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
		TransactionTemplate transactionTemplate = transactionTemplate();
		User user = user();
		VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
		vehicleType.setVehicleTypeId(7L);
		Vin vin = new Vin("JTENU5JR6M5962554", 12000, vehicleType);
		UserVin existingAssociation = new UserVin(user, vin, 32000);
		existingAssociation.setAvailableImageUrls(List.of(PHOTO_1));
		existingAssociation.setSelectedImageUrl(PHOTO_1);

		when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));
		when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554"))
				.thenReturn(Optional.of(existingAssociation));

		VehicleOnboardingService service = new VehicleOnboardingService(vinRepository, vehicleTypeRepository,
				userVinRepository, recallRepository, maintMileageRepository, maintCostRepository, providerClient,
				new VehicleDataMapper(), transactionTemplate);

		var response = service.addVinToUser(user, new AddVinRequest(" JTENU5JR6M5962554 ", 45000));

		assertEquals(32000, response.currentMileage());
		assertFalse(response.createdVin());
		assertFalse(response.createdVehicleType());
		assertFalse(response.createdAssociation());
		assertEquals(List.of(PHOTO_1), response.availableImageUrls());
		assertEquals(PHOTO_1, response.selectedImageUrl());
		verify(userVinRepository, never()).save(any(UserVin.class));
		verify(providerClient, never()).decodeVin(any());
		verify(providerClient, never()).getPhotos(any());
	}

	@Test
	void shouldCreateVinForExistingVehicleTypeWithoutFetchingSupplementalData() {
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
		UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
		RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
		MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
		MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
		VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
		TransactionTemplate transactionTemplate = transactionTemplate();
		User user = user();
		VehicleType vehicleType = new VehicleType("Chevrolet", "Silverado 1500", "ZR2", "2022");
		vehicleType.setVehicleStyle("4x4 4dr Crew Cab 5.8 ft. SB");
		vehicleType.setVehicleTypeId(7L);

		when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.empty());
		when(providerClient.decodeVin("JTENU5JR6M5962554")).thenReturn(vinDecodeResponse());
		when(vehicleTypeRepository.findByIdentity("2022", "Chevrolet", "Silverado 1500", "ZR2",
				"4x4 4dr Crew Cab 5.8 ft. SB")).thenReturn(Optional.of(vehicleType));
		when(vinRepository.save(any(Vin.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554")).thenReturn(Optional.empty());
		when(userVinRepository.save(any(UserVin.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(photosResponse());

		VehicleOnboardingService service = new VehicleOnboardingService(vinRepository, vehicleTypeRepository,
				userVinRepository, recallRepository, maintMileageRepository, maintCostRepository, providerClient,
				new VehicleDataMapper(), transactionTemplate);

		var response = service.addVinToUser(user, new AddVinRequest("jtenu5jr6m5962554", 45000));

		assertEquals("JTENU5JR6M5962554", response.vin());
		assertEquals(45000, response.currentMileage());
		assertTrue(response.createdVin());
		assertFalse(response.createdVehicleType());
		assertTrue(response.createdAssociation());
		assertEquals(PHOTO_1, response.selectedImageUrl());
		verify(providerClient, never()).getOwnerManual(any());
		verify(providerClient, never()).getMaintenanceSchedule(any());
		verify(providerClient, never()).getRepairCosts(any());
		verify(providerClient, never()).getRecalls(any());
		verify(recallRepository, never()).saveAll(any());
		verify(maintMileageRepository, never()).saveAll(any());
		verify(maintCostRepository, never()).saveAll(any());
	}

	@Test
	void shouldRejectMissingUserAndInvalidMileageBeforeCallingProvider() {
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
		UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
		RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
		MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
		MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
		VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
		VehicleOnboardingService service = new VehicleOnboardingService(vinRepository, vehicleTypeRepository,
				userVinRepository, recallRepository, maintMileageRepository, maintCostRepository, providerClient,
				new VehicleDataMapper(), transactionTemplate());

		assertEquals("Authenticated user is required", assertThrows(IllegalArgumentException.class,
				() -> service.addVinToUser(null, new AddVinRequest("JTENU5JR6M5962554", 1))).getMessage());
		assertEquals("VIN must be 17 characters", assertThrows(IllegalArgumentException.class,
				() -> service.addVinToUser(user(), new AddVinRequest("too-short", 1))).getMessage());
		assertEquals("Current mileage cannot be negative", assertThrows(IllegalArgumentException.class,
				() -> service.addVinToUser(user(), new AddVinRequest("JTENU5JR6M5962554", -1))).getMessage());
		verify(providerClient, never()).decodeVin(any());
	}

	@Test
	void shouldAllowVehicleOnboardingWhenProviderReturnsNoPhotos() {
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		VehicleTypeRepositoryJPA vehicleTypeRepository = mock(VehicleTypeRepositoryJPA.class);
		UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
		RecallRepositoryJPA recallRepository = mock(RecallRepositoryJPA.class);
		MaintMileageRepositoryJPA maintMileageRepository = mock(MaintMileageRepositoryJPA.class);
		MaintCostRepositoryJPA maintCostRepository = mock(MaintCostRepositoryJPA.class);
		VehicleDataProviderClient providerClient = mock(VehicleDataProviderClient.class);
		TransactionTemplate transactionTemplate = transactionTemplate();
		User user = user();
		VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
		vehicleType.setVehicleTypeId(7L);
		Vin vin = new Vin("JTENU5JR6M5962554", 12000, vehicleType);

		when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));
		when(userVinRepository.findByUserUserIdAndVinVin(1L, "JTENU5JR6M5962554")).thenReturn(Optional.empty());
		when(userVinRepository.save(any(UserVin.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(providerClient.getPhotos("JTENU5JR6M5962554")).thenReturn(new VehiclePhotosResponse(null));

		VehicleOnboardingService service = new VehicleOnboardingService(vinRepository, vehicleTypeRepository,
				userVinRepository, recallRepository, maintMileageRepository, maintCostRepository, providerClient,
				new VehicleDataMapper(), transactionTemplate);

		var response = service.addVinToUser(user, new AddVinRequest("JTENU5JR6M5962554", 45000));

		assertEquals(List.of(), response.availableImageUrls());
		assertEquals(null, response.selectedImageUrl());
		assertTrue(response.createdAssociation());
	}

	private TransactionTemplate transactionTemplate() {
		TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
		when(transactionTemplate.execute(any()))
				.thenAnswer(invocation -> invocation.getArgument(0, TransactionCallback.class).doInTransaction(null));
		return transactionTemplate;
	}

	private User user() {
		User user = new User();
		user.setUserId(1L);
		user.setUserEmail("driver@example.com");
		user.setRole(Role.USER);
		return user;
	}

	private VinDecodeResponse vinDecodeResponse() {
		return new VinDecodeResponse("3GCUDHEL3NG668790", true, "3GC", "Mexico", "3GCUDHELNG", "3", true, "Active",
				"Chevrolet", "Silverado 1500", "ZR2", "4x4 4dr Crew Cab 5.8 ft. SB", "Truck", "5.3L V8 OHV 16V FFV",
				"4WD", "Automatic", new VinDecodeResponse.Vehicle("3GCUDHEL3NG668790", 2022, "Chevrolet",
						"Silverado 1500", "General Motors de Mexico"),
				new VinDecodeResponse.Photos(true, false, true, 12), false);
	}

	private VehiclePhotosResponse photosResponse() {
		return new VehiclePhotosResponse(new VehiclePhotosResponse.PhotoData(List.of(PHOTO_1, PHOTO_2)));
	}
}
