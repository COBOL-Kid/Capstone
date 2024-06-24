package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.data.ReminderRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.Reminder;
import com.capstone.models.Result;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ReminderService {

    ReminderRepositoryJPA reminderRepository;
    MaintenanceRecordRepositoryJPA maintenanceRecordRepository;
    VinRepositoryJPA vinRepository;

    @Autowired
    public ReminderService(ReminderRepositoryJPA reminderRepository, MaintenanceRecordRepositoryJPA maintenanceRecordRepository, VinRepositoryJPA vinRepository) {
        this.reminderRepository = reminderRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.vinRepository = vinRepository;
    }

    public List<Reminder> getAllRemindersByVinID(Long vinId) {
        return reminderRepository.getRemindersByVinIdMatches(vinId);
    }

    //TODO figure out how to get list of emails that need to be sent every day
//    public List<Reminder> getUpcomingReminders(Long vinId) {
//        List<Reminder> allReminders = getAllRemindersByVinID(vinId);
//        if (allReminders.isEmpty()) {
//            return List.of();
//        }
//        Optional<Vin> remindersVin = vinRepository.findById(vinId);
//        if (remindersVin.isEmpty()) {
//            return
//        }
//        List<Reminder> upcomingReminders = new ArrayList<>();
//        LocalDate currentDate = LocalDate.now();
//        for (Reminder reminder : allReminders) {
//            if (reminder.getReminderDate().isAfter(currentDate)) {
//                upcomingReminders.add(reminder);
//            }
//        }
//        return upcomingReminders;
//    }

    @Transactional
    public Result<Reminder> createReminder(Reminder reminder) {
        Result<Reminder> result = validateReminder(reminder);
        if (!result.isSuccess()) {
            return result;
        }
        result = reminderInFuture(reminder);
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

    @Transactional
    public Result<Reminder> updateReminder(Reminder incomingReminder) {
        Result<Reminder> result = validateReminder(incomingReminder);
        if (!result.isSuccess()) {
            return result;
        }
        result = reminderInFuture(incomingReminder);
        if (!result.isSuccess()) {
            return result;
        }
        Optional<Reminder> existingReminderOpt = reminderRepository.findById(incomingReminder.getReminderId());
        if (existingReminderOpt.isEmpty()) {
            result.addError("Reminder not found");
            return result;
        }
        Reminder existingReminder = existingReminderOpt.get();
        existingReminder.setVinId(incomingReminder.getVinId());
        existingReminder.setMaintenanceRecordId(incomingReminder.getMaintenanceRecordId());
        existingReminder.setDescription(incomingReminder.getDescription());
        existingReminder.setReminderDate(incomingReminder.getReminderDate());
        try {
            result.setPayload(reminderRepository.save(existingReminder));
        } catch (DataIntegrityViolationException e) {
            result.addError("DataIntegrityViolationException");
        } catch (JpaSystemException e) {
            result.addError("JpaSystemException");
        }
        return result;
    }

    @Transactional
    public Result<Reminder> deleteReminder(Long reminderId) {
        Result<Reminder> result = reminderExists(reminderId);
        if (!result.isSuccess()) {
            result.addError("Reminder not found");
            return result;
        }
        try {
            reminderRepository.deleteById(reminderId);
        } catch (EmptyResultDataAccessException e) {
            result.addError("EmptyResultDataAccessException");
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
        return result;
    }

    private Result<Reminder> reminderInFuture(Reminder reminder) {
        Result<Reminder> result = new Result<>();
        if (!reminder.getReminderDate().isAfter(LocalDate.now())) {
            result.addError("Reminder can only be in the future");
        }
        return result;
    }

    private Result<Reminder> reminderExists(Long reminderId) {
        Result<Reminder> result = new Result<>();
        Optional<Reminder> existingReminderOpt = reminderRepository.findById(reminderId);
        if (existingReminderOpt.isEmpty()) {
            result.addError("Reminder not found");
            return result;
        }
        return result;
    }
}
