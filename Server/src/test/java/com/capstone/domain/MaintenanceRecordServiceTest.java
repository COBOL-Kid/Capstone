package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.MaintenanceRecord;
import com.capstone.models.Result;
import com.capstone.models.Vin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
class MaintenanceRecordServiceTest {

    @Autowired
    MaintenanceRecordService maintenanceRecordService;

    @MockitoBean
    MaintenanceRecordRepositoryJPA maintenanceRecordRepositoryJPA;
    @MockitoBean
    VinRepositoryJPA vinRepositoryJPA;

    private MaintenanceRecord makeValidMaintenanceRecord() {
        return new MaintenanceRecord(1L, "description", LocalDate.now(), 1, 1.0);
    }

    @Test
    void findAllMaintenanceRecordsByVinId() {
        MaintenanceRecord expected = makeValidMaintenanceRecord();
        when(maintenanceRecordRepositoryJPA.findAllByVinId(expected.getVinId())).thenReturn(List.of(expected));
        assertEquals(expected, maintenanceRecordService.findAllMaintenanceRecordsByVinId(expected.getVinId()).get(0));
    }

    @Test
    void shouldNotFindNonExistentVinId() {
        when(maintenanceRecordRepositoryJPA.findAllByVinId(1L)).thenReturn(List.of());
        assertEquals(0, maintenanceRecordService.findAllMaintenanceRecordsByVinId(1L).size());
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
        maintenanceRecord.setVinId(1L);  // Set the vinId to ensure it is not null
        when(maintenanceRecordRepositoryJPA.findById(maintenanceRecord.getVinId())).thenReturn(Optional.of(maintenanceRecord));
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(maintenanceRecord);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("Maintenance record contains null or invalid values"));
    }

    @Test
    void createMaintenanceRecord_withNonExistentVinId_returnsFailure() {
        MaintenanceRecord maintenanceRecord = makeValidMaintenanceRecord();
        when(vinRepositoryJPA.findById(maintenanceRecord.getVinId())).thenReturn(Optional.empty());
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(maintenanceRecord);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("VIN number does not exist"));
    }

    @Test
    void createMaintenanceRecord_withValidData_returnsSuccess() {
        MaintenanceRecord maintenanceRecord = makeValidMaintenanceRecord();
        Vin vin = new Vin();
        when(vinRepositoryJPA.findById(maintenanceRecord.getVinId())).thenReturn(Optional.of(vin));
        when(maintenanceRecordRepositoryJPA.save(maintenanceRecord)).thenReturn(maintenanceRecord);
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(maintenanceRecord);
        assertTrue(result.isSuccess());
        assertEquals(maintenanceRecord, result.getPayload());
    }

    @Test
    void shouldThrowDataIntegrityViolationException_whenCreateMaintenanceRecord() {
        MaintenanceRecord record = makeValidMaintenanceRecord();
        Vin vin = new Vin();
        when(vinRepositoryJPA.findById(record.getVinId())).thenReturn(Optional.of(vin));
        when(maintenanceRecordRepositoryJPA.save(record)).thenThrow(DataIntegrityViolationException.class);
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(record);

        assertTrue(result.getErrors().contains("DataIntegrityViolationException"));
    }

    @Test
    void shouldThrowJpaSystemException_whenCreateMaintenanceRecord() {
        MaintenanceRecord record = makeValidMaintenanceRecord();
        Vin vin = new Vin();
        when(vinRepositoryJPA.findById(record.getVinId())).thenReturn(Optional.of(vin));
        when(maintenanceRecordRepositoryJPA.save(record)).thenThrow(JpaSystemException.class);
        Result<MaintenanceRecord> result = maintenanceRecordService.createMaintenanceRecord(record);

        assertTrue(result.getErrors().contains("JpaSystemException"));
    }

    @Test
    void updateMaintenanceRecord_nonExistentRecord_DateCompleted_returnsFailure() {
        MaintenanceRecord maintenanceRecord = makeValidMaintenanceRecord();
        maintenanceRecord.setMaintenanceRecordId(999L);
        when(maintenanceRecordRepositoryJPA.findByDescriptionAndMileageDueAndVinId(
                maintenanceRecord.getDescription(),
                maintenanceRecord.getMileageDue(),
                maintenanceRecord.getVinId())).thenReturn(Optional.empty());
        Result<MaintenanceRecord> result = maintenanceRecordService.updateMaintenanceRecordDateCompleted(maintenanceRecord);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("MaintenanceRecord not found"));
    }

    @Test
    void updateMaintenanceRecord_DateCompleted_withNullData_returnsFailure() {
        MaintenanceRecord maintenanceRecord = null;
        Result<MaintenanceRecord> result = maintenanceRecordService.updateMaintenanceRecordDateCompleted(maintenanceRecord);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("Maintenance record is null"));
    }

    @Test
    void updateMaintenanceRecord_DateCompleted_withInvalidData_returnsFailure() {
        MaintenanceRecord maintenanceRecord = new MaintenanceRecord();
        maintenanceRecord.setVinId(1L);
        Result<MaintenanceRecord> result = maintenanceRecordService.updateMaintenanceRecordDateCompleted(maintenanceRecord);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("Maintenance record contains null or invalid values"));
    }

    @Test
    void updateMaintenanceRecord_DateCompleted_withValidData_returnsSuccess() {
        MaintenanceRecord maintenanceRecord = makeValidMaintenanceRecord();
        maintenanceRecord.setMaintenanceRecordId(1L);
        when(maintenanceRecordRepositoryJPA.findByDescriptionAndMileageDueAndVinId(
                maintenanceRecord.getDescription(),
                maintenanceRecord.getMileageDue(),
                maintenanceRecord.getVinId())).thenReturn(Optional.of(maintenanceRecord));
        when(maintenanceRecordRepositoryJPA.save(any())).thenReturn(maintenanceRecord);
        Result<MaintenanceRecord> result = maintenanceRecordService.updateMaintenanceRecordDateCompleted(maintenanceRecord);
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldThrowDataIntegrityViolationException_whenUpdateMaintenanceRecordDateCompleted() {
        MaintenanceRecord record = makeValidMaintenanceRecord();
        record.setMaintenanceRecordId(1L);
        when(maintenanceRecordRepositoryJPA.findByDescriptionAndMileageDueAndVinId(
                record.getDescription(),
                record.getMileageDue(),
                record.getVinId())).thenReturn(Optional.of(record));
        when(maintenanceRecordRepositoryJPA.save(any())).thenThrow(DataIntegrityViolationException.class);
        Result<MaintenanceRecord> result = maintenanceRecordService.updateMaintenanceRecordDateCompleted(record);

        assertTrue(result.getErrors().contains("DataIntegrityViolationException"));
    }

    @Test
    void shouldThrowJpaSystemException_whenUpdateMaintenanceRecordDateCompleted() {
        MaintenanceRecord record = makeValidMaintenanceRecord();
        record.setMaintenanceRecordId(1L);
        when(maintenanceRecordRepositoryJPA.findByDescriptionAndMileageDueAndVinId(
                record.getDescription(),
                record.getMileageDue(),
                record.getVinId())).thenReturn(Optional.of(record));
        when(maintenanceRecordRepositoryJPA.save(any())).thenThrow(JpaSystemException.class);
        Result<MaintenanceRecord> result = maintenanceRecordService.updateMaintenanceRecordDateCompleted(record);

        assertTrue(result.getErrors().contains("JpaSystemException"));
    }

    @Test
    void deleteMaintenanceRecord_nonExistentRecord_returnsFailure() {
        Long nonExistentMaintenanceRecordId = 999L;
        when(maintenanceRecordRepositoryJPA.findById(nonExistentMaintenanceRecordId)).thenReturn(Optional.empty());
        Result<MaintenanceRecord> result = maintenanceRecordService.deleteMaintenanceRecord(nonExistentMaintenanceRecordId);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("MaintenanceRecord not found"));
    }

    @Test
    void deleteMaintenanceRecord_existingRecord_returnsSuccess() {
        MaintenanceRecord existingMaintenanceRecord = makeValidMaintenanceRecord();
        existingMaintenanceRecord.setMaintenanceRecordId(1L);
        when(maintenanceRecordRepositoryJPA.findById(existingMaintenanceRecord.getMaintenanceRecordId())).thenReturn(Optional.of(existingMaintenanceRecord));
        Result<MaintenanceRecord> result = maintenanceRecordService.deleteMaintenanceRecord(existingMaintenanceRecord.getMaintenanceRecordId());
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldThrowDataIntegrityViolationException_whenDeleteMaintenanceRecord() {
        MaintenanceRecord record = makeValidMaintenanceRecord();
        record.setMaintenanceRecordId(1L);
        when(maintenanceRecordRepositoryJPA.findById(record.getMaintenanceRecordId())).thenReturn(Optional.of(record));
        doThrow(DataIntegrityViolationException.class).when(maintenanceRecordRepositoryJPA).deleteById(record.getMaintenanceRecordId());
        Result<MaintenanceRecord> result = maintenanceRecordService.deleteMaintenanceRecord(record.getMaintenanceRecordId());
        assertTrue(result.getErrors().contains("DataIntegrityViolationException"));
    }

    @Test
    void shouldThrowJpaSystemException_whenDeleteMaintenanceRecord() {
        MaintenanceRecord record = makeValidMaintenanceRecord();
        record.setMaintenanceRecordId(1L);
        when(maintenanceRecordRepositoryJPA.findById(record.getMaintenanceRecordId())).thenReturn(Optional.of(record));
        doThrow(JpaSystemException.class).when(maintenanceRecordRepositoryJPA).deleteById(record.getMaintenanceRecordId());
        Result<MaintenanceRecord> result = maintenanceRecordService.deleteMaintenanceRecord(record.getMaintenanceRecordId());
        assertTrue(result.getErrors().contains("JpaSystemException"));
    }
}
