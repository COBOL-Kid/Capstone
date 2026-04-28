package com.capstone.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capstone.models.UserVin;
import com.capstone.models.UserVinId;

public interface UserVinRepositoryJPA extends JpaRepository<UserVin, UserVinId> {

    Optional<UserVin> findByUserUserIdAndVinVin(Long userId, String vin);
}