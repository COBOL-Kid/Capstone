package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginRateLimitFilterTest {

  private LoginRateLimitFilter filter;
  private MockHttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new LoginRateLimitFilter(3, 15);
    response = new MockHttpServletResponse();
    filterChain = mock(FilterChain.class);
  }

  @Test
  void shouldRateLimitByRemoteAddr() throws Exception {
    MockHttpServletRequest request = authenticateRequest("203.0.113.10");

    for (int attempt = 0; attempt < 3; attempt++) {
      filter.doFilter(request, response, filterChain);
    }

    MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
    filter.doFilter(request, blockedResponse, filterChain);

    assertEquals(429, blockedResponse.getStatus());
    verify(filterChain, times(3)).doFilter(request, response);
  }

  @Test
  void shouldNotRateLimitDifferentRemoteAddrs() throws Exception {
    for (int attempt = 0; attempt < 3; attempt++) {
      filter.doFilter(authenticateRequest("203.0.113.11"), response, filterChain);
    }

    MockHttpServletResponse otherClientResponse = new MockHttpServletResponse();
    FilterChain otherClientChain = mock(FilterChain.class);
    MockHttpServletRequest otherClientRequest = authenticateRequest("203.0.113.12");
    filter.doFilter(otherClientRequest, otherClientResponse, otherClientChain);

    assertEquals(200, otherClientResponse.getStatus());
    verify(otherClientChain).doFilter(otherClientRequest, otherClientResponse);
  }

  @Test
  void shouldIgnoreNonAuthenticateRequests() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/account/me");
    request.setRemoteAddr("203.0.113.10");

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  private static MockHttpServletRequest authenticateRequest(String remoteAddr) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/authenticate");
    request.setRemoteAddr(remoteAddr);
    return request;
  }
}
