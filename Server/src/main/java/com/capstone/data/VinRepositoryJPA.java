package com.capstone.data;

import com.capstone.models.Vin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VinRepositoryJPA extends JpaRepository<Vin, String> {

	@Query("SELECT uv.vin FROM UserVin uv WHERE uv.user.userId = :userId")
	List<Vin> getVinsByUserId(@Param("userId") Long userId);

	@Modifying
	@Query("delete from Vin v where v.vin in :vins and not exists "
			+ "(select uv.id.vin from UserVin uv where uv.id.vin = v.vin)")
	int deleteOrphanedVins(@Param("vins") Collection<String> vins);

}
