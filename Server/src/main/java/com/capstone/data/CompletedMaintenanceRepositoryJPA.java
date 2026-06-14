package com.capstone.data;

import com.capstone.models.CompletedMaintenance;
import com.capstone.models.MaintMileage;
import com.capstone.models.UserVin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompletedMaintenanceRepositoryJPA
    extends JpaRepository<CompletedMaintenance, Long> {

  @Query(
      "SELECT c FROM CompletedMaintenance c WHERE c.userVin = :userVin AND c.maintMileage = :maintMileage")
  Optional<CompletedMaintenance> findByUserVinAndMaintMileage(
      UserVin userVin, MaintMileage maintMileage);

  @Modifying
  @Query("delete from CompletedMaintenance cm where cm.userVin.id.userId = :userId")
  int deleteAllForUserId(@Param("userId") Long userId);

  @Modifying
  @Query(
      "delete from CompletedMaintenance cm where cm.userVin.id.userId = :userId and cm.userVin.id.vin = :vin")
  int deleteAllForUserVin(@Param("userId") Long userId, @Param("vin") String vin);
}
