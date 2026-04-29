package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.capstone.data.VinRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;
import com.capstone.models.Vin;

class VinServiceTest {

	@Test
	void shouldDelegateFindVinsByUserId() {
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
		VinService service = new VinService(vinRepository, userVinRepository);
		Vin vin = vin();
		UserVin userVin = userVin(vin);

		when(userVinRepository.findAllForUser(1L)).thenReturn(List.of(userVin));

		var responses = service.findVinsByUserId(1L);

		assertEquals(1, responses.size());
		assertEquals(vin.getVin(), responses.getFirst().vin());
		assertEquals(45000, responses.getFirst().currentMileage());
		assertEquals("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg",
				responses.getFirst().selectedImageUrl());
	}

	@Test
	void shouldNormalizeVinBeforeLookup() {
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		VinService service = new VinService(vinRepository, mock(UserVinRepositoryJPA.class));
		Vin vin = vin();

		when(vinRepository.findById("JTENU5JR6M5962554")).thenReturn(Optional.of(vin));

		assertEquals(Optional.of(vin), service.findByVin(" jtenu5jr6m5962554 "));
		verify(vinRepository).findById("JTENU5JR6M5962554");
	}

	@Test
	void shouldReturnEmptyForBlankVinWithoutRepositoryLookup() {
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		VinService service = new VinService(vinRepository, mock(UserVinRepositoryJPA.class));

		assertTrue(service.findByVin("  ").isEmpty());
		verify(vinRepository, never()).findById(any());
	}

	@Test
	void shouldUpdateSelectedImageWhenUrlIsAvailable() {
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
		VinService service = new VinService(vinRepository, userVinRepository);
		UserVin userVin = userVin(vin());

		when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554")).thenReturn(Optional.of(userVin));
		when(userVinRepository.save(userVin)).thenReturn(userVin);

		var response = service.updateSelectedImage(1L, " jtenu5jr6m5962554 ",
				"https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg");

		assertTrue(response.isPresent());
		assertEquals("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg", response.get().selectedImageUrl());
	}

	@Test
	void shouldRejectSelectedImageOutsideAvailableImages() {
		UserVin userVin = userVin(vin());
		UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
		VinService service = new VinService(mock(VinRepositoryJPA.class), userVinRepository);

		when(userVinRepository.findForUserVin(1L, "JTENU5JR6M5962554")).thenReturn(Optional.of(userVin));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> service.updateSelectedImage(1L, "JTENU5JR6M5962554", "https://example.com/not-available.jpg"));

		assertEquals("Selected image URL must be one of the available vehicle images", ex.getMessage());
	}

	private Vin vin() {
		VehicleType vehicleType = new VehicleType("Toyota", "4RUNNER", "SRS Prem", "2021");
		vehicleType.setVehicleTypeId(7L);
		return new Vin("JTENU5JR6M5962554", 45000, vehicleType);
	}

	private UserVin userVin(Vin vin) {
		User user = new User();
		user.setUserId(1L);
		UserVin userVin = new UserVin(user, vin, 45000);
		userVin.setAvailableImageUrls(List.of("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg",
				"https://api.auto.dev/photos/retail/JTENU5JR6M5962554-2.jpg"));
		userVin.setSelectedImageUrl("https://api.auto.dev/photos/retail/JTENU5JR6M5962554-1.jpg");
		return userVin;
	}
}
