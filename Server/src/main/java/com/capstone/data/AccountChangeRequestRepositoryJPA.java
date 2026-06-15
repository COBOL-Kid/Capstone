package com.capstone.data;

import com.capstone.models.AccountChangeRequest;
import com.capstone.models.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountChangeRequestRepositoryJPA
    extends JpaRepository<AccountChangeRequest, Long> {

  @Query("SELECT a FROM AccountChangeRequest a WHERE a.user = :user")
  Optional<AccountChangeRequest> findByUser(User user);

  void deleteByUser(User user);
}
