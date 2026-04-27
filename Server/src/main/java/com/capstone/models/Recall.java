package com.capstone.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "recall", uniqueConstraints = @UniqueConstraint(columnNames = { "vehicle_type_id", "campaign_id" }))
public class Recall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recall_id")
    private Long recallId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleTypeId;

    @Column(name = "campaign_id", nullable = false, columnDefinition = "varchar(20)")
    private String campaignId;

    @Column(name = "recall_no", columnDefinition = "varchar(20)")
    private String recallNo;

    @Column(name = "recall_date", nullable = false)
    private LocalDate recallDate;

    @Column(name = "component_affected", nullable = false, columnDefinition = "varchar(120)")
    private String componentAffected;

    @Column(name = "summary", nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "consequences", nullable = false, columnDefinition = "text")
    private String consequences;

    @Column(name = "remedy", nullable = false, columnDefinition = "text")
    private String remedy;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "manufacturer_name", columnDefinition = "varchar(120)")
    private String manufacturerName;

    public Recall() {
    }

    public Recall(VehicleType vehicleTypeId, String campaignId, LocalDate recallDate, String componentAffected,
            String summary, String consequences, String remedy) {
        this.vehicleTypeId = vehicleTypeId;
        this.campaignId = campaignId;
        this.recallDate = recallDate;
        this.componentAffected = componentAffected;
        this.summary = summary;
        this.consequences = consequences;
        this.remedy = remedy;
    }

    public Recall(Long recallId, VehicleType vehicleTypeId, String campaignId, String recallNo,
            LocalDate recallDate, String componentAffected, String summary, String consequences, String remedy,
            String notes, String manufacturerName) {
        this.recallId = recallId;
        this.vehicleTypeId = vehicleTypeId;
        this.campaignId = campaignId;
        this.recallNo = recallNo;
        this.recallDate = recallDate;
        this.componentAffected = componentAffected;
        this.summary = summary;
        this.consequences = consequences;
        this.remedy = remedy;
        this.notes = notes;
        this.manufacturerName = manufacturerName;
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

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getRecallNo() {
        return recallNo;
    }

    public void setRecallNo(String recallNo) {
        this.recallNo = recallNo;
    }

    public LocalDate getRecallDate() {
        return recallDate;
    }

    public void setRecallDate(LocalDate recallDate) {
        this.recallDate = recallDate;
    }

    public String getComponentAffected() {
        return componentAffected;
    }

    public void setComponentAffected(String componentAffected) {
        this.componentAffected = componentAffected;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getConsequences() {
        return consequences;
    }

    public void setConsequences(String consequences) {
        this.consequences = consequences;
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

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }
}
