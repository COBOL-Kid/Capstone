package com.capstone.data;

import com.capstone.models.VehicleInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleInfoRepository extends JpaRepository<VehicleInfo, Long> {
}