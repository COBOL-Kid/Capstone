package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.MaintenanceRecord;
import com.capstone.models.Result;
import com.capstone.models.Vin;
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

    public List<MaintenanceRecord> findAllMaintenanceRecordsByVinId(Long vinId) {
        return maintenanceRecordRepositoryJPA.findAllByVinId(vinId);
    }

    @Transactional
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
    public Result<MaintenanceRecord> createNotYetExistentMaintenanceRecords(List<MaintenanceRecord> records) {
        Result<MaintenanceRecord> masterResult = new Result<>();
        for (MaintenanceRecord record : records) {
            Result<MaintenanceRecord> result = validateMaintenanceRecord(record);
            if (!result.isSuccess()) {
                return result;
            }
            vinIdExists(result);
            if (!result.isSuccess()) {
                return result;
            }
            Optional<MaintenanceRecord> existingRecord = maintenanceRecordRepositoryJPA.findByDescriptionAndMileageDueAAndVinId(record.getDescription(), record.getMileageDue(), record.getVinId());
            if (existingRecord.isEmpty()) {
                try {
                    result.setPayload(maintenanceRecordRepositoryJPA.save(result.getPayload()));
                } catch (DataIntegrityViolationException e) {
                    result.addError("DataIntegrityViolationException");
                } catch (JpaSystemException e) {
                    result.addError("JpaSystemException");
                }
            }
            if (!result.isSuccess()) {
                masterResult.setErrors(result.getErrors());
            }
        }
        return masterResult;
    }

    @Transactional
    public Result<MaintenanceRecord> updateMaintenanceRecordDateCompleted(MaintenanceRecord incomingRecord) {
        Result<MaintenanceRecord> result = validateMaintenanceRecord(incomingRecord);
        if (!result.isSuccess()) {
            return result;
        }
        Optional<MaintenanceRecord> existingRecordOpt = maintenanceRecordRepositoryJPA.findById(incomingRecord.getMaintenanceRecordId());
        if (existingRecordOpt.isEmpty()) {
            result.addError("MaintenanceRecord not found");
            return result;
        }
        MaintenanceRecord existingRecord = existingRecordOpt.get();
        existingRecord.setDateCompleted(incomingRecord.getDateCompleted());
        try {
            result.setPayload(maintenanceRecordRepositoryJPA.save(existingRecord));
        } catch (DataIntegrityViolationException e) {
            result.addError("DataIntegrityViolationException");
        } catch (JpaSystemException e) {
            result.addError("JpaSystemException");
        }
        return result;
    }

    @Transactional
    public Result<MaintenanceRecord> deleteMaintenanceRecord(Long maintenanceRecordId) {
        Result<MaintenanceRecord> result = new Result<>();
        Optional<MaintenanceRecord> existingRecord = maintenanceRecordRepositoryJPA.findById(maintenanceRecordId);
        if (existingRecord.isEmpty()) {
            result.addError("MaintenanceRecord not found");
            return result;
        }
        try {
            maintenanceRecordRepositoryJPA.deleteById(maintenanceRecordId);
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
        Optional<Vin> record = vinRepositoryJPA.findById(result.getPayload().getVinId());
        if (record.isEmpty()) {
            result.addError("VIN number does not exist");
        }
    }
}
