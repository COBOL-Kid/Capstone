package com.capstone.data;

import com.capstone.models.MaintMileage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintMileageRepositoryJPA extends JpaRepository<MaintMileage, Long> {

  @Query(
      "SELECT m FROM MAINT_MILEAGE m WHERE m.maintMileageId = :maintMileageId AND m.vehicleTypeId.vehicleTypeId = :vehicleTypeId")
  Optional<MaintMileage> findByMaintMileageIdAndVehicleTypeId_VehicleTypeId(
      Long maintMileageId, Long vehicleTypeId);
}
