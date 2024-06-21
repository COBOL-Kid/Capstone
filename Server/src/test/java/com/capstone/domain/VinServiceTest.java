package com.capstone.domain;

import com.capstone.data.VehicleInfoRepositoryJPA;
import com.capstone.data.VinRepositoryJPA;
import com.capstone.models.Result;
import com.capstone.models.Vin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
class VinServiceTest {

    @MockBean
    VinRepositoryJPA vinRepository;
    @MockBean
    VehicleInfoRepositoryJPA vehicleInfoRepository;
    @Autowired
    private VinService vinService;

    @Test
    void shouldFindVinsByOwnerId() {
        long ownerId = 1L;
        Vin vin = new Vin(ownerId, 1L, "VIN1234", 10000);
        List<Vin> expectedVins = List.of(vin);

        when(vinRepository.getVinsByOwnerId(ownerId)).thenReturn(expectedVins);

        List<Vin> actualVins = vinService.findVinsByOwnerId(ownerId);

        assertEquals(expectedVins, actualVins);
    }

    @Test
    void shouldHandleWhenNoVinsFoundForOwnerId() {
        long ownerId = 1L;

        when(vinRepository.getVinsByOwnerId(ownerId)).thenReturn(Collections.emptyList());

        List<Vin> actualVins = vinService.findVinsByOwnerId(ownerId);

        assertTrue(actualVins.isEmpty());
    }

    @Test
    void shouldCreateVinWhenValid() {
        Vin validVin = new Vin(1L, 1L, "VIN1234", 10000);

        when(vinRepository.save(validVin)).thenReturn(validVin);

        Result<Vin> actualResult = vinService.createVin(validVin);

        assertTrue(actualResult.isSuccess());
        assertEquals(validVin, actualResult.getPayload());
    }

    @Test
    void shouldNotCreateVinWhenInvalid() {
        Vin invalidVin = new Vin(1L, 1L, "VIN1234", 10000);
        invalidVin.setVin(null);

        Result<Vin> actualResult = vinService.createVin(invalidVin);

        assertFalse(actualResult.isSuccess());
        assertNotNull(actualResult.getErrors());
        assertTrue(actualResult.getErrors().contains("Fields cannot be null or empty"));
    }

    @Test
    void shouldHandleDataIntegrityViolationExceptionWhenCreateVin() {
        Vin validVin = new Vin(1L, 1L, "VIN1234", 10000);

        when(vinRepository.save(validVin)).thenThrow(DataIntegrityViolationException.class);

        Result<Vin> actualResult = vinService.createVin(validVin);

        assertFalse(actualResult.isSuccess());
        assertTrue(actualResult.getErrors().contains("DataIntegrityViolationException"));
    }

    @Test
    void shouldHandleJpaSystemExceptionWhenCreateVin() {
        Vin validVin = new Vin(1L, 1L, "VIN1234", 10000);

        when(vinRepository.save(validVin)).thenThrow(JpaSystemException.class);

        Result<Vin> actualResult = vinService.createVin(validVin);

        assertFalse(actualResult.isSuccess());
        assertTrue(actualResult.getErrors().contains("JpaSystemException"));
    }

    @Test
    void shouldUpdateVinWhenValidAndExists() {
        Vin validVin = new Vin(1L, 1L, "VIN1234", 10000);
        validVin.setVinId(1L);

        when(vinRepository.findById(validVin.getVinId())).thenReturn(Optional.of(validVin));
        when(vinRepository.save(validVin)).thenReturn(validVin);

        Result<Vin> actualResult = vinService.updateVin(validVin);

        assertTrue(actualResult.isSuccess());
        assertEquals(validVin, actualResult.getPayload());
    }

    @Test
    void shouldNotUpdateVinWhenInvalid() {
        Vin invalidVin = new Vin(1L, 1L, "VIN1234", 10000);
        invalidVin.setVin(null);

        Result<Vin> actualResult = vinService.updateVin(invalidVin);

        assertFalse(actualResult.isSuccess());
        assertNotNull(actualResult.getErrors());
        assertTrue(actualResult.getErrors().contains("Fields cannot be null or empty"));
    }

    @Test
    void shouldNotUpdateVinWhenNotExists() {
        Vin nonexistentVin = new Vin(1L, 1L, "VIN1234", 10000);
        nonexistentVin.setVinId(999L);

        when(vinRepository.findById(nonexistentVin.getVinId())).thenReturn(Optional.empty());

        Result<Vin> actualResult = vinService.updateVin(nonexistentVin);

        assertFalse(actualResult.isSuccess());
        assertTrue(actualResult.getErrors().contains("VIN number does not exist"));
    }

    @Test
    void shouldHandleDataIntegrityViolationExceptionWhenUpdateVin() {
        Vin validVin = new Vin(1L, 1L, "VIN1234", 10000);
        validVin.setVinId(1L);

        when(vinRepository.findById(validVin.getVinId())).thenReturn(Optional.of(validVin));
        when(vinRepository.save(validVin)).thenThrow(DataIntegrityViolationException.class);

        Result<Vin> actualResult = vinService.updateVin(validVin);

        assertFalse(actualResult.isSuccess());
        assertTrue(actualResult.getErrors().contains("DataIntegrityViolationException"));
    }

    @Test
    void shouldHandleJpaSystemExceptionWhenUpdateVin() {
        Vin validVin = new Vin(1L, 1L, "VIN1234", 10000);
        validVin.setVinId(1L);

        when(vinRepository.findById(validVin.getVinId())).thenReturn(Optional.of(validVin));
        when(vinRepository.save(validVin)).thenThrow(JpaSystemException.class);

        Result<Vin> actualResult = vinService.updateVin(validVin);

        assertFalse(actualResult.isSuccess());
        assertTrue(actualResult.getErrors().contains("JpaSystemException"));
    }

    @Test
    void shouldDeleteVinWhenExists() {
        Vin validVin = new Vin(1L, 1L, "VIN1234", 10000);
        validVin.setVinId(1L);

        when(vinRepository.findById(validVin.getVinId())).thenReturn(Optional.of(validVin));

        Result actualResult = vinService.deleteVinByid(validVin.getVinId());

        assertTrue(actualResult.isSuccess());
    }

    @Test
    void shouldNotDeleteVinWhenNotExist() {
        long nonexistentVinId = 999L;

        when(vinRepository.findById(nonexistentVinId)).thenReturn(Optional.empty());

        Result actualResult = vinService.deleteVinByid(nonexistentVinId);

        assertFalse(actualResult.isSuccess());
        assertTrue(actualResult.getErrors().contains("VIN number does not exist"));
    }

    @Test
    void shouldHandleDataIntegrityViolationExceptionWhenDeleteVin() {
        Vin validVin = new Vin(1L, 1L, "VIN1234", 10000);
        validVin.setVinId(1L);

        when(vinRepository.findById(validVin.getVinId())).thenReturn(Optional.of(validVin));
        doThrow(DataIntegrityViolationException.class).when(vinRepository).deleteById(validVin.getVinId());

        Result actualResult = vinService.deleteVinByid(validVin.getVinId());

        assertFalse(actualResult.isSuccess());
        assertTrue(actualResult.getErrors().contains("DataIntegrityViolationException"));
    }

    @Test
    void shouldHandleJpaSystemExceptionWhenDeleteVin() {
        Vin validVin = new Vin(1L, 1L, "VIN1234", 10000);
        validVin.setVinId(1L);

        when(vinRepository.findById(validVin.getVinId())).thenReturn(Optional.of(validVin));
        doThrow(JpaSystemException.class).when(vinRepository).deleteById(validVin.getVinId());

        Result actualResult = vinService.deleteVinByid(validVin.getVinId());

        assertFalse(actualResult.isSuccess());
        assertTrue(actualResult.getErrors().contains("JpaSystemException"));
    }
}
