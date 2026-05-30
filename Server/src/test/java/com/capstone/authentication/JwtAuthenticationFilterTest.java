package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.capstone.models.Role;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldSkipAuthenticationWhenBearerHeaderIsMissing() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    filter.doFilter(
        new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).extractUserEmail(any());
  }

  @Test
  void shouldSetAuthenticationForValidBearerToken() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer jwt-token");
    AuthenticatedUser user = new AuthenticatedUser(1L, "driver@example.com", Role.USER);

    when(jwtService.extractUserEmail("jwt-token")).thenReturn("driver@example.com");
    when(jwtService.extractUserId("jwt-token")).thenReturn(1L);
    when(jwtService.extractRole("jwt-token")).thenReturn("USER");
    when(jwtService.validateToken("jwt-token", user)).thenReturn(true);

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertEquals(user, authentication.getPrincipal());
    assertEquals(user.getAuthorities(), authentication.getAuthorities());
    verify(jwtService).validateToken("jwt-token", user);
  }

  @Test
  void shouldLeaveContextEmptyWhenTokenIsInvalid() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer jwt-token");
    AuthenticatedUser user = new AuthenticatedUser(1L, "driver@example.com", Role.USER);

    when(jwtService.extractUserEmail("jwt-token")).thenReturn("driver@example.com");
    when(jwtService.extractUserId("jwt-token")).thenReturn(1L);
    when(jwtService.extractRole("jwt-token")).thenReturn("USER");
    when(jwtService.validateToken("jwt-token", user)).thenReturn(false);

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldRejectMalformedTokenWithoutCrashingFilterChain() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer not.a.real.jwt");
    MockFilterChain chain = new MockFilterChain();

    when(jwtService.extractUserEmail("not.a.real.jwt"))
        .thenThrow(new MalformedJwtException("malformed"));

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).validateToken(any(), any());
  }

  @Test
  void shouldRejectTokenMissingRoleClaim() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer jwt-token");
    MockFilterChain chain = new MockFilterChain();

    when(jwtService.extractUserEmail("jwt-token")).thenReturn("driver@example.com");
    when(jwtService.extractUserId("jwt-token")).thenReturn(1L);
    when(jwtService.extractRole("jwt-token")).thenReturn(null);

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).validateToken(any(), any());
  }
}
