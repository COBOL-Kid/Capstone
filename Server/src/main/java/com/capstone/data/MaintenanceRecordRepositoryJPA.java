package com.capstone.data;

import com.capstone.models.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRecordRepositoryJPA extends JpaRepository<MaintenanceRecord, Long> {

    @Query("select r from MaintenanceRecord r where r.vinId = ?1")
    List<MaintenanceRecord> findAllByVinId(Long vinId);

    @Query("select r from MaintenanceRecord r where r.description =?1 and r.mileageDue =?2 and r.vinId = ?3")
    Optional<MaintenanceRecord> findByDescriptionAndMileageDueAndVinId(String description, int mileageDue, Long vinId);

}
