package com.capstone.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.domain.dto.UserVehicleResponse;
import com.capstone.models.Vin;
import com.capstone.models.UserVin;
import com.capstone.models.VehicleType;

@Service
public class VinService {

	private final VinRepositoryJPA vinRepositoryJPA;
	private final UserVinRepositoryJPA userVinRepositoryJPA;

	public VinService(VinRepositoryJPA vinRepositoryJPA, UserVinRepositoryJPA userVinRepositoryJPA) {
		this.vinRepositoryJPA = vinRepositoryJPA;
		this.userVinRepositoryJPA = userVinRepositoryJPA;
	}

	public List<UserVehicleResponse> findVinsByUserId(Long userId) {
		return userVinRepositoryJPA.findAllForUser(userId).stream().map(this::toUserVehicleResponse).toList();
	}

	public Optional<Vin> findByVin(String vin) {
		if (vin == null || vin.isBlank()) {
			return Optional.empty();
		}
		return vinRepositoryJPA.findById(vin.trim().toUpperCase());
	}

	@Transactional
	public Optional<UserVehicleResponse> updateSelectedImage(Long userId, String vin, String selectedImageUrl) {
		String normalizedVin = normalizeVin(vin);
		String normalizedImageUrl = selectedImageUrl != null ? selectedImageUrl.trim() : null;
		if (normalizedImageUrl == null || normalizedImageUrl.isBlank()) {
			throw new IllegalArgumentException("Selected image URL is required");
		}
		return userVinRepositoryJPA.findForUserVin(userId, normalizedVin)
				.map(userVin -> updateSelectedImage(userVin, normalizedImageUrl));
	}

	private UserVehicleResponse updateSelectedImage(UserVin userVin, String selectedImageUrl) {
		if (!userVin.getAvailableImageUrls().contains(selectedImageUrl)) {
			throw new IllegalArgumentException("Selected image URL must be one of the available vehicle images");
		}
		userVin.setSelectedImageUrl(selectedImageUrl);
		return toUserVehicleResponse(userVinRepositoryJPA.save(userVin));
	}

	private UserVehicleResponse toUserVehicleResponse(UserVin userVin) {
		Vin vin = userVin.getVin();
		VehicleType vehicleType = vin.getVehicleTypeId();
		return new UserVehicleResponse(vin.getVin(), userVin.getCurrentMileage(), vehicleType.getVehicleTypeId(),
				vehicleType.getVehicleMake(), vehicleType.getVehicleModel(), vehicleType.getVehicleTrim(),
				vehicleType.getVehicleYear(), userVin.getAvailableImageUrls(), userVin.getSelectedImageUrl());
	}

	private String normalizeVin(String vin) {
		if (vin == null || vin.isBlank()) {
			throw new IllegalArgumentException("VIN is required");
		}
		String normalizedVin = vin.trim().toUpperCase();
		if (normalizedVin.length() != 17) {
			throw new IllegalArgumentException("VIN must be 17 characters");
		}
		return normalizedVin;
	}
}
