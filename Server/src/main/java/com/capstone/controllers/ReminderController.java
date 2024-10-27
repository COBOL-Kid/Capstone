package com.capstone.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.domain.ReminderService;
import com.capstone.models.Reminder;
import com.capstone.models.Result;

@RestController
@RequestMapping("/api/reminder")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @GetMapping("/{vinId}")
    public ResponseEntity<?> getAllRemindersByVinId(@PathVariable Long vinId) {
        List<Reminder> reminders = reminderService.getAllRemindersByVinID(vinId);
        if (reminders.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(reminders, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> createReminder(@RequestBody Reminder reminder) {
        Result<Reminder> result = reminderService.createReminder(reminder);
        if (result.isSuccess()) {
            return new ResponseEntity<>(result.getPayload(), HttpStatus.CREATED);
        }
        return new ResponseEntity<>(result.getErrors(), HttpStatus.BAD_REQUEST);

    }

    @PutMapping
    public ResponseEntity<?> updateReminder(@RequestBody Reminder reminder) {
        Result<Reminder> result = reminderService.updateReminder(reminder);
        if (result.isSuccess()) {
            return new ResponseEntity<>(result.getPayload(), HttpStatus.OK);
        }
        return new ResponseEntity<>(result.getErrors(), HttpStatus.BAD_REQUEST);
    }

    @DeleteMapping("/delete/{reminderId}")
    public ResponseEntity<?> deleteReminder(@PathVariable Long reminderId) {
        Result<Reminder> result = reminderService.deleteReminder(reminderId);
        if (result.isSuccess()) {
            return new ResponseEntity<>(result.getPayload(), HttpStatus.OK);
        }
        return new ResponseEntity<>(result.getErrors(), HttpStatus.BAD_REQUEST);
    }

}
