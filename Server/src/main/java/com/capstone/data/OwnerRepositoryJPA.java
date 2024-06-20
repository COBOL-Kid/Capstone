package com.capstone.data;

import com.capstone.models.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OwnerRepositoryJPA extends JpaRepository<Owner, Long> {

    @Query(value = "SELECT o FROM Owner o WHERE o.userName = ?1")
    Optional<Owner> getOwnerByUserName(String userName);
}
