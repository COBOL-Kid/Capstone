package com.capstone.data;

import com.capstone.models.VehicleInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VehicleInfoRepositoryJPA extends JpaRepository<VehicleInfo, Long> {

    @Query("SELECT v FROM VehicleInfo v WHERE v.year = ?1 AND v.make = ?2 AND v.model = ?3")
    Optional<VehicleInfo> findByVehicleInfoYearAndMakeAndModel(int year, String make, String model);
}