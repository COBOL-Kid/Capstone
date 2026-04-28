package com.capstone.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.Vin;

@Service
public class VinService {

    VinRepositoryJPA vinRepositoryJPA;

    public VinService(VinRepositoryJPA vinRepositoryJPA) {
        this.vinRepositoryJPA = vinRepositoryJPA;
    }

    public List<Vin> findVinsByUserId(Long userId) {
        return vinRepositoryJPA.getVinsByUserId(userId);
    }

    public Optional<Vin> findByVin(String vin) {
        if (vin == null || vin.isBlank()) {
            return Optional.empty();
        }
        return vinRepositoryJPA.findById(vin.trim().toUpperCase());
    }
}
