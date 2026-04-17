package com.capstone.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

import com.capstone.data.ReminderRepositoryJPA;
import com.capstone.models.Reminder;
import com.capstone.models.Result;

import jakarta.transaction.Transactional;

@Service
public class ReminderService {

    ReminderRepositoryJPA reminderRepository;

    public ReminderService(ReminderRepositoryJPA reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    public List<Reminder> getAllRemindersByVinID(Long vinId) {
        return reminderRepository.getRemindersByVinIdMatches(vinId);
    }

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
