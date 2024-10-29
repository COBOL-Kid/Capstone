package com.capstone.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.domain.MaintenanceRecordService;
import com.capstone.models.MaintenanceRecord;
import com.capstone.models.Result;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceRecordService maintenanceRecordService;

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

    @PutMapping
    public ResponseEntity<?> updateMaintenanceRecord(@RequestBody MaintenanceRecord maintenanceRecord) {
        Result<MaintenanceRecord> record = maintenanceRecordService.updateMaintenanceRecordDateCompleted(maintenanceRecord);
        if (record.isSuccess()) {
            return new ResponseEntity<>(record.getPayload(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(record.getErrors(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/update_maintenance_records")
    public ResponseEntity<?> updateMaintenanceRecords(@RequestBody List<MaintenanceRecord> maintenanceRecords) {
        Result<MaintenanceRecord> result = maintenanceRecordService.createNotYetExistentMaintenanceRecords(maintenanceRecords);
        if (!result.isSuccess()) {
            return new ResponseEntity<>(result.getErrors(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{maintenanceRecordId}")
    public ResponseEntity<?> deleteMaintenanceRecord(@PathVariable Long maintenanceRecordId) {
        Result<MaintenanceRecord> result = maintenanceRecordService.deleteMaintenanceRecord(maintenanceRecordId);
        if (result.isSuccess()) {
            return new ResponseEntity<>(result.getPayload(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(result.getErrors(), HttpStatus.BAD_REQUEST);
        }
    }

}
