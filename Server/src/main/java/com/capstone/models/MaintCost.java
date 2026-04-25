package com.capstone.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class MaintCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maint_cost_id")
    private Long maintCostId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleTypeId;

    @Column(name = "maint_title", nullable = false, columnDefinition = "varchar(30)")
    private String maintTitle;

    @Column(name = "maint_desc", nullable = false, columnDefinition = "varchar(50)")
    private String maintDesc;

    @Column(name = "independent_avg", columnDefinition = "int")
    private int independentAvg;

    @Column(name = "independent_high", columnDefinition = "int")
    private int independentHigh;

    @Column(name = "independent_low", columnDefinition = "int")
    private int independentLow;

    @Column(name = "dealer_avg", columnDefinition = "int")
    private int dealerAvg;

    @Column(name = "dealer_high", columnDefinition = "int")
    private int dealerHigh;

    @Column(name = "dealer_low", columnDefinition = "int")
    private int dealerLow;

}
