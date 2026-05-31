package com.capstone.data;

import com.capstone.models.VehicleWarranty;
import com.capstone.models.VehicleWarrantyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleWarrantyRepositoryJPA
    extends JpaRepository<VehicleWarranty, VehicleWarrantyId> {}
