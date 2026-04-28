package com.capstone.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capstone.models.User;

public interface UserRepositoryJPA extends JpaRepository<User, Long> {

    Optional<User> findByUserEmail(String userEmail);
}