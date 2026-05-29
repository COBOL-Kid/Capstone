package com.capstone.data;

import com.capstone.models.MiscMaintCost;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiscMaintCostRepositoryJPA extends JpaRepository<MiscMaintCost, Long> {

  List<MiscMaintCost> findByVehicleTypeId_VehicleTypeId(Long vehicleTypeId);
}
