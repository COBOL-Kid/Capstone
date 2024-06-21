package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.MaintenanceRecord;
import com.capstone.models.Result;
import com.sun.tools.javac.Main;
import org.springframework.beans.factory.annotation.Autowired;
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
        result.setPayload(maintenanceRecordRepositoryJPA.save(result.getPayload()));
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

    private void maintenanceRecordIdExists(Result<MaintenanceRecord> result) {
        Optional<MaintenanceRecord> record = maintenanceRecordRepositoryJPA.findById(result.getPayload().getMaintenanceRecordId());
        if (record.isEmpty()) {
            result.addError("Maintenance record does not exist");
        }
    }
}
