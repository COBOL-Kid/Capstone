package com.capstone.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.CompletedRecallRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.dto.ChangePasswordRequest;
import com.capstone.models.dto.DeleteAccountRequest;
import com.capstone.models.dto.UpdateAccountRequest;
import com.capstone.models.User;

class AccountServiceTest {

	@Test
	void shouldReturnSafeAccountProfileForCurrentUser() {
		UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
		AccountService service = service(userRepository);
		User principal = principal();
		User storedUser = storedUser();

		when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));

		var response = service.getAccount(principal);

		assertEquals(1L, response.userId());
		assertEquals("driver@example.com", response.email());
		assertEquals("Pat", response.firstName());
		assertEquals("Driver", response.lastName());
		assertEquals("+15551234567", response.userSms());
		assertEquals(storedUser.getCreatedAt(), response.createdAt());
		assertEquals(storedUser.getUpdatedAt(), response.updatedAt());
	}

	@Test
	void shouldUpdateProfileWithTrimmedValues() {
		UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
		AccountService service = service(userRepository);
		User storedUser = storedUser();
		UpdateAccountRequest request = new UpdateAccountRequest(" Patricia ", " Driver-Smith ", "   ");

		when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
		when(userRepository.save(storedUser)).thenReturn(storedUser);

		var response = service.updateProfile(principal(), request);

		assertEquals("Patricia", storedUser.getFirstName());
		assertEquals("Driver-Smith", storedUser.getLastName());
		assertNull(storedUser.getUserSms());
		assertEquals("Patricia", response.firstName());
		verify(userRepository).save(storedUser);
	}

	@Test
	void shouldChangePasswordWhenCurrentPasswordMatches() {
		UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		AccountService service = service(userRepository, passwordEncoder, mock(CompletedMaintenanceRepositoryJPA.class),
				mock(CompletedRecallRepositoryJPA.class), mock(UserVinRepositoryJPA.class),
				mock(VinRepositoryJPA.class));
		User storedUser = storedUser();

		when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
		when(passwordEncoder.matches("old-secret", "encoded-old")).thenReturn(true);
		when(passwordEncoder.encode("new-secret")).thenReturn("encoded-new");

		service.changePassword(principal(), new ChangePasswordRequest("old-secret", "new-secret"));

		assertEquals("encoded-new", storedUser.getUserPw());
		verify(userRepository).save(storedUser);
	}

	@Test
	void shouldRejectPasswordChangeWhenCurrentPasswordDoesNotMatch() {
		UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		AccountService service = service(userRepository, passwordEncoder, mock(CompletedMaintenanceRepositoryJPA.class),
				mock(CompletedRecallRepositoryJPA.class), mock(UserVinRepositoryJPA.class),
				mock(VinRepositoryJPA.class));

		when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser()));
		when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

		assertThrows(InvalidAccountCredentialsException.class,
				() -> service.changePassword(principal(), new ChangePasswordRequest("wrong", "new-secret")));
	}

	@Test
	void shouldPurgeUserOwnedDataBeforeDeletingAccount() {
		UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		CompletedMaintenanceRepositoryJPA completedMaintenanceRepository = mock(
				CompletedMaintenanceRepositoryJPA.class);
		CompletedRecallRepositoryJPA completedRecallRepository = mock(CompletedRecallRepositoryJPA.class);
		UserVinRepositoryJPA userVinRepository = mock(UserVinRepositoryJPA.class);
		VinRepositoryJPA vinRepository = mock(VinRepositoryJPA.class);
		AccountService service = service(userRepository, passwordEncoder, completedMaintenanceRepository,
				completedRecallRepository, userVinRepository, vinRepository);
		User storedUser = storedUser();

		when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));
		when(passwordEncoder.matches("secret", "encoded-old")).thenReturn(true);
		when(userVinRepository.findVinNumbersForUser(1L)).thenReturn(List.of("JTENU5JR6M5962554"));

		service.deleteAccount(principal(), new DeleteAccountRequest("secret"));

		InOrder inOrder = inOrder(completedMaintenanceRepository, completedRecallRepository, userVinRepository,
				vinRepository, userRepository);
		inOrder.verify(userVinRepository).findVinNumbersForUser(1L);
		inOrder.verify(completedMaintenanceRepository).deleteAllForUserId(1L);
		inOrder.verify(completedRecallRepository).deleteAllForUserId(1L);
		inOrder.verify(userVinRepository).deleteAllForUserId(1L);
		inOrder.verify(vinRepository).deleteOrphanedVins(List.of("JTENU5JR6M5962554"));
		inOrder.verify(userRepository).delete(storedUser);
	}

	private AccountService service(UserRepositoryJPA userRepository) {
		return service(userRepository, mock(PasswordEncoder.class), mock(CompletedMaintenanceRepositoryJPA.class),
				mock(CompletedRecallRepositoryJPA.class), mock(UserVinRepositoryJPA.class),
				mock(VinRepositoryJPA.class));
	}

	private AccountService service(UserRepositoryJPA userRepository, PasswordEncoder passwordEncoder,
			CompletedMaintenanceRepositoryJPA completedMaintenanceRepository,
			CompletedRecallRepositoryJPA completedRecallRepository, UserVinRepositoryJPA userVinRepository,
			VinRepositoryJPA vinRepository) {
		return new AccountService(userRepository, passwordEncoder, completedMaintenanceRepository,
				completedRecallRepository, userVinRepository, vinRepository);
	}

	private User principal() {
		User user = new User();
		user.setUserId(1L);
		return user;
	}

	private User storedUser() {
		User user = new User();
		user.setUserId(1L);
		user.setUserEmail("driver@example.com");
		user.setFirstName("Pat");
		user.setLastName("Driver");
		user.setUserSms("+15551234567");
		user.setUserPw("encoded-old");
		user.setCreatedAt(Instant.parse("2026-01-02T03:04:00Z"));
		user.setUpdatedAt(Instant.parse("2026-02-03T04:05:00Z"));
		return user;
	}
}
