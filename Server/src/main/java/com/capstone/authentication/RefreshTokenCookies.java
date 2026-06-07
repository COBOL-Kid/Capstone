package com.capstone.authentication;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public final class RefreshTokenCookies {

  private static final String NAME = "refreshToken";
  private static final String PATH = "/api/auth";
  private static final String SAME_SITE = "Strict";

  private RefreshTokenCookies() {}

  public static HttpHeaders setCookie(String token, long maxAgeSeconds) {
    return cookieHeaders(token, maxAgeSeconds);
  }

  public static HttpHeaders clearCookie() {
    return cookieHeaders("", 0);
  }

  private static HttpHeaders cookieHeaders(String token, long maxAgeSeconds) {
    ResponseCookie cookie =
        ResponseCookie.from(NAME, token)
            .httpOnly(true)
            .secure(true)
            .path(PATH)
            .maxAge(maxAgeSeconds)
            .sameSite(SAME_SITE)
            .build();
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
    return headers;
  }
}
