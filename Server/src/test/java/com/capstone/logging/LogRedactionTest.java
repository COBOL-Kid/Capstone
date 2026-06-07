package com.capstone.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LogRedactionTest {

  @Test
  void shouldMaskVin() {
    assertEquals("****5678", LogRedaction.maskVin("1HGCM82633A12345678"));
    assertEquals("****", LogRedaction.maskVin("ABC"));
    assertEquals("****", LogRedaction.maskVin(null));
  }

  @Test
  void shouldExtractEmailDomain() {
    assertEquals("example.com", LogRedaction.emailDomain("user@example.com"));
    assertEquals("unknown", LogRedaction.emailDomain("not-an-email"));
    assertEquals("unknown", LogRedaction.emailDomain(null));
  }
}
