package com.capstone.data;

import com.capstone.models.VehicleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class VehicleInfoRepositoryJPATest {

    @Autowired
    private VehicleInfoRepositoryJPA vehicleInfoRepositoryJPA;

    @BeforeEach
    void setup() {
        vehicleInfoRepositoryJPA.deleteAll();
    }

    @Test
    void shouldFindYearMakeModel() {
        VehicleInfo expected = new VehicleInfo(1999, "test", "test", "image");
        vehicleInfoRepositoryJPA.save(expected);
        Optional<VehicleInfo> actual = vehicleInfoRepositoryJPA.findByVehicleInfoYearAndMakeAndModel(expected.getYear(), expected.getMake(), expected.getModel());
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    void shouldNotFindNonExistentYearMakeModel() {
        VehicleInfo expected = new VehicleInfo(1999, "test", "test", "image");
        vehicleInfoRepositoryJPA.save(expected);
        Optional<VehicleInfo> actual = vehicleInfoRepositoryJPA.findByVehicleInfoYearAndMakeAndModel(1, "no", "no");
        assertFalse(actual.isPresent());
    }
}