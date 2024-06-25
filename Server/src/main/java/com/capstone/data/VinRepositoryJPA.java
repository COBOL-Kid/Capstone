package com.capstone.data;

import com.capstone.models.Vin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VinRepositoryJPA extends JpaRepository<Vin, Long> {

    @Query("SELECT v FROM Vin v WHERE v.ownerId = ?1")
    List<Vin> getVinsByOwnerId(Long ownerId);

}
