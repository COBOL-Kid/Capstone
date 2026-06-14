package com.capstone.authentication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.support.IntegrationTestProperties;
import com.capstone.support.MailjetTestSupport;
import com.mailjet.client.MailjetClient;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerIntegrationTest {

  @MockitoBean private MailjetClient mailjetClient;

  @Autowired private MockMvc mockMvc;

  @DynamicPropertySource
  static void h2CreateDropProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2CreateDrop("auth-controller-integration")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @BeforeEach
  void stubMailjet() throws Exception {
    MailjetTestSupport.stubSuccessfulSend(mailjetClient);
  }

  @Test
  void registerReturns400ForInvalidPayload() throws Exception {
    MockHttpServletResponse bootstrap = bootstrapCsrf();
    mockMvc
        .perform(
            withCsrf(
                bootstrap,
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"firstname":"","lastname":"D","email":"not-an-email","password":"short"}
                        """)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registerReturns409ForDuplicateEmail() throws Exception {
    String email = "dup-" + System.nanoTime() + "@example.com";
    String body =
        """
        {"firstname":"Pat","lastname":"Driver","email":"%s","password":"Password1!"}
        """
            .formatted(email);

    MvcResult first = performRegister(body);
    String csrf = readCsrf(first);
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-XSRF-TOKEN", csrf)
                .cookie(registerCookies(first))
                .content(body))
        .andExpect(status().isConflict())
        .andExpect(content().string("Unable to complete registration"));
  }

  private MvcResult performRegister(String body) throws Exception {
    MockHttpServletResponse bootstrap = bootstrapCsrf();
    return mockMvc
        .perform(
            withCsrf(
                bootstrap,
                post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)))
        .andExpect(status().isOk())
        .andReturn();
  }

  private MockHttpServletResponse bootstrapCsrf() throws Exception {
    return mockMvc.perform(get("/actuator/health")).andReturn().getResponse();
  }

  private static MockHttpServletRequestBuilder withCsrf(
      MockHttpServletResponse bootstrap, MockHttpServletRequestBuilder request) {
    return request.header("X-XSRF-TOKEN", readCsrf(bootstrap)).cookie(bootstrap.getCookies());
  }

  private static Cookie[] registerCookies(MvcResult registerResult) {
    Cookie[] requestCookies = registerResult.getRequest().getCookies();
    Cookie[] responseCookies = registerResult.getResponse().getCookies();
    if (requestCookies == null || requestCookies.length == 0) {
      return responseCookies;
    }
    if (responseCookies == null || responseCookies.length == 0) {
      return requestCookies;
    }
    Cookie[] merged = new Cookie[requestCookies.length + responseCookies.length];
    System.arraycopy(requestCookies, 0, merged, 0, requestCookies.length);
    System.arraycopy(responseCookies, 0, merged, requestCookies.length, responseCookies.length);
    return merged;
  }

  private static String readCsrf(MvcResult registerResult) {
    Cookie[] cookies = registerResult.getRequest().getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("XSRF-TOKEN".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    throw new IllegalStateException("Missing XSRF-TOKEN cookie");
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
