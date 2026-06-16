package com.capstone.authentication;

import com.capstone.data.UserRepositoryJPA;
import com.capstone.models.Role;
import com.capstone.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtService jwtService;
  private final UserRepositoryJPA userRepository;
  private final AuthCookies authCookies;

  public JwtAuthenticationFilter(
      JwtService jwtService, UserRepositoryJPA userRepository, AuthCookies authCookies) {
    this.jwtService = jwtService;
    this.userRepository = userRepository;
    this.authCookies = authCookies;
  }

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String jwtToken = authCookies.readAccessToken(request).orElse(null);
    if (jwtToken == null || jwtToken.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }
    try {
      Claims claims = jwtService.parseClaims(jwtToken);
      String userEmail = claims.getSubject();
      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        AuthenticatedUser userDetails = toAuthenticatedUser(claims);
        if (!jwtService.validateToken(claims, userDetails)) {
          filterChain.doFilter(request, response);
          return;
        }
        User user =
            userRepository
                .findById(userDetails.userId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (isLocked(user)) {
          response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is locked");
          return;
        }
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
      log.debug("Rejecting request with invalid JWT: {}", ex.getMessage());
    }
    filterChain.doFilter(request, response);
  }

  private static boolean isLocked(User user) {
    return user.getLockoutEnd() != null && user.getLockoutEnd().isAfter(LocalDateTime.now());
  }

  private static AuthenticatedUser toAuthenticatedUser(Claims claims) {
    Number userId = claims.get("userId", Number.class);
    if (userId == null) {
      throw new IllegalArgumentException("JWT is missing userId claim");
    }
    String roleName = claims.get("role", String.class);
    if (roleName == null || roleName.isBlank()) {
      throw new IllegalArgumentException("JWT is missing role claim");
    }
    Role role = Role.valueOf(roleName);
    Boolean emailVerified = claims.get("emailVerified", Boolean.class);
    return new AuthenticatedUser(
        userId.longValue(), claims.getSubject(), role, Boolean.TRUE.equals(emailVerified));
  }
}
