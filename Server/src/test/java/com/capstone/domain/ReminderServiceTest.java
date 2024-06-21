package com.capstone.domain;

import com.capstone.data.ReminderRepositoryJPA;
import com.capstone.models.Reminder;
import com.capstone.models.Result;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class ReminderServiceTest {

    @Autowired
    ReminderService reminderService;

    @MockBean
    ReminderRepositoryJPA reminderRepository;


    @Test
    void validGetReminderByVinIDTest() {
        List<Reminder> reminders = new ArrayList<>();
        reminders.add(new Reminder());

        when(reminderService.getReminderByVinID(1)).thenReturn(reminders);

        List<Reminder> result = reminderService.getReminderByVinID(1);

        assertFalse(result.isEmpty());
    }

    @Test
    void invalidGetReminderByVinIDTest() {
        List<Reminder> result = reminderService.getReminderByVinID(-1);
        assertTrue(result.isEmpty());
    }

    @Test
    void validCreateReminderTest() {
        Reminder reminder = new Reminder(1, 1, "description", LocalDate.of(2026, 8, 17));
        when(reminderRepository.save(reminder)).thenReturn(reminder);
        Result<Reminder> result = reminderService.createReminder(reminder);
        assertTrue(result.isSuccess());
        assertEquals(reminder, result.getPayload());
    }

    @Test
    void inValidCreateReminderBecauseReminderIsNullTest() {
        Result<Reminder> result = reminderService.createReminder(null);
        assertTrue(result.getErrors().contains("Reminder is null"));
    }

    @Test
    void invalidCreateReminderBecauseReminderIsInvalidTest() {
        Reminder reminder = Mockito.mock(Reminder.class);
        when(reminder.isInvalid()).thenReturn(true);
        Result<Reminder> result = reminderService.createReminder(reminder);
        assertTrue(result.getErrors().contains("Reminder is invalid"));
    }

    @Test
    void invalidCreateReminderBecauseDateIsNotInFutureTest() {
        Reminder reminder = new Reminder(1, 1, "description", LocalDate.now());
        Result<Reminder> result = reminderService.createReminder(reminder);
        assertTrue(result.getErrors().contains("Reminder can only be in the future"));
    }
}
