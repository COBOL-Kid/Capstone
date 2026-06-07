package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class RefreshTokenCookiesTest {

  @Test
  void shouldSetRefreshTokenCookieWithExpectedAttributes() {
    HttpHeaders headers = RefreshTokenCookies.setCookie("refresh-value", 604800);

    String setCookie = headers.getFirst(HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.contains("refreshToken=refresh-value"));
    assertTrue(setCookie.contains("HttpOnly"));
    assertTrue(setCookie.contains("Secure"));
    assertTrue(setCookie.contains("Path=/api/auth"));
    assertTrue(setCookie.contains("SameSite=Strict"));
    assertTrue(setCookie.contains("Max-Age=604800"));
  }

  @Test
  void shouldClearRefreshTokenCookieWithExpectedAttributes() {
    HttpHeaders headers = RefreshTokenCookies.clearCookie();

    String setCookie = headers.getFirst(HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.contains("refreshToken="));
    assertTrue(setCookie.contains("HttpOnly"));
    assertTrue(setCookie.contains("Secure"));
    assertTrue(setCookie.contains("Path=/api/auth"));
    assertTrue(setCookie.contains("SameSite=Strict"));
    assertTrue(setCookie.contains("Max-Age=0"));
  }
}
