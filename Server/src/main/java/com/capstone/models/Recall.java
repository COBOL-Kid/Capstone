package com.capstone.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "recall",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"vehicle_type_id", "nhtsa_campaign_number"}))
public class Recall {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "recall_id")
  private Long recallId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id", nullable = false)
  private VehicleType vehicleTypeId;

  @Column(name = "nhtsa_campaign_number", nullable = false, columnDefinition = "varchar(20)")
  private String nhtsaCampaignNumber;

  @Column(name = "recall_no", columnDefinition = "varchar(20)")
  private String recallNo;

  @Column(name = "report_received_date", nullable = false)
  private LocalDate reportReceivedDate;

  @Column(name = "component", nullable = false, columnDefinition = "varchar(255)")
  private String component;

  @Column(name = "summary", nullable = false, columnDefinition = "text")
  private String summary;

  @Column(name = "consequence", nullable = false, columnDefinition = "text")
  private String consequence;

  @Column(name = "remedy", nullable = false, columnDefinition = "text")
  private String remedy;

  @Column(name = "notes", columnDefinition = "text")
  private String notes;

  @Column(name = "manufacturer", columnDefinition = "varchar(120)")
  private String manufacturer;

  @Column(name = "park_it", nullable = false)
  private boolean parkIt;

  @Column(name = "park_outside", nullable = false)
  private boolean parkOutside;

  @Column(name = "over_the_air_update", nullable = false)
  private boolean overTheAirUpdate;

  @Column(name = "model_year", columnDefinition = "char(4)")
  private String modelYear;

  @Column(name = "make", columnDefinition = "varchar(60)")
  private String make;

  @Column(name = "model", columnDefinition = "varchar(80)")
  private String model;

  public Recall() {}

  public Recall(
      VehicleType vehicleTypeId,
      String nhtsaCampaignNumber,
      LocalDate reportReceivedDate,
      String component,
      String summary,
      String consequence,
      String remedy) {
    this.vehicleTypeId = vehicleTypeId;
    this.nhtsaCampaignNumber = nhtsaCampaignNumber;
    this.reportReceivedDate = reportReceivedDate;
    this.component = component;
    this.summary = summary;
    this.consequence = consequence;
    this.remedy = remedy;
  }

  public Recall(
      Long recallId,
      VehicleType vehicleTypeId,
      String campaignId,
      String recallNo,
      LocalDate recallDate,
      String componentAffected,
      String summary,
      String consequences,
      String remedy,
      String notes,
      String manufacturerName) {
    this(vehicleTypeId, campaignId, recallDate, componentAffected, summary, consequences, remedy);
    this.recallId = recallId;
    this.recallNo = recallNo;
    this.notes = notes;
    this.manufacturer = manufacturerName;
  }

  public Long getRecallId() {
    return recallId;
  }

  public void setRecallId(Long recallId) {
    this.recallId = recallId;
  }

  public VehicleType getVehicleTypeId() {
    return vehicleTypeId;
  }

  public void setVehicleTypeId(VehicleType vehicleTypeId) {
    this.vehicleTypeId = vehicleTypeId;
  }

  public String getNhtsaCampaignNumber() {
    return nhtsaCampaignNumber;
  }

  public void setNhtsaCampaignNumber(String nhtsaCampaignNumber) {
    this.nhtsaCampaignNumber = nhtsaCampaignNumber;
  }

  public String getCampaignId() {
    return nhtsaCampaignNumber;
  }

  public void setCampaignId(String campaignId) {
    this.nhtsaCampaignNumber = campaignId;
  }

  public String getRecallNo() {
    return recallNo;
  }

  public void setRecallNo(String recallNo) {
    this.recallNo = recallNo;
  }

  public LocalDate getReportReceivedDate() {
    return reportReceivedDate;
  }

  public void setReportReceivedDate(LocalDate reportReceivedDate) {
    this.reportReceivedDate = reportReceivedDate;
  }

  public LocalDate getRecallDate() {
    return reportReceivedDate;
  }

  public void setRecallDate(LocalDate recallDate) {
    this.reportReceivedDate = recallDate;
  }

  public String getComponent() {
    return component;
  }

  public void setComponent(String component) {
    this.component = component;
  }

  public String getComponentAffected() {
    return component;
  }

  public void setComponentAffected(String componentAffected) {
    this.component = componentAffected;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getConsequence() {
    return consequence;
  }

  public void setConsequence(String consequence) {
    this.consequence = consequence;
  }

  public String getConsequences() {
    return consequence;
  }

  public void setConsequences(String consequences) {
    this.consequence = consequences;
  }

  public String getRemedy() {
    return remedy;
  }

  public void setRemedy(String remedy) {
    this.remedy = remedy;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public String getManufacturerName() {
    return manufacturer;
  }

  public void setManufacturerName(String manufacturerName) {
    this.manufacturer = manufacturerName;
  }

  public boolean isParkIt() {
    return parkIt;
  }

  public void setParkIt(boolean parkIt) {
    this.parkIt = parkIt;
  }

  public boolean isParkOutside() {
    return parkOutside;
  }

  public void setParkOutside(boolean parkOutside) {
    this.parkOutside = parkOutside;
  }

  public boolean isOverTheAirUpdate() {
    return overTheAirUpdate;
  }

  public void setOverTheAirUpdate(boolean overTheAirUpdate) {
    this.overTheAirUpdate = overTheAirUpdate;
  }

  public String getModelYear() {
    return modelYear;
  }

  public void setModelYear(String modelYear) {
    this.modelYear = modelYear;
  }

  public String getMake() {
    return make;
  }

  public void setMake(String make) {
    this.make = make;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }
}
