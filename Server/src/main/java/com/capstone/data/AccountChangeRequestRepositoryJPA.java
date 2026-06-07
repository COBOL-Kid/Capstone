package com.capstone.data;

import com.capstone.models.AccountChangeRequest;
import com.capstone.models.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountChangeRequestRepositoryJPA
    extends JpaRepository<AccountChangeRequest, Long> {

  Optional<AccountChangeRequest> findByUser(User user);

  void deleteByUser(User user);
}
