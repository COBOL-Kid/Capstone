package com.capstone.data;

import com.capstone.models.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepositoryJPA extends JpaRepository<User, Long> {

  @Query("SELECT u FROM User u WHERE u.userEmail = :userEmail")
  Optional<User> findByUserEmail(String userEmail);

  @Query("SELECT u.userId FROM User u WHERE u.userEmail = :userEmail")
  boolean existsByUserEmail(String userEmail);

  @Query("SELECT u FROM User u WHERE u.emailVerified = FALSE AND u.createdAt < :createdAt")
  List<User> findByEmailVerifiedFalseAndCreatedAtBefore(Instant createdAt);
}
