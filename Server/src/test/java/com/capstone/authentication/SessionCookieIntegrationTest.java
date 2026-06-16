package com.capstone.authentication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.support.IntegrationTestProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SessionCookieIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @DynamicPropertySource
  static void h2FlywaySeedProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2FlywaySeed("session-cookie-integration")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @Test
  void authenticatedGetAccountUsesSessionCookie() throws Exception {
    MockHttpServletResponse bootstrap =
        mockMvc.perform(get("/actuator/health")).andReturn().getResponse();

    MvcResult loginResult =
        mockMvc
            .perform(
                post("/api/auth/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-XSRF-TOKEN", readCsrf(bootstrap))
                    .cookie(bootstrap.getCookies())
                    .content(
                        """
                        {"email":"test.user@example.com","password":"Password123!"}
                        """))
            .andExpect(status().isOk())
            .andReturn();

    Cookie[] sessionCookies = loginResult.getResponse().getCookies();
    Cookie sessionCookie = findCookie(sessionCookies, AuthCookies.SESSION_COOKIE_NAME);
    if (sessionCookie == null) {
      throw new IllegalStateException("Missing __session cookie after login");
    }

    mockMvc
        .perform(get("/api/account/me").cookie(sessionCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test.user@example.com"));
  }

  private static Cookie findCookie(Cookie[] cookies, String name) {
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  private static String readCsrf(MockHttpServletResponse response) {
    Cookie[] cookies = response.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("XSRF-TOKEN".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    throw new IllegalStateException("Missing XSRF-TOKEN cookie");
  }
}
