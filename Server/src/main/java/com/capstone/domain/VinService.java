package com.capstone.domain;

import com.capstone.data.VehicleInfoRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VinService {

    VinRepositoryJPA vinRepository;

    VehicleInfoRepositoryJPA vehicleInfoRepository;

    @Autowired
    public VinService(VinRepositoryJPA vinRepository, VehicleInfoRepositoryJPA vehicleInfoRepository) {
        this.vinRepository = vinRepository;
        this.vehicleInfoRepository = vehicleInfoRepository;
    }


}
