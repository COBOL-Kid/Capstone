package com.capstone.data;

import com.capstone.models.MaintenanceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MaintenanceMaintenanceRecordRepositoryTest {

    @Autowired
    MaintenanceRecordRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.save(new MaintenanceRecord(1, "test", "test", LocalDate.now(), 10000, 10000, 12.99));
    }

    @Test
    void findAllByVinId() {
        MaintenanceRecord expected = new MaintenanceRecord(1, "test", "test", LocalDate.now(), 10000, 10000, 12.99);
        Optional<List<MaintenanceRecord>> actual = repository.findAllByVinId(1);
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get().get(0));
    }

    @Test
    void sdhouldNotFindNonExistentVinId() {
        Optional<List<MaintenanceRecord>> result = repository.findAllByVinId(2);
        assertTrue(result.get().isEmpty());
    }
}