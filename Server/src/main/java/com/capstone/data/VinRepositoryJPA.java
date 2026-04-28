package com.capstone.data;

import com.capstone.models.Vin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VinRepositoryJPA extends JpaRepository<Vin, String> {

	@Query("SELECT uv.vin FROM UserVin uv WHERE uv.user.userId = :userId")
	List<Vin> getVinsByUserId(@Param("userId") Long userId);

}
