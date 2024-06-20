package com.capstone.data;

import com.capstone.models.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepositoryJPA extends JpaRepository<Reminder, Long> {
}
