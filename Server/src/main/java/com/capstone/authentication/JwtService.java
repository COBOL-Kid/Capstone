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
  private volatile SecretKey signInKey;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signInKey()).build().parseSignedClaims(token).getPayload();
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
    return claimsResolver.apply(parseClaims(token));
  }

  public String generateToken(Map<String, Object> extraClaims, UserDetails ownerDetails) {
    return Jwts.builder()
        .claims(extraClaims)
        .subject(ownerDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expirationMillis()))
        .signWith(signInKey(), Jwts.SIG.HS256)
        .compact();
  }

  public boolean validateToken(String token, UserDetails ownerDetails) {
    return validateToken(parseClaims(token), ownerDetails);
  }

  public boolean validateToken(Claims claims, UserDetails ownerDetails) {
    String userEmail = claims.getSubject();
    if (userEmail == null
        || !userEmail.equals(ownerDetails.getUsername())
        || isExpired(claims.getExpiration())) {
      return false;
    }
    if (ownerDetails instanceof AuthenticatedUser authenticatedUser) {
      Number userId = claims.get("userId", Number.class);
      return userId != null && userId.longValue() == authenticatedUser.userId();
    }
    return true;
  }

  private static boolean isExpired(Date expiration) {
    return expiration == null || expiration.before(new Date());
  }

  private long expirationMillis() {
    if (jwtProperties.getExpirationMinutes() <= 0) {
      throw new IllegalStateException("JWT expiration must be positive");
    }
    return Duration.ofMinutes(jwtProperties.getExpirationMinutes()).toMillis();
  }

  private SecretKey signInKey() {
    SecretKey cached = signInKey;
    if (cached != null) {
      return cached;
    }
    synchronized (this) {
      cached = signInKey;
      if (cached == null) {
        cached = createSignInKey(jwtProperties);
        signInKey = cached;
      }
      return cached;
    }
  }

  private static SecretKey createSignInKey(JwtProperties jwtProperties) {
    String secretKey = jwtProperties.getSecret();
    if (secretKey == null || secretKey.isBlank()) {
      throw new IllegalStateException("JWT secret is required");
    }
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
