package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.configuration.CookieSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class AuthCookiesTest {

  @Test
  void shouldSetSessionCookiesWithExpectedAttributes() {
    CookieSecurityProperties properties = new CookieSecurityProperties();
    properties.setSecure(true);
    AuthCookies authCookies = new AuthCookies(properties);

    HttpHeaders headers =
        authCookies.setSessionCookies("access-value", "refresh-value", 3600, 604800);

    String accessCookie =
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .filter(cookie -> cookie.startsWith("accessToken="))
            .findFirst()
            .orElseThrow();
    String refreshCookie =
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .filter(cookie -> cookie.startsWith("refreshToken="))
            .findFirst()
            .orElseThrow();

    assertNotNull(accessCookie);
    assertTrue(accessCookie.contains("accessToken=access-value"));
    assertTrue(accessCookie.contains("HttpOnly"));
    assertTrue(accessCookie.contains("Secure"));
    assertTrue(accessCookie.contains("Path=/api"));
    assertTrue(accessCookie.contains("SameSite=Strict"));
    assertTrue(accessCookie.contains("Max-Age=3600"));

    assertNotNull(refreshCookie);
    assertTrue(refreshCookie.contains("refreshToken=refresh-value"));
    assertTrue(refreshCookie.contains("Path=/api/auth"));
    assertTrue(refreshCookie.contains("Max-Age=604800"));
  }

  @Test
  void shouldClearSessionCookiesWithExpectedAttributes() {
    CookieSecurityProperties properties = new CookieSecurityProperties();
    AuthCookies authCookies = new AuthCookies(properties);

    HttpHeaders headers = authCookies.clearSessionCookies();

    assertTrue(
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .anyMatch(cookie -> cookie.contains("accessToken=") && cookie.contains("Max-Age=0")));
    assertTrue(
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .anyMatch(cookie -> cookie.contains("refreshToken=") && cookie.contains("Max-Age=0")));
  }
}
