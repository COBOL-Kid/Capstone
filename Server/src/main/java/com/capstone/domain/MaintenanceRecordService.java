package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.MaintenanceRecord;
import com.capstone.models.Result;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MaintenanceRecordService {

    MaintenanceRecordRepositoryJPA maintenanceRecordRepositoryJPA;
    VinRepositoryJPA vinRepositoryJPA;

    @Autowired
    public MaintenanceRecordService(MaintenanceRecordRepositoryJPA maintenanceRecordRepositoryJPA, VinRepositoryJPA vinRepositoryJPA) {
        this.maintenanceRecordRepositoryJPA = maintenanceRecordRepositoryJPA;
        this.vinRepositoryJPA = vinRepositoryJPA;
    }

    public List<MaintenanceRecord> findAllMaintenanceRecordsByVinId(long vinId) {
        return maintenanceRecordRepositoryJPA.findAllByVinId(vinId);
    }

    public Result<MaintenanceRecord> createMaintenanceRecord(MaintenanceRecord maintenanceRecord) {
        Result<MaintenanceRecord> result = validateMaintenanceRecord(maintenanceRecord);
        if (!result.isSuccess()) {
            return result;
        }
        vinIdExists(result);
        if (!result.isSuccess()) {
            return result;
        }
        try {
            result.setPayload(maintenanceRecordRepositoryJPA.save(result.getPayload()));
        } catch (DataIntegrityViolationException e) {
            result.addError("DataIntegrityViolationException");
        } catch (JpaSystemException e) {
            result.addError("JpaSystemException");
        }
        return result;
    }

    @Transactional
    public Result<MaintenanceRecord> updateMaintenanceRecord(MaintenanceRecord incomingRecord) {
        Result<MaintenanceRecord> result = validateMaintenanceRecord(incomingRecord);
        if (!result.isSuccess()) {
            return result;
        }
        Optional<MaintenanceRecord> existingRecord = maintenanceRecordRepositoryJPA.findById(incomingRecord.getMaintenanceRecordId());
        if (existingRecord.isEmpty()) {
            result.addError("MaintenanceRecord not found");
            return result;
        }
        existingRecord.get().setDescription(incomingRecord.getDescription());
        existingRecord.get().setNotes(incomingRecord.getNotes());
        existingRecord.get().setDateCompleted(incomingRecord.getDateCompleted());
        existingRecord.get().setMileageDue(incomingRecord.getMileageDue());
        existingRecord.get().setMileageCompleted(incomingRecord.getMileageCompleted());
        existingRecord.get().setCost(incomingRecord.getCost());
        try {
            result.setPayload(maintenanceRecordRepositoryJPA.save(existingRecord.get()));
        } catch (DataIntegrityViolationException e) {
            result.addError("DataIntegrityViolationException");
        } catch (JpaSystemException e) {
            result.addError("JpaSystemException");
        }
        return result;
    }

    private Result<MaintenanceRecord> validateMaintenanceRecord(MaintenanceRecord maintenanceRecord) {
        Result<MaintenanceRecord> result = new Result<>();
        if (maintenanceRecord == null) {
            result.addError("Maintenance record is null");
            return result;
        }
        result.setPayload(maintenanceRecord);
        if (result.getPayload().isInvalid()) {
            result.addError("Maintenance record contains null or invalid values");
        }
        return result;
    }

    private void vinIdExists(Result<MaintenanceRecord> result) {
        Optional<MaintenanceRecord> record = maintenanceRecordRepositoryJPA.findById(result.getPayload().getVinId());
        if (record.isEmpty()) {
            result.addError("VIN number does not exist");
        }
    }
}
