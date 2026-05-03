package com.capstone.data;

import com.capstone.models.Recall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecallRepositoryJPA extends JpaRepository<Recall, Long> {

    @Query("SELECT r FROM Recall r "
            + "JOIN UserVin uv ON uv.vin.vehicleTypeId = r.vehicleTypeId "
            + "WHERE uv.user.userId = :userId AND uv.vin.vin = :vin "
            + "AND NOT EXISTS ("
            + "  SELECT 1 FROM CompletedRecall cr "
            + "  WHERE cr.userVin.user.userId = :userId AND cr.userVin.vin.vin = :vin "
            + "  AND cr.recall.recallId = r.recallId"
            + ")")
    List<Recall> findUncompletedRecalls(@Param("userId") Long userId, @Param("vin") String vin);
}
