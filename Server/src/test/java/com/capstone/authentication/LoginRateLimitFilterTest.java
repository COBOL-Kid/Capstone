package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginRateLimitFilterTest {

  private LoginRateLimitFilter filter;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter = new LoginRateLimitFilter(3, 15);
    filterChain = mock(FilterChain.class);
  }

  @Test
  void shouldRateLimitByRemoteAddrAfterFailedAttempts() throws Exception {
    MockHttpServletRequest request = authenticateRequest("203.0.113.10");
    FilterChain unauthorizedChain = unauthorizedChain();

    for (int attempt = 0; attempt < 3; attempt++) {
      MockHttpServletResponse attemptResponse = new MockHttpServletResponse();
      filter.doFilter(request, attemptResponse, unauthorizedChain);
      assertEquals(HttpStatus.UNAUTHORIZED.value(), attemptResponse.getStatus());
    }

    MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
    filter.doFilter(request, blockedResponse, unauthorizedChain());

    assertEquals(429, blockedResponse.getStatus());
  }

  @Test
  void shouldNotRateLimitSuccessfulLogins() throws Exception {
    MockHttpServletRequest request = authenticateRequest("203.0.113.11");
    FilterChain successChain = successChain();

    for (int attempt = 0; attempt < 5; attempt++) {
      MockHttpServletResponse attemptResponse = new MockHttpServletResponse();
      filter.doFilter(request, attemptResponse, successChain);
      assertEquals(HttpStatus.OK.value(), attemptResponse.getStatus());
    }
  }

  @Test
  void shouldNotRateLimitDifferentRemoteAddrs() throws Exception {
    FilterChain unauthorizedChain = unauthorizedChain();
    for (int attempt = 0; attempt < 3; attempt++) {
      MockHttpServletResponse attemptResponse = new MockHttpServletResponse();
      filter.doFilter(authenticateRequest("203.0.113.12"), attemptResponse, unauthorizedChain);
    }

    MockHttpServletResponse otherClientResponse = new MockHttpServletResponse();
    filter.doFilter(authenticateRequest("203.0.113.13"), otherClientResponse, successChain());

    assertEquals(HttpStatus.OK.value(), otherClientResponse.getStatus());
  }

  @Test
  void shouldIgnoreNonAuthenticateRequests() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/account/me");
    request.setRemoteAddr("203.0.113.10");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  private static FilterChain unauthorizedChain() {
    return (request, response) ->
        ((HttpServletResponse) response).setStatus(HttpStatus.UNAUTHORIZED.value());
  }

  private static FilterChain successChain() {
    return (request, response) -> ((HttpServletResponse) response).setStatus(HttpStatus.OK.value());
  }

  private static MockHttpServletRequest authenticateRequest(String remoteAddr) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/authenticate");
    request.setRemoteAddr(remoteAddr);
    return request;
  }
}
