package com.capstone.authentication;

import com.capstone.models.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    final String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }
    String jwtToken = authHeader.substring(7);
    try {
      Claims claims = jwtService.parseClaims(jwtToken);
      String userEmail = claims.getSubject();
      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        AuthenticatedUser userDetails = toAuthenticatedUser(claims);
        if (jwtService.validateToken(claims, userDetails)) {
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
      log.debug("Rejecting request with invalid JWT: {}", ex.getMessage());
    }
    filterChain.doFilter(request, response);
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
    return new AuthenticatedUser(userId.longValue(), claims.getSubject(), role);
  }
}
