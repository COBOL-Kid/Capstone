package com.capstone.data;

import com.capstone.models.MaintMileage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintMileageRepositoryJPA extends JpaRepository<MaintMileage, Long> {

  Optional<MaintMileage> findByMaintMileageIdAndVehicleTypeId_VehicleTypeId(
      Long maintMileageId, Long vehicleTypeId);
}
