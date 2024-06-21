
package com.capstone.data;

import com.capstone.models.Reminder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReminderRepositoryJPATest {

    @Autowired
    private ReminderRepositoryJPA reminderRepositoryJPA;

    @Test
    void getReminderByVinIdMatches() {
        Reminder reminder = new Reminder();
        reminder.setVinId(100L);
        reminder.setMaintenanceRecordId(101L);
        reminder.setDescription("Test Reminder");
        reminder.setReminderDate(LocalDate.now());
        reminder = reminderRepositoryJPA.save(reminder);

        Optional<Reminder> fetchedReminder = reminderRepositoryJPA.getReminderByVinIdMatches(100L);
        assertTrue(fetchedReminder.isPresent());
        assertEquals(100L, fetchedReminder.get().getVinId());
    }

    @Test
    void getReminderByNonexistentVinId() {
        Optional<Reminder> fetchedReminder = reminderRepositoryJPA.getReminderByVinIdMatches(9999L);
        assertFalse(fetchedReminder.isPresent());
    }
}

