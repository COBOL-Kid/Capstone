package com.capstone.domain;

import com.capstone.data.MaintenanceRecordRepositoryJPA;
import com.capstone.models.MaintenanceRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
class MaintenanceRecordServiceTest {

    @Autowired
    MaintenanceRecordService maintenanceRecordService;

    @MockBean
    MaintenanceRecordRepositoryJPA maintenanceRecordRepositoryJPA;

    @Test
    void findAllByVinId() {
        MaintenanceRecord expected = new MaintenanceRecord(1, "description", "notes", null, 1, 1, 1.0);
        when(maintenanceRecordRepositoryJPA.findAllByVinId(expected.getVinId())).thenReturn(List.of(expected));
        assertEquals(expected, maintenanceRecordService.findAllByVinId(expected.getVinId()).get(0));
    }

    @Test
    void shouldNotFindNonExistentVinId() {
        when(maintenanceRecordRepositoryJPA.findAllByVinId(1)).thenReturn(List.of());
        assertEquals(0, maintenanceRecordService.findAllByVinId(1).size());
    }
}