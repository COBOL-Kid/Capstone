package com.capstone.data;

import com.capstone.models.Reminder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ReminderRepositoryJPATest {

    @Autowired
    private ReminderRepositoryJPA reminderRepositoryJPA;

    @BeforeEach
    public void setUp() {
        reminderRepositoryJPA.deleteAll();
        Reminder reminder = new Reminder();
        reminder.setVinId(1L);
        reminder.setReminderId(1L);
        reminder.setReminderDate(LocalDate.now());
        reminder.setDescription("test");
        reminder.setMaintenanceRecordId(1L);
        reminderRepositoryJPA.save(reminder);
    }

    @Test
    void testGetRemindersByVinIdMatches() {
        Long testVinId = 1L;
        List<Reminder> reminderList = reminderRepositoryJPA.getRemindersByVinIdMatches(testVinId);
        assertFalse(reminderList.isEmpty());
    }

    @Test
    void testGetRemindersByVinIdMatchesEmpty() {
        Long testVinId = 9999L;
        List<Reminder> reminderList = reminderRepositoryJPA.getRemindersByVinIdMatches(testVinId);
        assertTrue(reminderList.isEmpty());
    }
}

