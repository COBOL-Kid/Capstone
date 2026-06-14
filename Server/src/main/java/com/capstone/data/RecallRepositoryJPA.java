package com.capstone.data;

import com.capstone.models.Recall;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecallRepositoryJPA extends JpaRepository<Recall, Long> {

  @Query(
      "SELECT r FROM Recall r WHERE r.recallId = :recallId AND r.vehicleTypeId.vehicleTypeId = :vehicleTypeId")
  Optional<Recall> findByRecallIdAndVehicleTypeId_VehicleTypeId(Long recallId, Long vehicleTypeId);
}
