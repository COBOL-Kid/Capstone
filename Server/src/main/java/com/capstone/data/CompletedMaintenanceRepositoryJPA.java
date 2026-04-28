package com.capstone.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capstone.models.CompletedMaintenance;
import com.capstone.models.MaintMileage;
import com.capstone.models.UserVin;

public interface CompletedMaintenanceRepositoryJPA extends JpaRepository<CompletedMaintenance, Long> {

    @Query("select cm from CompletedMaintenance cm where cm.userVin.user.userId = :userId and cm.userVin.vin.vin = :vin")
    List<CompletedMaintenance> findAllForUserVin(@Param("userId") Long userId, @Param("vin") String vin);

    Optional<CompletedMaintenance> findByUserVinAndMaintMileage(UserVin userVin, MaintMileage maintMileage);
}