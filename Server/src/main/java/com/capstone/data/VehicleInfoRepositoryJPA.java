package com.capstone.data;

import com.capstone.models.VehicleInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleInfoRepositoryJPA extends JpaRepository<VehicleInfo, Long> {
}