package com.capstone.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity(name = "vehicle_type")
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @OneToMany(mappedBy = "vehicleType")
    private Set<Vin> vins = new HashSet<>();

    @Column(name = "vehicle_make", nullable = false, columnDefinition = "varchar(25)")
    private String vehicleMake;

    @Column(name = "vehicle_model", nullable = false, columnDefinition = "varchar(20)")
    private String vehicleModel;

    @Column(name = "vehicle_trim", nullable = false, columnDefinition = "varchar(30)")
    private String vehicleTrim;

    @Column(name = "vehicle_year", nullable = false, columnDefinition = "char(4)")
    private String vehicleYear;

    @Column(name = "owners_manual", columnDefinition = "varchar(100)")
    private String ownersManual;
}
