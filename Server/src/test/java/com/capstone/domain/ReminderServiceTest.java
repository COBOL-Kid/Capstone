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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void validUpdateReminderTest() {
        Reminder oldReminder = new Reminder(1, 1, "description", LocalDate.of(2025, 8, 18));
        Reminder newReminder = new Reminder(2, 2, "new description", LocalDate.of(2027, 8, 18));
        when(reminderRepository.findById(any())).thenReturn(Optional.of(oldReminder));
        when(reminderRepository.save(oldReminder)).thenReturn(newReminder);

        Result<Reminder> result = reminderService.updateReminder(newReminder);

        assertEquals(newReminder, result.getPayload());
        assertTrue(result.isSuccess());
    }

    @Test
    void invalidUpdateReminderBecauseReminderIsNullTest() {
        Result<Reminder> result = reminderService.updateReminder(null);
        assertTrue(result.getErrors().contains("Reminder is null"));
    }

    @Test
    void invalidUpdateReminderBecauseReminderIsInvalidTest() {
        Reminder newReminder = Mockito.mock(Reminder.class);
        when(newReminder.isInvalid()).thenReturn(true);

        Result<Reminder> result = reminderService.updateReminder(newReminder);

        assertTrue(result.getErrors().contains("Reminder is invalid"));
    }

    @Test
    void invalidUpdateReminderBecauseDateIsNotInFutureTest() {
        Reminder newReminder = new Reminder(1, 1, "description", LocalDate.of(1999, 12, 12));

        when(reminderRepository.getReferenceById(any())).thenReturn(newReminder);

        Result<Reminder> result = reminderService.updateReminder(newReminder);

        assertTrue(result.getErrors().contains("Reminder can only be in the future"));
    }
}
