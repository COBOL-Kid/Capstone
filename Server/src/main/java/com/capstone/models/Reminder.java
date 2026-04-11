package com.capstone.models;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "reminder")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reminderId;

    @Column(name = "vin_id", nullable = false, columnDefinition = "integer")
    private Long vinId;

    @Column(name = "maintenance_record_id", nullable = true, columnDefinition = "integer")
    private Long maintenanceRecordId;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "reminder_date", nullable = false, columnDefinition = "date")
    private LocalDate reminderDate;

    public Reminder() {
    }

    public Reminder(Long vinId, Long maintenanceRecordId, String description, LocalDate reminderDate) {
        this.vinId = vinId;
        this.maintenanceRecordId = maintenanceRecordId;
        this.description = description;
        this.reminderDate = reminderDate;
    }

    public Long getReminderId() {
        return reminderId;
    }

    public void setReminderId(Long reminderId) {
        this.reminderId = reminderId;
    }

    public Long getVinId() {
        return vinId;
    }

    public void setVinId(Long vinId) {
        this.vinId = vinId;
    }

    public Long getMaintenanceRecordId() {
        return maintenanceRecordId;
    }

    public void setMaintenanceRecordId(Long maintenanceRecordId) {
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Reminder reminder = (Reminder) o;
        return Objects.equals(getVinId(), reminder.getVinId())
                && Objects.equals(getMaintenanceRecordId(), reminder.getMaintenanceRecordId())
                && Objects.equals(getDescription(), reminder.getDescription())
                && Objects.equals(getReminderDate(), reminder.getReminderDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVinId(), getMaintenanceRecordId(), getDescription(), getReminderDate());
    }
}
