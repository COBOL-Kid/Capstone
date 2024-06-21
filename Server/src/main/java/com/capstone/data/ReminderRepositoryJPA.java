package com.capstone.data;

import com.capstone.models.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReminderRepositoryJPA extends JpaRepository<Reminder, Long> {

    @Query("SELECT r FROM Reminder r WHERE r.vinId =?1")
    List<Reminder> getRemindersByVinIdMatches(long vinId);
}
