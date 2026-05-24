package com.capstone.data;

import com.capstone.models.MaintCost;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintCostRepositoryJPA extends JpaRepository<MaintCost, Long> {

  List<MaintCost> findByVehicleTypeId_VehicleTypeId(Long vehicleTypeId);
}
