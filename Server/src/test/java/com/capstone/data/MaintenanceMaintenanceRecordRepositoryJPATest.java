package com.capstone.data;

import com.capstone.models.MaintenanceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MaintenanceMaintenanceRecordRepositoryJPATest {

    @Autowired
    MaintenanceRecordRepositoryJPA repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new MaintenanceRecord(1, "test", "test", LocalDate.now(), 10000, 10000, 12.99));
    }

    @Test
    void findAllByVinId() {
        MaintenanceRecord expected = new MaintenanceRecord(1, "test", "test", LocalDate.now(), 10000, 10000, 12.99);
        List<MaintenanceRecord> actual = repository.findAllByVinId(1);
        assertEquals(expected, actual.get(0));
    }

    @Test
    void shouldNotFindNonExistentVinId() {
        List<MaintenanceRecord> result = repository.findAllByVinId(2);
        assertTrue(result.isEmpty());
    }
}