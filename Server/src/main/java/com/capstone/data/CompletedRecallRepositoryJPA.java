package com.capstone.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capstone.models.CompletedRecall;
import com.capstone.models.Recall;
import com.capstone.models.UserVin;

public interface CompletedRecallRepositoryJPA extends JpaRepository<CompletedRecall, Long> {

    @Query("select cr from CompletedRecall cr where cr.userVin.user.userId = :userId and cr.userVin.vin.vin = :vin")
    List<CompletedRecall> findAllForUserVin(@Param("userId") Long userId, @Param("vin") String vin);

    Optional<CompletedRecall> findByUserVinAndRecall(UserVin userVin, Recall recall);
}