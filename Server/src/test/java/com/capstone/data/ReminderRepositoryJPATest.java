
package com.capstone.data;

import com.capstone.models.Reminder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReminderRepositoryJPATest {

    @Autowired
    private ReminderRepositoryJPA reminderRepositoryJPA;
    @Test
    void testGetRemindersByVinIdMatches() {
        long testVinId = 1L;
        Optional<List<Reminder>> reminderList = reminderRepositoryJPA.getRemindersByVinIdMatches(testVinId);
        assertNotNull(reminderList);
        assertFalse(reminderList.isEmpty());
    }

    @Test
    void testGetRemindersByVinIdMatchesEmpty() {
        long testVinId = 9999L;
        Optional<List<Reminder>> reminderList = reminderRepositoryJPA.getRemindersByVinIdMatches(testVinId);
        assertNotNull(reminderList);
        assertFalse(reminderList.isEmpty());
    }
}

