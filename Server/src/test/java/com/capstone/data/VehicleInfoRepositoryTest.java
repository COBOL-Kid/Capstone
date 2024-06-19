package com.capstone.data;

import com.capstone.models.VehicleInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class VehicleInfoRepositoryTest {

    @Autowired
    private VehicleInfoRepository vehicleInfoRepository;

    @Test
    public void testFindVehicleInfoByVehicleInfoId_VehicleExists() {
        VehicleInfo vehicleInfo = new VehicleInfo(2006, "Toyota", "Tundra", "A Url");
        vehicleInfo.setVehicleInfoId(1);

        Optional<VehicleInfo> result = vehicleInfoRepository.findVehicleInfoByVehicleInfoId(vehicleInfo.getVehicleInfoId());

        assertEquals(Optional.of(vehicleInfo), result);
    }

    @Test
    public void testFindVehicleInfoByVehicleInfoId_VehicleDoesNotExist() {
        int vehicleInfoId = 2;

        Optional<VehicleInfo> result = vehicleInfoRepository.findVehicleInfoByVehicleInfoId(vehicleInfoId);

        assertEquals(Optional.empty(), result);
    }

    @Test
    public void testFindVehicleInfoByVehicleInfoId_DifferentVehicleReturned() {
        VehicleInfo vehicleInfo = new VehicleInfo(2006, "Toyota", "Tundra", "A Url");

        VehicleInfo differentVehicleInfo = new VehicleInfo(2010, "Honda", "Accord", "Another Url");

        Optional<VehicleInfo> result = vehicleInfoRepository.findVehicleInfoByVehicleInfoId(vehicleInfo.getVehicleInfoId());

        assertNotEquals(Optional.of(differentVehicleInfo), result);
    }
}