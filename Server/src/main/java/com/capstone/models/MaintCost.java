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

    public MaintCost() {
    }

    public MaintCost(VehicleType vehicleTypeId, String maintTitle, String maintDesc) {
        this.vehicleTypeId = vehicleTypeId;
        this.maintTitle = maintTitle;
        this.maintDesc = maintDesc;
    }

    public MaintCost(Long maintCostId, VehicleType vehicleTypeId, String maintTitle, String maintDesc,
            int independentAvg, int independentHigh, int independentLow, int dealerAvg, int dealerHigh,
            int dealerLow) {
        this.maintCostId = maintCostId;
        this.vehicleTypeId = vehicleTypeId;
        this.maintTitle = maintTitle;
        this.maintDesc = maintDesc;
        this.independentAvg = independentAvg;
        this.independentHigh = independentHigh;
        this.independentLow = independentLow;
        this.dealerAvg = dealerAvg;
        this.dealerHigh = dealerHigh;
        this.dealerLow = dealerLow;
    }

    public Long getMaintCostId() {
        return maintCostId;
    }

    public void setMaintCostId(Long maintCostId) {
        this.maintCostId = maintCostId;
    }

    public VehicleType getVehicleTypeId() {
        return vehicleTypeId;
    }

    public void setVehicleTypeId(VehicleType vehicleTypeId) {
        this.vehicleTypeId = vehicleTypeId;
    }

    public String getMaintTitle() {
        return maintTitle;
    }

    public void setMaintTitle(String maintTitle) {
        this.maintTitle = maintTitle;
    }

    public String getMaintDesc() {
        return maintDesc;
    }

    public void setMaintDesc(String maintDesc) {
        this.maintDesc = maintDesc;
    }

    public int getIndependentAvg() {
        return independentAvg;
    }

    public void setIndependentAvg(int independentAvg) {
        this.independentAvg = independentAvg;
    }

    public int getIndependentHigh() {
        return independentHigh;
    }

    public void setIndependentHigh(int independentHigh) {
        this.independentHigh = independentHigh;
    }

    public int getIndependentLow() {
        return independentLow;
    }

    public void setIndependentLow(int independentLow) {
        this.independentLow = independentLow;
    }

    public int getDealerAvg() {
        return dealerAvg;
    }

    public void setDealerAvg(int dealerAvg) {
        this.dealerAvg = dealerAvg;
    }

    public int getDealerHigh() {
        return dealerHigh;
    }

    public void setDealerHigh(int dealerHigh) {
        this.dealerHigh = dealerHigh;
    }

    public int getDealerLow() {
        return dealerLow;
    }

    public void setDealerLow(int dealerLow) {
        this.dealerLow = dealerLow;
    }

}
