package com.capstone.domain;

import com.capstone.data.VehicleInfoRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
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

    public List<Vin> findVinsByOwnerId(Long ownerId) {
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

    @Transactional
    public Result<Vin> updateVin(Vin incomingVin) {
        Result<Vin> result = validateVin(incomingVin);
        if (!result.isSuccess()) {
            return result;
        }
        result = vinIdExists(incomingVin.getVinId());
        if (!result.isSuccess()) {
            return result;
        }
        Vin existingVin = result.getPayload();
        existingVin.setOwnerId(incomingVin.getOwnerId());
        existingVin.setVehicleInfoId(incomingVin.getVehicleInfoId());
        existingVin.setVin(incomingVin.getVin());
        existingVin.setMileage(incomingVin.getMileage());
        try {
            result.setPayload(vinRepositoryJPA.save(existingVin));
        } catch (DataIntegrityViolationException e) {
            result.addError("DataIntegrityViolationException");
        } catch (JpaSystemException e) {
            result.addError("JpaSystemException");
        }
        return result;
    }

    public Result<Vin> deleteVinByid(Long vinId) {
        Result<Vin> result = vinIdExists(vinId);
        if (!result.isSuccess()) {
            return result;
        }
        try {
            vinRepositoryJPA.deleteById(result.getPayload().getVinId());
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

    private Result<Vin> vinIdExists(Long vinId) {
        Optional<Vin> record = vinRepositoryJPA.findById(vinId);
        Result<Vin> result = new Result<>();
        if (record.isEmpty()) {
            result.addError("VIN number does not exist");
            return result;
        }
        result.setPayload(record.get());
        return result;
    }
}
