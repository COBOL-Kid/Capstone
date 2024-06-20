package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.models.MaintenanceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaintenanceRecordService {

    MaintenanceRecordRepositoryJPA repository;

    public MaintenanceRecordService(MaintenanceRecordRepositoryJPA repository) {
        this.repository = repository;
    }

    List<MaintenanceRecord> findAllByVinId(long vinId) {
        return repository.findAllByVinId(vinId);
    }
}
