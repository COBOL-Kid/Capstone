package com.capstone.data;

import com.capstone.models.CompletedRecall;
import com.capstone.models.Recall;
import com.capstone.models.UserVin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompletedRecallRepositoryJPA extends JpaRepository<CompletedRecall, Long> {

  Optional<CompletedRecall> findByUserVinAndRecall(UserVin userVin, Recall recall);

  @Modifying
  @Query("delete from CompletedRecall cr where cr.userVin.id.userId = :userId")
  void deleteAllForUserId(@Param("userId") Long userId);

  @Modifying
  @Query(
      "delete from CompletedRecall cr where cr.userVin.id.userId = :userId and cr.userVin.id.vin = :vin")
  void deleteAllForUserVin(@Param("userId") Long userId, @Param("vin") String vin);
}
