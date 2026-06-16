package com.capstone.authentication;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

final class SessionCookieCodec {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private SessionCookieCodec() {}

  static String encode(String accessToken, String refreshToken) {
    try {
      String json = MAPPER.writeValueAsString(new SessionCookiePayload(accessToken, refreshToken));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (JacksonException ex) {
      throw new IllegalStateException("Failed to encode session cookie", ex);
    }
  }

  static Optional<SessionCookiePayload> decode(String cookieValue) {
    if (cookieValue == null || cookieValue.isBlank()) {
      return Optional.empty();
    }
    try {
      String json = new String(Base64.getUrlDecoder().decode(cookieValue), StandardCharsets.UTF_8);
      SessionCookiePayload payload = MAPPER.readValue(json, SessionCookiePayload.class);
      if (payload.accessToken() == null || payload.accessToken().isBlank()) {
        return Optional.empty();
      }
      return Optional.of(payload);
    } catch (IllegalArgumentException | JacksonException ex) {
      return Optional.empty();
    }
  }
}
