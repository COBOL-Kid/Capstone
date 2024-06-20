package com.capstone.models;

import jakarta.persistence.*;

import java.util.Date;
import java.util.Objects;

@Entity
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long recordId;

    @Column(name = "vin_id",
            nullable = false,
            columnDefinition = "text")
    private long vinId;

    @Column(name = "description",
            nullable = false,
            columnDefinition = "text")
    private String description;

    @Column(name = "notes",
            nullable = true,
            columnDefinition = "text")
    private String notes;

    @Column(name = "date_completed",
            nullable = true,
            columnDefinition = "date")
    private Date dateCompleted;

    @Column(name = "mileage_due",
            nullable = false,
            columnDefinition = "int")
    private int mileageDue;

    @Column(name = "mileage_completed",
            nullable = false,
            columnDefinition = "int")
    private int mileageCompleted;

    @Column(name = "cost",
            nullable = true,
            columnDefinition = "double"
    )
    private double cost;


    public Record() {
    }

    public Record(long recordId, long vinId, String description, String notes, Date dateCompleted, int mileageDue, int mileageCompleted, double cost) {
        this.recordId = recordId;
        this.vinId = vinId;
        this.description = description;
        this.notes = notes;
        this.dateCompleted = dateCompleted;
        this.mileageDue = mileageDue;
        this.mileageCompleted = mileageCompleted;
        this.cost = cost;
    }

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    public long getVinId() {
        return vinId;
    }

    public void setVinId(long vinId) {
        this.vinId = vinId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public int getMileageDue() {
        return mileageDue;
    }

    public void setMileageDue(int mileageDue) {
        this.mileageDue = mileageDue;
    }

    public int getMileageCompleted() {
        return mileageCompleted;
    }

    public void setMileageCompleted(int mileageCompleted) {
        this.mileageCompleted = mileageCompleted;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return getVinId() == record.getVinId() && getMileageDue() == record.getMileageDue() && getMileageCompleted() == record.getMileageCompleted() && Double.compare(getCost(), record.getCost()) == 0 && Objects.equals(getDescription(), record.getDescription()) && Objects.equals(getNotes(), record.getNotes()) && Objects.equals(getDateCompleted(), record.getDateCompleted());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVinId(), getDescription(), getNotes(), getDateCompleted(), getMileageDue(), getMileageCompleted(), getCost());
    }
}
