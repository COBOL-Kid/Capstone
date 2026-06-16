package com.capstone.authentication;

import com.capstone.configuration.CookieSecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookies {

  /** Firebase Hosting forwards only this cookie name to Cloud Run on GET requests. */
  static final String SESSION_COOKIE_NAME = "__session";

  private static final String LEGACY_ACCESS_TOKEN_NAME = "accessToken";
  private static final String LEGACY_REFRESH_TOKEN_NAME = "refreshToken";
  private static final String SESSION_PATH = "/";
  private static final String LEGACY_ACCESS_PATH = "/api";
  private static final String LEGACY_REFRESH_PATH = "/api/auth";
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
        buildCookie(
                SESSION_COOKIE_NAME,
                SessionCookieCodec.encode(accessToken, refreshToken),
                SESSION_PATH,
                refreshMaxAgeSeconds)
            .toString());
    headers.addAll(clearLegacySessionCookies());
    return headers;
  }

  public HttpHeaders clearSessionCookies() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.SET_COOKIE, buildCookie(SESSION_COOKIE_NAME, "", SESSION_PATH, 0).toString());
    headers.addAll(clearLegacySessionCookies());
    return headers;
  }

  public Optional<String> readAccessToken(HttpServletRequest request) {
    Optional<SessionCookiePayload> session = readSessionCookie(request);
    if (session.isPresent()) {
      return Optional.of(session.get().accessToken());
    }
    return readLegacyCookie(request, LEGACY_ACCESS_TOKEN_NAME);
  }

  public Optional<String> readRefreshToken(HttpServletRequest request) {
    Optional<SessionCookiePayload> session = readSessionCookie(request);
    if (session.isPresent()) {
      String refreshToken = session.get().refreshToken();
      if (refreshToken != null && !refreshToken.isBlank()) {
        return Optional.of(refreshToken);
      }
      return Optional.empty();
    }
    return readLegacyCookie(request, LEGACY_REFRESH_TOKEN_NAME);
  }

  private HttpHeaders clearLegacySessionCookies() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.SET_COOKIE,
        buildCookie(LEGACY_ACCESS_TOKEN_NAME, "", LEGACY_ACCESS_PATH, 0).toString());
    headers.add(
        HttpHeaders.SET_COOKIE,
        buildCookie(LEGACY_REFRESH_TOKEN_NAME, "", LEGACY_REFRESH_PATH, 0).toString());
    return headers;
  }

  private Optional<SessionCookiePayload> readSessionCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    for (Cookie cookie : cookies) {
      if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
        return SessionCookieCodec.decode(cookie.getValue());
      }
    }
    return Optional.empty();
  }

  private static Optional<String> readLegacyCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName())) {
        String value = cookie.getValue();
        if (value != null && !value.isBlank()) {
          return Optional.of(value);
        }
      }
    }
    return Optional.empty();
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
