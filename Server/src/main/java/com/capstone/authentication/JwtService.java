package com.capstone.authentication;

import com.capstone.configuration.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtProperties jwtProperties;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public String extractUserEmail(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Long extractUserId(String token) {
    Number userId = extractClaim(token, claims -> claims.get("userId", Number.class));
    if (userId == null) {
      throw new IllegalArgumentException("JWT is missing userId claim");
    }
    return userId.longValue();
  }

  public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getClaimsFromToken(token);
    return claimsResolver.apply(claims);
  }

  public String generateToken(Map<String, Object> extraClaims, UserDetails ownerDetails) {
    return Jwts.builder()
        .claims(extraClaims)
        .subject(ownerDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expirationMillis()))
        .signWith(getSignInKey(), Jwts.SIG.HS256)
        .compact();
  }

  public boolean validateToken(String token, UserDetails ownerDetails) {
    final String userEmail = extractUserEmail(token);
    if (!userEmail.equals(ownerDetails.getUsername()) || isTokenExpired(token)) {
      return false;
    }
    if (ownerDetails instanceof AuthenticatedUser authenticatedUser) {
      Long tokenUserId = extractUserId(token);
      return tokenUserId.equals(authenticatedUser.userId());
    }
    return true;
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private Claims getClaimsFromToken(String token) {
    return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSignInKey() {
    String secretKey = jwtProperties.getSecret();
    if (secretKey == null || secretKey.isBlank()) {
      throw new IllegalStateException("JWT secret is required");
    }
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  private long expirationMillis() {
    if (jwtProperties.getExpirationMinutes() <= 0) {
      throw new IllegalStateException("JWT expiration must be positive");
    }
    return Duration.ofMinutes(jwtProperties.getExpirationMinutes()).toMillis();
  }
}
