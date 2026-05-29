package com.capstone.models;

import jakarta.persistence.*;

@Entity
@Table(
    name = "misc_maint_cost",
    uniqueConstraints = @UniqueConstraint(columnNames = {"vehicle_type_id", "maint_title"}))
public class MiscMaintCost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "misc_maint_cost_id")
  private Long miscMaintCostId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id")
  private VehicleType vehicleTypeId;

  @Column(name = "maint_title", nullable = false, columnDefinition = "varchar(120)")
  private String maintTitle;

  @Column(name = "maint_desc", columnDefinition = "text")
  private String maintDesc;

  @Column(name = "independent_avg", columnDefinition = "int")
  private Integer independentAvg;

  @Column(name = "independent_high", columnDefinition = "int")
  private Integer independentHigh;

  @Column(name = "independent_low", columnDefinition = "int")
  private Integer independentLow;

  @Column(name = "dealer_avg", columnDefinition = "int")
  private Integer dealerAvg;

  @Column(name = "dealer_high", columnDefinition = "int")
  private Integer dealerHigh;

  @Column(name = "dealer_low", columnDefinition = "int")
  private Integer dealerLow;

  public MiscMaintCost() {}

  public MiscMaintCost(VehicleType vehicleTypeId, String maintTitle, String maintDesc) {
    this.vehicleTypeId = vehicleTypeId;
    this.maintTitle = maintTitle;
    this.maintDesc = maintDesc;
  }

  public MiscMaintCost(
      Long miscMaintCostId,
      VehicleType vehicleTypeId,
      String maintTitle,
      String maintDesc,
      Integer independentAvg,
      Integer independentHigh,
      Integer independentLow,
      Integer dealerAvg,
      Integer dealerHigh,
      Integer dealerLow) {
    this.miscMaintCostId = miscMaintCostId;
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

  public Long getMiscMaintCostId() {
    return miscMaintCostId;
  }

  public void setMiscMaintCostId(Long miscMaintCostId) {
    this.miscMaintCostId = miscMaintCostId;
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

  public Integer getIndependentAvg() {
    return independentAvg;
  }

  public void setIndependentAvg(Integer independentAvg) {
    this.independentAvg = independentAvg;
  }

  public Integer getIndependentHigh() {
    return independentHigh;
  }

  public void setIndependentHigh(Integer independentHigh) {
    this.independentHigh = independentHigh;
  }

  public Integer getIndependentLow() {
    return independentLow;
  }

  public void setIndependentLow(Integer independentLow) {
    this.independentLow = independentLow;
  }

  public Integer getDealerAvg() {
    return dealerAvg;
  }

  public void setDealerAvg(Integer dealerAvg) {
    this.dealerAvg = dealerAvg;
  }

  public Integer getDealerHigh() {
    return dealerHigh;
  }

  public void setDealerHigh(Integer dealerHigh) {
    this.dealerHigh = dealerHigh;
  }

  public Integer getDealerLow() {
    return dealerLow;
  }

  public void setDealerLow(Integer dealerLow) {
    this.dealerLow = dealerLow;
  }
}
