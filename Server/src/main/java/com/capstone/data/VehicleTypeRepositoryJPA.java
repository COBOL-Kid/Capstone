package com.capstone.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capstone.models.VehicleType;

public interface VehicleTypeRepositoryJPA extends JpaRepository<VehicleType, Long> {

    @Query("""
            select vt from #{#entityName} vt
            where lower(vt.vehicleYear) = lower(:vehicleYear)
                and lower(vt.vehicleMake) = lower(:vehicleMake)
                and lower(vt.vehicleModel) = lower(:vehicleModel)
                and lower(vt.vehicleTrim) = lower(:vehicleTrim)
                and lower(vt.vehicleStyle) = lower(:vehicleStyle)
            """)
    Optional<VehicleType> findByIdentity(@Param("vehicleYear") String vehicleYear,
            @Param("vehicleMake") String vehicleMake, @Param("vehicleModel") String vehicleModel,
            @Param("vehicleTrim") String vehicleTrim, @Param("vehicleStyle") String vehicleStyle);
}