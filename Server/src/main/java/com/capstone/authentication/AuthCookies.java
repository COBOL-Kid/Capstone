package com.capstone.authentication;

import com.capstone.configuration.CookieSecurityProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookies {

  static final String ACCESS_TOKEN_NAME = "accessToken";
  static final String REFRESH_TOKEN_NAME = "refreshToken";
  private static final String ACCESS_PATH = "/api";
  private static final String REFRESH_PATH = "/api/auth";
  private static final String SAME_SITE = "Strict";

  private final CookieSecurityProperties cookieSecurityProperties;

  public AuthCookies(CookieSecurityProperties cookieSecurityProperties) {
    this.cookieSecurityProperties = cookieSecurityProperties;
  }

  public HttpHeaders setSessionCookies(
      String accessToken,
      String refreshToken,
      long accessMaxAgeSeconds,
      long refreshMaxAgeSeconds) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.SET_COOKIE,
        buildCookie(ACCESS_TOKEN_NAME, accessToken, ACCESS_PATH, accessMaxAgeSeconds).toString());
    headers.add(
        HttpHeaders.SET_COOKIE,
        buildCookie(REFRESH_TOKEN_NAME, refreshToken, REFRESH_PATH, refreshMaxAgeSeconds)
            .toString());
    return headers;
  }

  public HttpHeaders setAccessCookie(String accessToken, long maxAgeSeconds) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.SET_COOKIE,
        buildCookie(ACCESS_TOKEN_NAME, accessToken, ACCESS_PATH, maxAgeSeconds).toString());
    return headers;
  }

  public HttpHeaders setRefreshCookie(String refreshToken, long maxAgeSeconds) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.SET_COOKIE,
        buildCookie(REFRESH_TOKEN_NAME, refreshToken, REFRESH_PATH, maxAgeSeconds).toString());
    return headers;
  }

  public HttpHeaders clearSessionCookies() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN_NAME, "", ACCESS_PATH, 0).toString());
    headers.add(
        HttpHeaders.SET_COOKIE, buildCookie(REFRESH_TOKEN_NAME, "", REFRESH_PATH, 0).toString());
    return headers;
  }

  private ResponseCookie buildCookie(String name, String value, String path, long maxAgeSeconds) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(cookieSecurityProperties.isSecure())
        .path(path)
        .maxAge(maxAgeSeconds)
        .sameSite(SAME_SITE)
        .build();
  }
}
