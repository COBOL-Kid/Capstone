package com.capstone.controllers;

import com.capstone.domain.MaintenanceRecordService;
import com.capstone.models.MaintenanceRecord;
import com.capstone.models.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<?> addMaintenanceRecord(@RequestBody MaintenanceRecord maintenanceRecord) {
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(maintenanceRecord);
        if (result.isSuccess()) {
            return new ResponseEntity<>(result.getPayload(), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(result.getErrors(), HttpStatus.BAD_REQUEST);
        }
    }
}
