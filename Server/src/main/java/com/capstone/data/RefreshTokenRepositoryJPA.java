package com.capstone.data;

import com.capstone.models.RefreshToken;
import com.capstone.models.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepositoryJPA extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByToken(String token);

  @Modifying
  @Query("delete from REFRESH_TOKEN rt where rt.user = :user")
  void deleteByUser(@Param("user") User user);

  @Modifying
  @Query("delete from REFRESH_TOKEN rt where rt.token = :token")
  void deleteByToken(@Param("token") String token);

  @Modifying
  @Query("delete from REFRESH_TOKEN rt where rt.id = :id")
  int deleteByIdReturning(@Param("id") Long id);
}
