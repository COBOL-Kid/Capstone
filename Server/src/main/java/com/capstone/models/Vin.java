package com.capstone.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vin")
public class Vin {

    @Id
    @Column(name = "vin_num", nullable = false, columnDefinition = "char(17)")
    private String vin;

    @Column(name = "vin_mileage", nullable = false, columnDefinition = "integer")
    private int vinMileage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;

    @ManyToMany(mappedBy = "userVins")
    private Set<User> users = new HashSet<>();

    // @Column(name = vehicleImage, columnDefinition = ?)

}
