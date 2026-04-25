package com.capstone.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "MAINT_MILEAGE")
public class MaintMileage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maint_mileage_id")
    private Long maintMileageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleTypeId;

    @Column(name = "mileage_due", nullable = false, columnDefinition = "int")
    private int mileageDue;

    @Column(name = "maint_desc", nullable = false, columnDefinition = "varchar(50)")
    private String maintDesc;
}
