package com.capstone.data;

import com.capstone.models.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepositoryJPA extends JpaRepository<User, Long> {

  Optional<User> findByUserEmail(String userEmail);

  boolean existsByUserEmail(String userEmail);
}
