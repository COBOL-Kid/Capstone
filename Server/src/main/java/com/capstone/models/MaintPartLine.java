package com.capstone.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "maint_part_line")
public class MaintPartLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "maint_part_line_id")
  private Long maintPartLineId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "maint_mileage_id", nullable = false)
  private MaintMileage maintMileage;

  @Column(name = "part_desc", nullable = false, columnDefinition = "varchar(255)")
  private String partDesc;

  @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalCost;

  @Column(name = "currency", nullable = false, columnDefinition = "varchar(3)")
  private String currency;

  public MaintPartLine() {}

  public MaintPartLine(
      MaintMileage maintMileage, String partDesc, BigDecimal totalCost, String currency) {
    this.maintMileage = maintMileage;
    this.partDesc = partDesc;
    this.totalCost = totalCost;
    this.currency = currency;
  }

  public Long getMaintPartLineId() {
    return maintPartLineId;
  }

  public void setMaintPartLineId(Long maintPartLineId) {
    this.maintPartLineId = maintPartLineId;
  }

  public MaintMileage getMaintMileage() {
    return maintMileage;
  }

  public void setMaintMileage(MaintMileage maintMileage) {
    this.maintMileage = maintMileage;
  }

  public String getPartDesc() {
    return partDesc;
  }

  public void setPartDesc(String partDesc) {
    this.partDesc = partDesc;
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
