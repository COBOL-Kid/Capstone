package com.capstone.data;

import com.capstone.models.MaintCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintCostRepositoryJPA extends JpaRepository<MaintCost, Long> {
    
    List<MaintCost> findByVehicleTypeId_VehicleTypeId(Long vehicleTypeId);
}
