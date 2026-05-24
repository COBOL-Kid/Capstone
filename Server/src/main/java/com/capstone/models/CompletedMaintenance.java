package com.capstone.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "completed_maintenance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "vin_num", "maint_mileage_id"}))
public class CompletedMaintenance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "completed_maintenance_id")
  private Long completedMaintenanceId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false),
    @JoinColumn(
        name = "vin_num",
        referencedColumnName = "vin_num",
        nullable = false,
        columnDefinition = "char(17)")
  })
  private UserVin userVin;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "maint_mileage_id", nullable = false)
  private MaintMileage maintMileage;

  @Column(name = "completed_date", nullable = false)
  private LocalDate completedDate;

  @Column(name = "mileage_completed", nullable = false, columnDefinition = "int")
  private int mileageCompleted;

  @Column(name = "cost")
  private Double cost;

  @Column(name = "notes", columnDefinition = "text")
  private String notes;

  public CompletedMaintenance() {}

  public CompletedMaintenance(
      UserVin userVin, MaintMileage maintMileage, LocalDate completedDate, int mileageCompleted) {
    this.userVin = userVin;
    this.maintMileage = maintMileage;
    this.completedDate = completedDate;
    this.mileageCompleted = mileageCompleted;
  }

  public CompletedMaintenance(
      Long completedMaintenanceId,
      UserVin userVin,
      MaintMileage maintMileage,
      LocalDate completedDate,
      int mileageCompleted,
      Double cost,
      String notes) {
    this.completedMaintenanceId = completedMaintenanceId;
    this.userVin = userVin;
    this.maintMileage = maintMileage;
    this.completedDate = completedDate;
    this.mileageCompleted = mileageCompleted;
    this.cost = cost;
    this.notes = notes;
  }

  public Long getCompletedMaintenanceId() {
    return completedMaintenanceId;
  }

  public void setCompletedMaintenanceId(Long completedMaintenanceId) {
    this.completedMaintenanceId = completedMaintenanceId;
  }

  public UserVin getUserVin() {
    return userVin;
  }

  public void setUserVin(UserVin userVin) {
    this.userVin = userVin;
  }

  public MaintMileage getMaintMileage() {
    return maintMileage;
  }

  public void setMaintMileage(MaintMileage maintMileage) {
    this.maintMileage = maintMileage;
  }

  public LocalDate getCompletedDate() {
    return completedDate;
  }

  public void setCompletedDate(LocalDate completedDate) {
    this.completedDate = completedDate;
  }

  public int getMileageCompleted() {
    return mileageCompleted;
  }

  public void setMileageCompleted(int mileageCompleted) {
    this.mileageCompleted = mileageCompleted;
  }

  public Double getCost() {
    return cost;
  }

  public void setCost(Double cost) {
    this.cost = cost;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
