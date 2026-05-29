package com.capstone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "maint_mileage_summary",
    uniqueConstraints = @UniqueConstraint(columnNames = {"vehicle_type_id", "mileage_due"}))
public class MaintMileageSummary {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "maint_mileage_summary_id")
  private Long maintMileageSummaryId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id", nullable = false)
  private VehicleType vehicleTypeId;

  @Column(name = "mileage_due", nullable = false, columnDefinition = "int")
  private int mileageDue;

  @Column(name = "total_parts_cost", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalPartsCost;

  @Column(name = "total_labor_cost", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalLaborCost;

  @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalCost;

  @Column(name = "currency", nullable = false, columnDefinition = "varchar(3)")
  private String currency;

  public MaintMileageSummary() {}

  public MaintMileageSummary(
      VehicleType vehicleTypeId,
      int mileageDue,
      BigDecimal totalPartsCost,
      BigDecimal totalLaborCost,
      BigDecimal totalCost,
      String currency) {
    this.vehicleTypeId = vehicleTypeId;
    this.mileageDue = mileageDue;
    this.totalPartsCost = totalPartsCost;
    this.totalLaborCost = totalLaborCost;
    this.totalCost = totalCost;
    this.currency = currency;
  }

  public Long getMaintMileageSummaryId() {
    return maintMileageSummaryId;
  }

  public void setMaintMileageSummaryId(Long maintMileageSummaryId) {
    this.maintMileageSummaryId = maintMileageSummaryId;
  }

  public VehicleType getVehicleTypeId() {
    return vehicleTypeId;
  }

  public void setVehicleTypeId(VehicleType vehicleTypeId) {
    this.vehicleTypeId = vehicleTypeId;
  }

  public int getMileageDue() {
    return mileageDue;
  }

  public void setMileageDue(int mileageDue) {
    this.mileageDue = mileageDue;
  }

  public BigDecimal getTotalPartsCost() {
    return totalPartsCost;
  }

  public void setTotalPartsCost(BigDecimal totalPartsCost) {
    this.totalPartsCost = totalPartsCost;
  }

  public BigDecimal getTotalLaborCost() {
    return totalLaborCost;
  }

  public void setTotalLaborCost(BigDecimal totalLaborCost) {
    this.totalLaborCost = totalLaborCost;
  }

  public BigDecimal getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(BigDecimal totalCost) {
    this.totalCost = totalCost;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }
}
