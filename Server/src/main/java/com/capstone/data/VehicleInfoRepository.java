package com.capstone.data;

import com.capstone.models.VehicleInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VehicleInfoRepository extends JpaRepository<VehicleInfo, Long> {
}