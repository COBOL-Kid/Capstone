package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.MaintenanceRecord;
import com.capstone.models.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class MaintenanceRecordServiceTest {

    @Autowired
    MaintenanceRecordService maintenanceRecordService;

    @MockBean
    MaintenanceRecordRepositoryJPA maintenanceRecordRepositoryJPA;
    @MockBean
    VinRepositoryJPA vinRepositoryJPA;

    private MaintenanceRecord makeValidMaintenanceRecord() {
        MaintenanceRecord maintenanceRecord = new MaintenanceRecord(1, "description", "notes", null, 1, 1, 1.0);
        return maintenanceRecord;
    }

    @Test
    void findAllMaintenanceRecordsByVinId() {
        MaintenanceRecord expected = makeValidMaintenanceRecord();
        when(maintenanceRecordRepositoryJPA.findAllByVinId(expected.getVinId())).thenReturn(List.of(expected));
        assertEquals(expected, maintenanceRecordService.findAllMaintenanceRecordsByVinId(expected.getVinId()).get(0));
    }

    @Test
    void shouldNotFindNonExistentVinId() {
        when(maintenanceRecordRepositoryJPA.findAllByVinId(1)).thenReturn(List.of());
        assertEquals(0, maintenanceRecordService.findAllMaintenanceRecordsByVinId(1).size());
    }

    @Test
    void createMaintenanceRecord_withNullData_returnsFailure() {
        MaintenanceRecord maintenanceRecord = null;
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(maintenanceRecord);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("Maintenance record is null"));
    }

    @Test
    void createMaintenanceRecord_withInvalidData_returnsFailure() {
        MaintenanceRecord maintenanceRecord = new MaintenanceRecord();
        when(maintenanceRecordRepositoryJPA.findById(maintenanceRecord.getVinId())).thenReturn(Optional.of(maintenanceRecord));
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(maintenanceRecord);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("Maintenance record contains null or invalid values"));
    }

    @Test
    void createMaintenanceRecord_withNonExistentVinId_returnsFailure() {
        MaintenanceRecord maintenanceRecord = makeValidMaintenanceRecord();
        when(maintenanceRecordRepositoryJPA.findById(maintenanceRecord.getVinId())).thenReturn(Optional.empty());
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(maintenanceRecord);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("VIN number does not exist"));
    }

    @Test
    void createMaintenanceRecord_withValidData_returnsSuccess() {
        MaintenanceRecord maintenanceRecord = makeValidMaintenanceRecord();
        when(maintenanceRecordRepositoryJPA.findById(maintenanceRecord.getVinId())).thenReturn(Optional.of(maintenanceRecord));
        when(maintenanceRecordRepositoryJPA.save(maintenanceRecord)).thenReturn(maintenanceRecord);
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(maintenanceRecord);
        assertTrue(result.isSuccess());
        assertEquals(maintenanceRecord, result.getPayload());
    }


}