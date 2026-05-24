package com.capstone.data;

import com.capstone.models.Vin;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VinRepositoryJPA extends JpaRepository<Vin, String> {

  @Modifying
  @Query(
      "delete from Vin v where v.vin in :vins and not exists "
          + "(select uv.id.vin from UserVin uv where uv.id.vin = v.vin)")
  void deleteOrphanedVins(@Param("vins") Collection<String> vins);
}
