package com.capstone.data;

import com.capstone.models.Vin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VinRepository extends JpaRepository<Vin, Long> {

    @Query("SELECT v FROM Vin v WHERE v.ownerId = ?1")
    Optional<List<Vin>> getVinsByOwnerId(int ownerId);

}
