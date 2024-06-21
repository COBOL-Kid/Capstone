package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.data.ReminderRepositoryJPA;
import com.capstone.models.Reminder;
import com.capstone.models.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReminderService {

    ReminderRepositoryJPA reminderRepository;
    MaintenanceRecordRepositoryJPA maintenanceRecordRepository;

    @Autowired
    public ReminderService(ReminderRepositoryJPA reminderRepository, MaintenanceRecordRepositoryJPA maintenanceRecordRepository) {
        this.reminderRepository = reminderRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    Result<Reminder> getReminderByVinID(long vinId) {
        return null;
    }
}
