package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.capstone.configuration.CookieSecurityProperties;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.models.Role;
import com.capstone.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
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
  void shouldSkipAuthenticationWhenAccessTokenCookieIsMissing()
      throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    JwtAuthenticationFilter filter = filter(jwtService, mock(UserRepositoryJPA.class));

    filter.doFilter(
        new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).parseClaims(any());
  }

  @Test
  void shouldSetAuthenticationForValidSessionCookie() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    JwtAuthenticationFilter filter = filter(jwtService, userRepository);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(sessionCookie("jwt-token"));
    AuthenticatedUser user = new AuthenticatedUser(1L, "driver@example.com", Role.USER, true);
    Claims claims = mock(Claims.class);
    User storedUser = new User();
    storedUser.setUserId(1L);

    when(jwtService.parseClaims("jwt-token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("driver@example.com");
    when(claims.get("userId", Number.class)).thenReturn(1);
    when(claims.get("role", String.class)).thenReturn("USER");
    when(claims.get("emailVerified", Boolean.class)).thenReturn(true);
    when(jwtService.validateToken(claims, user)).thenReturn(true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertEquals(user, authentication.getPrincipal());
    assertEquals(user.getAuthorities(), authentication.getAuthorities());
    verify(jwtService).validateToken(claims, user);
  }

  @Test
  void shouldAcceptLegacyAccessTokenCookie() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    JwtAuthenticationFilter filter = filter(jwtService, userRepository);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new jakarta.servlet.http.Cookie("accessToken", "jwt-token"));
    AuthenticatedUser user = new AuthenticatedUser(1L, "driver@example.com", Role.USER, true);
    Claims claims = mock(Claims.class);
    User storedUser = new User();
    storedUser.setUserId(1L);

    when(jwtService.parseClaims("jwt-token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("driver@example.com");
    when(claims.get("userId", Number.class)).thenReturn(1);
    when(claims.get("role", String.class)).thenReturn("USER");
    when(claims.get("emailVerified", Boolean.class)).thenReturn(true);
    when(jwtService.validateToken(claims, user)).thenReturn(true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldRejectLockedAccounts() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    JwtAuthenticationFilter filter = filter(jwtService, userRepository);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(sessionCookie("jwt-token"));
    AuthenticatedUser user = new AuthenticatedUser(1L, "driver@example.com", Role.USER, true);
    Claims claims = mock(Claims.class);
    User storedUser = new User();
    storedUser.setUserId(1L);
    storedUser.setLockoutEnd(java.time.LocalDateTime.now().plusMinutes(5));

    when(jwtService.parseClaims("jwt-token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("driver@example.com");
    when(claims.get("userId", Number.class)).thenReturn(1);
    when(claims.get("role", String.class)).thenReturn("USER");
    when(claims.get("emailVerified", Boolean.class)).thenReturn(true);
    when(jwtService.validateToken(claims, user)).thenReturn(true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(storedUser));

    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(403, response.getStatus());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void shouldLeaveContextEmptyWhenTokenIsInvalid() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    JwtAuthenticationFilter filter = filter(jwtService, userRepository);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(sessionCookie("jwt-token"));
    AuthenticatedUser user = new AuthenticatedUser(1L, "driver@example.com", Role.USER, true);
    Claims claims = mock(Claims.class);

    when(jwtService.parseClaims("jwt-token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("driver@example.com");
    when(claims.get("userId", Number.class)).thenReturn(1);
    when(claims.get("role", String.class)).thenReturn("USER");
    when(claims.get("emailVerified", Boolean.class)).thenReturn(true);
    when(jwtService.validateToken(claims, user)).thenReturn(false);

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(userRepository, never()).findById(any());
  }

  @Test
  void shouldRejectMalformedTokenWithoutCrashingFilterChain() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    JwtAuthenticationFilter filter = filter(jwtService, userRepository);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(sessionCookie("not.a.real.jwt"));
    MockFilterChain chain = new MockFilterChain();

    when(jwtService.parseClaims("not.a.real.jwt"))
        .thenThrow(new MalformedJwtException("malformed"));

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).validateToken(any(Claims.class), any());
  }

  @Test
  void shouldRejectTokenMissingRoleClaim() throws ServletException, IOException {
    JwtService jwtService = mock(JwtService.class);
    UserRepositoryJPA userRepository = mock(UserRepositoryJPA.class);
    JwtAuthenticationFilter filter = filter(jwtService, userRepository);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(sessionCookie("jwt-token"));
    MockFilterChain chain = new MockFilterChain();
    Claims claims = mock(Claims.class);

    when(jwtService.parseClaims("jwt-token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn("driver@example.com");
    when(claims.get("userId", Number.class)).thenReturn(1);
    when(claims.get("role", String.class)).thenReturn(null);

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(jwtService, never()).validateToken(any(Claims.class), any());
  }

  private JwtAuthenticationFilter filter(JwtService jwtService, UserRepositoryJPA userRepository) {
    CookieSecurityProperties properties = new CookieSecurityProperties();
    properties.setSecure(true);
    return new JwtAuthenticationFilter(jwtService, userRepository, new AuthCookies(properties));
  }

  private static jakarta.servlet.http.Cookie sessionCookie(String accessToken) {
    return new jakarta.servlet.http.Cookie(
        AuthCookies.SESSION_COOKIE_NAME, SessionCookieCodec.encode(accessToken, "refresh-token"));
  }
}
