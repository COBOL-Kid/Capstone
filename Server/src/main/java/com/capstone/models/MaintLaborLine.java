package com.capstone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "maint_labor_line")
public class MaintLaborLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "maint_labor_line_id")
  private Long maintLaborLineId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "maint_mileage_id", nullable = false, unique = true)
  private MaintMileage maintMileage;

  @Column(name = "time_required_hours", nullable = false, precision = 6, scale = 2)
  private BigDecimal timeRequiredHours;

  @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
  private BigDecimal hourlyRate;

  @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalCost;

  @Column(name = "currency", nullable = false, columnDefinition = "varchar(3)")
  private String currency;

  public MaintLaborLine() {}

  public MaintLaborLine(
      MaintMileage maintMileage,
      BigDecimal timeRequiredHours,
      BigDecimal hourlyRate,
      BigDecimal totalCost,
      String currency) {
    this.maintMileage = maintMileage;
    this.timeRequiredHours = timeRequiredHours;
    this.hourlyRate = hourlyRate;
    this.totalCost = totalCost;
    this.currency = currency;
  }

  public Long getMaintLaborLineId() {
    return maintLaborLineId;
  }

  public void setMaintLaborLineId(Long maintLaborLineId) {
    this.maintLaborLineId = maintLaborLineId;
  }

  public MaintMileage getMaintMileage() {
    return maintMileage;
  }

  public void setMaintMileage(MaintMileage maintMileage) {
    this.maintMileage = maintMileage;
  }

  public BigDecimal getTimeRequiredHours() {
    return timeRequiredHours;
  }

  public void setTimeRequiredHours(BigDecimal timeRequiredHours) {
    this.timeRequiredHours = timeRequiredHours;
  }

  public BigDecimal getHourlyRate() {
    return hourlyRate;
  }

  public void setHourlyRate(BigDecimal hourlyRate) {
    this.hourlyRate = hourlyRate;
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
