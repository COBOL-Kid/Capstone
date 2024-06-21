package com.capstone.data;

import com.capstone.models.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaintenanceRecordRepositoryJPA extends JpaRepository<MaintenanceRecord, Long> {

    @Query("select r from MaintenanceRecord r where r.vinId = ?1")
    List<MaintenanceRecord> findAllByVinId(long vinId);

}
