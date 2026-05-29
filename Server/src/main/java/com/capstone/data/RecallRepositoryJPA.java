package com.capstone.data;

import com.capstone.models.Recall;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecallRepositoryJPA extends JpaRepository<Recall, Long> {

  Optional<Recall> findByRecallIdAndVehicleTypeId_VehicleTypeId(Long recallId, Long vehicleTypeId);

  @Query(
      "SELECT r FROM Recall r "
          + "JOIN UserVin uv ON uv.vin.vehicleType = r.vehicleTypeId "
          + "WHERE uv.user.userId = :userId AND uv.vin.vin = :vin "
          + "AND NOT EXISTS ("
          + "  SELECT 1 FROM CompletedRecall cr "
          + "  WHERE cr.userVin.user.userId = :userId AND cr.userVin.vin.vin = :vin "
          + "  AND cr.recall.recallId = r.recallId"
          + ")")
  List<Recall> findUncompletedRecalls(@Param("userId") Long userId, @Param("vin") String vin);
}
