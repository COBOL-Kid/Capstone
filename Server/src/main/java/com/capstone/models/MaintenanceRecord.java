package com.capstone.models;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "maintenance_record")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long maintenanceRecordId;

    @Column(name = "vin_id",
            nullable = false,
            columnDefinition = "integer")
    private Long vinId;

    @Column(name = "description",
            nullable = false,
            columnDefinition = "text")
    private String description;

    @Column(name = "date_completed",
            nullable = true,
            columnDefinition = "date")
    private LocalDate dateCompleted;

    @Column(name = "mileage_due",
            nullable = false,
            columnDefinition = "int")
    private int mileageDue;

    @Column(name = "cost",
            nullable = true,
            columnDefinition = "numeric"
    )
    private double cost;


    public MaintenanceRecord() {
    }

    public MaintenanceRecord(Long vinId, String description, LocalDate dateCompleted, int mileageDue, double cost) {
        this.vinId = vinId;
        this.description = description;
        this.dateCompleted = dateCompleted;
        this.mileageDue = mileageDue;
        this.cost = cost;
    }

    public Long getMaintenanceRecordId() {
        return maintenanceRecordId;
    }

    public void setMaintenanceRecordId(Long maintenanceRecordId) {
        this.maintenanceRecordId = maintenanceRecordId;
    }

    public Long getVinId() {
        return vinId;
    }

    public void setVinId(Long vinId) {
        this.vinId = vinId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(LocalDate dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public int getMileageDue() {
        return mileageDue;
    }

    public void setMileageDue(int mileageDue) {
        this.mileageDue = mileageDue;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public boolean isInvalid() {
        return vinId <= 0 || description == null || description.isEmpty() || mileageDue <= 0;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaintenanceRecord that = (MaintenanceRecord) o;
        return getMileageDue() == that.getMileageDue() && Double.compare(getCost(), that.getCost()) == 0 && Objects.equals(getVinId(), that.getVinId()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getDateCompleted(), that.getDateCompleted());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVinId(), getDescription(), getDateCompleted(), getMileageDue(), getCost());
    }
}
