package com.capstone.models;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "reminder")
public class Reminder {

    @Id
    @GeneratedValue
    private long reminderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "vin_id", nullable = false)
    private Vin vin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_record_id")
    private MaintenanceRecord maintenanceRecord;

    @Column(name = "vin_id",
            nullable = false,
            columnDefinition = "integer")
    private long vinId;

    @Column(name = "maintenance_record_id",
            nullable = true,
            columnDefinition = "integer")
    private long maintenanceRecordId;

    @Column(name = "description",
            nullable = false,
            columnDefinition = "text")
    private String description;

    @Column(name = "reminder_date",
            nullable = false,
            columnDefinition = "date")
    private LocalDate reminderDate;


    public Reminder() {
    }

    public Reminder(long vinId, long maintenanceRecordId, String description, LocalDate reminderDate) {
        this.vinId = vinId;
        this.maintenanceRecordId = maintenanceRecordId;
        this.description = description;
        this.reminderDate = reminderDate;
    }

    public long getReminderId() {
        return reminderId;
    }

    public void setReminderId(long reminderId) {
        this.reminderId = reminderId;
    }

    public long getVinId() {
        return vinId;
    }

    public void setVinId(long vinId) {
        this.vinId = vinId;
    }

    public long getMaintenanceRecordId() {
        return maintenanceRecordId;
    }

    public void setMaintenanceRecordId(long maintenanceRecordId) {
        this.maintenanceRecordId = maintenanceRecordId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getReminderDate() {
        return reminderDate;
    }

    public void setReminderDate(LocalDate reminderDate) {
        this.reminderDate = reminderDate;
    }

    public boolean isInvalid() {
        return reminderDate == null || vinId <= 0 || description == null || description.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reminder reminder = (Reminder) o;
        return getVinId() == reminder.getVinId() && getMaintenanceRecordId() == reminder.getMaintenanceRecordId() && Objects.equals(getDescription(), reminder.getDescription()) && Objects.equals(getReminderDate(), reminder.getReminderDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVinId(), getMaintenanceRecordId(), getDescription(), getReminderDate());
    }

    public Vin getVin() {
        return vin;
    }

    public void setVin(Vin vin) {
        this.vin = vin;
    }

    public MaintenanceRecord getMaintenanceRecord() {
        return maintenanceRecord;
    }

    public void setMaintenanceRecord(MaintenanceRecord maintenanceRecord) {
        this.maintenanceRecord = maintenanceRecord;
    }
}
