package com.capstone.data;

import com.capstone.models.UserVin;
import com.capstone.models.UserVinId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserVinRepositoryJPA extends JpaRepository<UserVin, UserVinId> {

  Optional<UserVin> findByUserUserIdAndVinVin(Long userId, String vin);

  @Query(
      "SELECT uv FROM UserVin uv JOIN FETCH uv.vin v JOIN FETCH v.vehicleType "
          + "WHERE uv.user.userId = :userId AND uv.vin.vin = :vin")
  Optional<UserVin> findForUserVin(@Param("userId") Long userId, @Param("vin") String vin);

  @Query("select uv.id.vin from UserVin uv where uv.id.userId = :userId")
  List<String> findVinNumbersForUser(@Param("userId") Long userId);

  @Modifying
  @Query("delete from UserVin uv where uv.id.userId = :userId and uv.id.vin = :vin")
  void deleteForUserVin(@Param("userId") Long userId, @Param("vin") String vin);

  @Modifying
  @Query("delete from UserVin uv where uv.id.userId = :userId")
  void deleteAllForUserId(@Param("userId") Long userId);
}
