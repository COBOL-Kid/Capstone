package com.capstone.authentication;

import com.capstone.models.Role;
import com.capstone.models.User;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipAuthenticationWhenBearerHeaderIsMissing() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUserEmail(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void shouldSetAuthenticationForValidBearerToken() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer jwt-token");
        User user = user();

        when(jwtService.extractUserEmail("jwt-token")).thenReturn("driver@example.com");
        when(userDetailsService.loadUserByUsername("driver@example.com")).thenReturn(user);
        when(jwtService.validateToken("jwt-token", user)).thenReturn(true);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertSame(user, authentication.getPrincipal());
        assertEquals(user.getAuthorities(), authentication.getAuthorities());
        verify(jwtService).validateToken("jwt-token", user);
    }

    @Test
    void shouldLeaveContextEmptyWhenTokenIsInvalid() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer jwt-token");
        UserDetails user = user();

        when(jwtService.extractUserEmail("jwt-token")).thenReturn("driver@example.com");
        when(userDetailsService.loadUserByUsername("driver@example.com")).thenReturn(user);
        when(jwtService.validateToken("jwt-token", user)).thenReturn(false);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldRejectMalformedTokenWithoutCrashingFilterChain() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not.a.real.jwt");
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUserEmail("not.a.real.jwt")).thenThrow(new MalformedJwtException("malformed"));

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void shouldRejectTokenForUnknownUserWithoutCrashingFilterChain() throws ServletException, IOException {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer jwt-token");
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUserEmail("jwt-token")).thenReturn("ghost@example.com");
        when(userDetailsService.loadUserByUsername("ghost@example.com"))
                .thenThrow(new UsernameNotFoundException("not found"));

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    private User user() {
        User user = new User();
        user.setUserId(1L);
        user.setUserEmail("driver@example.com");
        user.setUserPw("encoded-secret");
        user.setRole(Role.USER);
        return user;
    }
}
