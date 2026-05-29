package com.capstone.data;

import com.capstone.models.Recall;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecallRepositoryJPA extends JpaRepository<Recall, Long> {

  Optional<Recall> findByRecallIdAndVehicleTypeId_VehicleTypeId(Long recallId, Long vehicleTypeId);
}
