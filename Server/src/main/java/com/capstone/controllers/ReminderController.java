package com.capstone.controllers;

import com.capstone.domain.ReminderService;
import com.capstone.models.Reminder;
import com.capstone.models.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminder")
public class ReminderController {

    private final ReminderService reminderService;

    @Autowired
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

    @DeleteMapping("/delete/{reminderId}")
    public ResponseEntity<?> deleteReminder(@PathVariable Long reminderId) {
        Result<Reminder> result = reminderService.deleteReminder(reminderId);
        if (result.isSuccess()) {
            return new ResponseEntity<>(result.getPayload(), HttpStatus.OK);
        }
        return new ResponseEntity<>(result.getErrors(), HttpStatus.BAD_REQUEST);
    }

}
