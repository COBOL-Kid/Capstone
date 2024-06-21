package com.capstone.domain;

import com.capstone.data.VehicleInfoRepositoryJPA;
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
public class VinService {


    VinRepositoryJPA vinRepositoryJPA;

    VehicleInfoRepositoryJPA vehicleInfoRepositoryJPA;

    @Autowired
    public VinService(VinRepositoryJPA vinRepositoryJPA, VehicleInfoRepositoryJPA vehicleInfoRepositoryJPA) {
        this.vinRepositoryJPA = vinRepositoryJPA;
        this.vehicleInfoRepositoryJPA = vehicleInfoRepositoryJPA;
    }

    public List<Vin> findVinsByOwnerId(long ownerId) {
        return vinRepositoryJPA.getVinsByOwnerId(ownerId);
    }

    @Transactional
    public Result<Vin> createVin(Vin vin) {
        Result<Vin> result = validateVin(vin);
        if (!result.isSuccess()) {
            return result;
        }
        try {
            result.setPayload(vinRepositoryJPA.save(result.getPayload()));
        } catch (DataIntegrityViolationException e) {
            result.addError("DataIntegrityViolationException");
        } catch (JpaSystemException e) {
            result.addError("JpaSystemException");
        }
        return result;
    }


    private Result<Vin> validateVin(Vin vin) {
        Result<Vin> result = new Result<>();
        if (vin == null) {
            result.addError("Vin is null");
            return result;
        }
        if (vin.isInvalid()) {
            result.addError("Fields cannot be null or empty");
            return result;
        }
        result.setPayload(vin);
        return result;
    }

    private void vinIdExists(Result<MaintenanceRecord> result) {
        Optional<Vin> record = vinRepositoryJPA.findById(result.getPayload().getVinId());
        if (record.isEmpty()) {
            result.addError("VIN number does not exist");
        }
    }


}
