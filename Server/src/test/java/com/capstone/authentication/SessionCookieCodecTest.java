package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SessionCookieCodecTest {

  @Test
  void encodesAndDecodesSessionTokens() {
    String encoded = SessionCookieCodec.encode("access-jwt", "refresh-uuid");

    SessionCookiePayload payload =
        SessionCookieCodec.decode(encoded).orElseThrow(() -> new AssertionError("missing payload"));

    assertEquals("access-jwt", payload.accessToken());
    assertEquals("refresh-uuid", payload.refreshToken());
  }

  @Test
  void rejectsInvalidEncodedPayload() {
    assertTrue(SessionCookieCodec.decode("not-valid-base64!!!").isEmpty());
  }
}
