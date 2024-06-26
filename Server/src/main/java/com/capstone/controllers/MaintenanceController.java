package com.capstone.controllers;

import com.capstone.domain.MaintenanceRecordService;
import com.capstone.models.MaintenanceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceRecordService maintenanceRecordService;

    @Autowired
    public MaintenanceController(MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @GetMapping("/{vinId}")
    public ResponseEntity<?> getAllMaintenanceRecordsByVinId(@PathVariable Long vinId) {
        List<MaintenanceRecord> records = maintenanceRecordService.findAllMaintenanceRecordsByVinId(vinId);
        if (records.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(records, HttpStatus.OK);
    }
}
