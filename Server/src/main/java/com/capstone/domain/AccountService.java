package com.capstone.domain;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.data.CompletedMaintenanceRepositoryJPA;
import com.capstone.data.CompletedRecallRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.dto.AccountResponse;
import com.capstone.models.dto.ChangePasswordRequest;
import com.capstone.models.dto.DeleteAccountRequest;
import com.capstone.models.dto.UpdateAccountRequest;
import com.capstone.models.User;

@Service
public class AccountService {

	private final UserRepositoryJPA userRepository;
	private final PasswordEncoder passwordEncoder;
	private final CompletedMaintenanceRepositoryJPA completedMaintenanceRepository;
	private final CompletedRecallRepositoryJPA completedRecallRepository;
	private final UserVinRepositoryJPA userVinRepository;
	private final VinRepositoryJPA vinRepository;

	public AccountService(UserRepositoryJPA userRepository, PasswordEncoder passwordEncoder,
			CompletedMaintenanceRepositoryJPA completedMaintenanceRepository,
			CompletedRecallRepositoryJPA completedRecallRepository, UserVinRepositoryJPA userVinRepository,
			VinRepositoryJPA vinRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.completedMaintenanceRepository = completedMaintenanceRepository;
		this.completedRecallRepository = completedRecallRepository;
		this.userVinRepository = userVinRepository;
		this.vinRepository = vinRepository;
	}

	@Transactional(readOnly = true)
	public AccountResponse getAccount(User principal) {
		return toResponse(loadCurrentUser(principal));
	}

	@Transactional
	public AccountResponse updateProfile(User principal, UpdateAccountRequest request) {
		User user = loadCurrentUser(principal);
		user.setFirstName(cleanRequired(request.firstName(), "First name is required"));
		user.setLastName(cleanRequired(request.lastName(), "Last name is required"));
		user.setUserSms(cleanOptional(request.userSms()));
		return toResponse(userRepository.save(user));
	}

	@Transactional
	public void changePassword(User principal, ChangePasswordRequest request) {
		User user = loadCurrentUser(principal);
		if (!passwordEncoder.matches(request.currentPassword(), user.getUserPw())) {
			throw new InvalidAccountCredentialsException();
		}
		user.setUserPw(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}

	@Transactional
	public void deleteAccount(User principal, DeleteAccountRequest request) {
		User user = loadCurrentUser(principal);
		if (!passwordEncoder.matches(request.password(), user.getUserPw())) {
			throw new InvalidAccountCredentialsException();
		}
		Long userId = user.getUserId();
		List<String> userVins = userVinRepository.findVinNumbersForUser(userId);
		completedMaintenanceRepository.deleteAllForUserId(userId);
		completedRecallRepository.deleteAllForUserId(userId);
		userVinRepository.deleteAllForUserId(userId);
		if (!userVins.isEmpty()) {
			vinRepository.deleteOrphanedVins(userVins);
		}
		userRepository.delete(user);
	}

	private User loadCurrentUser(User principal) {
		if (principal == null || principal.getUserId() == null) {
			throw new InvalidAccountCredentialsException();
		}
		return userRepository.findById(principal.getUserId()).orElseThrow(InvalidAccountCredentialsException::new);
	}

	private AccountResponse toResponse(User user) {
		return new AccountResponse(user.getUserId(), user.getUserEmail(), user.getFirstName(), user.getLastName(),
				user.getUserSms(), user.getCreatedAt(), user.getUpdatedAt());
	}

	private String cleanRequired(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String cleanOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
