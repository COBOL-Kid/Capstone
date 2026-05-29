package com.capstone.data;

import com.capstone.models.MaintMileage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintMileageRepositoryJPA extends JpaRepository<MaintMileage, Long> {

  Optional<MaintMileage> findByMaintMileageIdAndVehicleTypeId_VehicleTypeId(
      Long maintMileageId, Long vehicleTypeId);

  @EntityGraph(attributePaths = {"partLines", "laborLine"})
  @Query(
      "SELECT m FROM MAINT_MILEAGE m "
          + "WHERE m.vehicleTypeId.vehicleTypeId = :vehicleTypeId "
          + "AND m.mileageDue <= :threshold "
          + "AND NOT EXISTS ("
          + "  SELECT 1 FROM CompletedMaintenance cm "
          + "  WHERE cm.maintMileage.maintMileageId = m.maintMileageId "
          + "  AND cm.userVin.user.userId = :userId "
          + "  AND cm.userVin.vin.vin = :vin"
          + ") "
          + "ORDER BY m.mileageDue ASC, m.maintDesc ASC")
  List<MaintMileage> findUpcomingAndPastDue(
      @Param("vehicleTypeId") Long vehicleTypeId,
      @Param("threshold") int threshold,
      @Param("userId") Long userId,
      @Param("vin") String vin);
}
