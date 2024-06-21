package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.data.ReminderRepositoryJPA;
import com.capstone.models.Reminder;
import com.capstone.models.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReminderService {

    ReminderRepositoryJPA reminderRepository;
    MaintenanceRecordRepositoryJPA maintenanceRecordRepository;

    @Autowired
    public ReminderService(ReminderRepositoryJPA reminderRepository, MaintenanceRecordRepositoryJPA maintenanceRecordRepository) {
        this.reminderRepository = reminderRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    public List<Reminder> getReminderByVinID(long vinId) {
        return reminderRepository.getRemindersByVinIdMatches(vinId);
    }

    public Result<Reminder> createReminder(Reminder reminder) {
        Result<Reminder> result = validateReminder(reminder);
        if (!result.isSuccess()) {
            return result;
        }
        reminderInFuture(result);
        if (!result.isSuccess()) {
            return result;
        }
        try {
            result.setPayload(reminderRepository.save(reminder));
        } catch (DataIntegrityViolationException e) {
            result.addError("DataIntegrityViolationException");
        } catch (JpaSystemException e) {
            result.addError("JpaSystemException");
        }
        return result;
    }

    private Result<Reminder> validateReminder(Reminder reminder) {
        Result<Reminder> result = new Result<>();
        if (reminder == null) {
            result.addError("Reminder is null");
            return result;
        }
        if (reminder.isInvalid()) {
            result.addError("Reminder is invalid");
            return result;
        }
        result.setPayload(reminder);
        return result;
    }

    private void reminderInFuture(Result<Reminder> result) {
        if (!result.getPayload().getReminderDate().isAfter(LocalDate.now())) {
            result.addError("Reminder can only be in the future");
        }
    }
}
