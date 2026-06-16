package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.capstone.configuration.CookieSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class AuthCookiesTest {

  @Test
  void shouldSetSessionCookieWithExpectedAttributes() {
    CookieSecurityProperties properties = new CookieSecurityProperties();
    properties.setSecure(true);
    AuthCookies authCookies = new AuthCookies(properties);

    HttpHeaders headers =
        authCookies.setSessionCookies("access-value", "refresh-value", 3600, 604800);

    String sessionCookie =
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .filter(cookie -> cookie.startsWith("__session="))
            .findFirst()
            .orElseThrow();

    assertNotNull(sessionCookie);
    assertTrue(sessionCookie.contains("HttpOnly"));
    assertTrue(sessionCookie.contains("Secure"));
    assertTrue(sessionCookie.contains("Path=/"));
    assertTrue(sessionCookie.contains("SameSite=Strict"));
    assertTrue(sessionCookie.contains("Max-Age=604800"));

    assertTrue(
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .anyMatch(cookie -> cookie.startsWith("accessToken=") && cookie.contains("Max-Age=0")));
    assertTrue(
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .anyMatch(
                cookie -> cookie.startsWith("refreshToken=") && cookie.contains("Max-Age=0")));
  }

  @Test
  void shouldClearSessionAndLegacyCookies() {
    CookieSecurityProperties properties = new CookieSecurityProperties();
    AuthCookies authCookies = new AuthCookies(properties);

    HttpHeaders headers = authCookies.clearSessionCookies();

    assertTrue(
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .anyMatch(cookie -> cookie.startsWith("__session=") && cookie.contains("Max-Age=0")));
    assertTrue(
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .anyMatch(cookie -> cookie.contains("accessToken=") && cookie.contains("Max-Age=0")));
    assertTrue(
        headers.get(HttpHeaders.SET_COOKIE).stream()
            .anyMatch(cookie -> cookie.contains("refreshToken=") && cookie.contains("Max-Age=0")));
  }
}
