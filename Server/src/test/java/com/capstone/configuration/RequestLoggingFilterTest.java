package com.capstone.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.capstone.authentication.AuthenticatedUser;
import com.capstone.logging.RequestContextMdc;
import com.capstone.models.Role;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestLoggingFilterTest {

  @AfterEach
  void tearDown() {
    MDC.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldParseTraceIdFromCloudTraceHeader() {
    assertEquals("abc123", RequestLoggingFilter.parseTraceId("abc123/1;o=1"));
    assertEquals("abc123", RequestLoggingFilter.parseTraceId("abc123"));
    assertNull(RequestLoggingFilter.parseTraceId(null));
    assertNull(RequestLoggingFilter.parseTraceId(" "));
  }

  @Test
  void shouldSetAndClearMdcAroundRequest() throws Exception {
    RequestLoggingFilter filter = new RequestLoggingFilter("test-project");
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/account/me");
    request.addHeader("X-Cloud-Trace-Context", "trace-1/2;o=1");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(eq(request), any());
    assertNull(MDC.get(RequestContextMdc.REQUEST_ID));
    assertNull(MDC.get(RequestContextMdc.GCP_TRACE));
  }

  @Test
  void shouldCaptureResponseStatusInAccessLog() throws Exception {
    RequestLoggingFilter filter = new RequestLoggingFilter("");
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/authenticate");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain =
        (req, res) -> {
          if (res instanceof jakarta.servlet.http.HttpServletResponse httpResponse) {
            httpResponse.sendError(429, "Too many attempts");
          }
        };

    filter.doFilter(request, response, chain);

    assertEquals(429, response.getStatus());
  }

  @Test
  void shouldCaptureAuthenticatedUserIdWhenRunningInsideSecurityChain() throws Exception {
    Logger accessLogger = (Logger) LoggerFactory.getLogger("ACCESS");
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    accessLogger.addAppender(appender);

    try {
      RequestLoggingFilter filter = new RequestLoggingFilter("");
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/account/me");
      MockHttpServletResponse response = new MockHttpServletResponse();
      AuthenticatedUser user = new AuthenticatedUser(42L, "driver@example.com", Role.USER, true);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
      FilterChain chain =
          (req, res) -> SecurityContextHolder.getContext().setAuthentication(authentication);

      filter.doFilter(request, response, chain);

      assertEquals(1, appender.list.size());
      assertTrue(appender.list.get(0).getFormattedMessage().contains("userId=42"));
    } finally {
      accessLogger.detachAppender(appender);
    }
  }

  @Test
  void shouldNotCaptureUserIdWhenSecurityContextClearedBeforeAccessLog() throws Exception {
    Logger accessLogger = (Logger) LoggerFactory.getLogger("ACCESS");
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    accessLogger.addAppender(appender);

    try {
      RequestLoggingFilter filter = new RequestLoggingFilter("");
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/account/me");
      MockHttpServletResponse response = new MockHttpServletResponse();
      AuthenticatedUser user = new AuthenticatedUser(42L, "driver@example.com", Role.USER, true);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
      FilterChain securityChain =
          (req, res) -> SecurityContextHolder.getContext().setAuthentication(authentication);
      FilterChain servletChain =
          (req, res) -> {
            try {
              securityChain.doFilter(req, res);
            } finally {
              SecurityContextHolder.clearContext();
            }
          };

      filter.doFilter(request, response, servletChain);

      assertEquals(1, appender.list.size());
      assertTrue(appender.list.get(0).getFormattedMessage().contains("userId=null"));
    } finally {
      accessLogger.detachAppender(appender);
    }
  }
}
