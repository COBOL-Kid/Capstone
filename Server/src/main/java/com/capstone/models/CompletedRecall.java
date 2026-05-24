package com.capstone.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "completed_recall",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "vin_num", "recall_id"}))
public class CompletedRecall {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "completed_recall_id")
  private Long completedRecallId;

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
  @JoinColumn(name = "recall_id", nullable = false)
  private Recall recall;

  @Column(name = "completed_date", nullable = false)
  private LocalDate completedDate;

  @Column(name = "repair_shop", columnDefinition = "varchar(80)")
  private String repairShop;

  @Column(name = "cost")
  private Double cost;

  @Column(name = "notes", columnDefinition = "text")
  private String notes;

  public CompletedRecall() {}

  public CompletedRecall(UserVin userVin, Recall recall, LocalDate completedDate) {
    this.userVin = userVin;
    this.recall = recall;
    this.completedDate = completedDate;
  }

  public CompletedRecall(
      Long completedRecallId,
      UserVin userVin,
      Recall recall,
      LocalDate completedDate,
      String repairShop,
      Double cost,
      String notes) {
    this.completedRecallId = completedRecallId;
    this.userVin = userVin;
    this.recall = recall;
    this.completedDate = completedDate;
    this.repairShop = repairShop;
    this.cost = cost;
    this.notes = notes;
  }

  public Long getCompletedRecallId() {
    return completedRecallId;
  }

  public void setCompletedRecallId(Long completedRecallId) {
    this.completedRecallId = completedRecallId;
  }

  public UserVin getUserVin() {
    return userVin;
  }

  public void setUserVin(UserVin userVin) {
    this.userVin = userVin;
  }

  public Recall getRecall() {
    return recall;
  }

  public void setRecall(Recall recall) {
    this.recall = recall;
  }

  public LocalDate getCompletedDate() {
    return completedDate;
  }

  public String getRepairShop() {
    return repairShop;
  }

  public void setRepairShop(String repairShop) {
    this.repairShop = repairShop;
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
