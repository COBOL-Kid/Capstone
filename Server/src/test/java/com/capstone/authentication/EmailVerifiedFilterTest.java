package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.capstone.models.Role;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class EmailVerifiedFilterTest {

  private final EmailVerifiedFilter filter = new EmailVerifiedFilter();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldAllowUnverifiedUsersToReadAccountDetails() throws ServletException, IOException {
    setAuthentication(false);
    MockHttpServletRequest request =
        new MockHttpServletRequest(HttpMethod.GET.name(), "/api/account/me");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
  }

  @Test
  void shouldBlockUnverifiedUsersFromVehicleEndpoints() throws ServletException, IOException {
    setAuthentication(false);
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/api/vin");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(403, response.getStatus());
  }

  @Test
  void shouldAllowVerifiedUsersToAccessProtectedEndpoints() throws ServletException, IOException {
    setAuthentication(true);
    MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/api/vin");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
  }

  private static void setAuthentication(boolean emailVerified) {
    AuthenticatedUser user =
        new AuthenticatedUser(1L, "driver@example.com", Role.USER, emailVerified);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
  }
}
