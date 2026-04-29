package com.capstone.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capstone.models.UserVin;
import com.capstone.models.UserVinId;

public interface UserVinRepositoryJPA extends JpaRepository<UserVin, UserVinId> {

	Optional<UserVin> findByUserUserIdAndVinVin(Long userId, String vin);

	@Query("SELECT uv FROM UserVin uv JOIN FETCH uv.vin v JOIN FETCH v.vehicleTypeId WHERE uv.user.userId = :userId")
	List<UserVin> findAllForUser(@Param("userId") Long userId);

	@Query("SELECT uv FROM UserVin uv JOIN FETCH uv.vin v JOIN FETCH v.vehicleTypeId "
			+ "WHERE uv.user.userId = :userId AND uv.vin.vin = :vin")
	Optional<UserVin> findForUserVin(@Param("userId") Long userId, @Param("vin") String vin);
}
