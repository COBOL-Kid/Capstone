package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.capstone.configuration.JwtProperties;
import com.capstone.models.Role;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET =
      Base64.getEncoder()
          .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

  @Test
  void shouldParseGeneratedTokenClaims() {
    JwtService jwtService = jwtService(15);
    AuthenticatedUser user = user();

    String token =
        jwtService.generateToken(Map.of("userId", user.userId(), "role", user.role().name()), user);
    Claims claims = jwtService.parseClaims(token);

    assertEquals("driver@example.com", claims.getSubject());
    assertEquals(1L, claims.get("userId", Number.class).longValue());
    assertEquals("USER", claims.get("role", String.class));
  }

  @Test
  void shouldValidateFreshTokenByString() {
    JwtService jwtService = jwtService(15);
    AuthenticatedUser user = user();

    String token =
        jwtService.generateToken(Map.of("userId", user.userId(), "role", user.role().name()), user);

    assertTrue(jwtService.validateToken(token, user));
  }

  @Test
  void shouldValidateFreshTokenByClaims() {
    JwtService jwtService = jwtService(15);
    AuthenticatedUser user = user();

    String token =
        jwtService.generateToken(Map.of("userId", user.userId(), "role", user.role().name()), user);
    Claims claims = jwtService.parseClaims(token);

    assertTrue(jwtService.validateToken(claims, user));
  }

  @Test
  void shouldRejectSubjectMismatch() {
    JwtService jwtService = jwtService(15);
    AuthenticatedUser user = user();
    Claims claims = mock(Claims.class);

    when(claims.getSubject()).thenReturn("other@example.com");
    when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
    when(claims.get("userId", Number.class)).thenReturn(1);

    assertFalse(jwtService.validateToken(claims, user));
  }

  @Test
  void shouldRejectUserIdMismatch() {
    JwtService jwtService = jwtService(15);
    AuthenticatedUser user = user();
    Claims claims = mock(Claims.class);

    when(claims.getSubject()).thenReturn("driver@example.com");
    when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
    when(claims.get("userId", Number.class)).thenReturn(99);

    assertFalse(jwtService.validateToken(claims, user));
  }

  @Test
  void shouldRejectMissingUserIdClaim() {
    JwtService jwtService = jwtService(15);
    AuthenticatedUser user = user();
    Claims claims = mock(Claims.class);

    when(claims.getSubject()).thenReturn("driver@example.com");
    when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
    when(claims.get("userId", Number.class)).thenReturn(null);

    assertFalse(jwtService.validateToken(claims, user));
  }

  @Test
  void shouldRejectExpiredClaims() {
    JwtService jwtService = jwtService(15);
    AuthenticatedUser user = user();
    Claims claims = mock(Claims.class);

    when(claims.getSubject()).thenReturn("driver@example.com");
    when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 60_000));
    when(claims.get("userId", Number.class)).thenReturn(1);

    assertFalse(jwtService.validateToken(claims, user));
  }

  @Test
  void shouldThrowWhenExtractingMissingUserId() {
    JwtService jwtService = jwtService(15);
    AuthenticatedUser user = user();

    String token = jwtService.generateToken(Map.of("role", user.role().name()), user);

    assertThrows(IllegalArgumentException.class, () -> jwtService.extractUserId(token));
  }

  private JwtService jwtService(long expirationMinutes) {
    JwtProperties properties = new JwtProperties();
    properties.setSecret(SECRET);
    properties.setExpirationMinutes(expirationMinutes);
    return new JwtService(properties);
  }

  private AuthenticatedUser user() {
    return new AuthenticatedUser(1L, "driver@example.com", Role.USER, true);
  }
}
