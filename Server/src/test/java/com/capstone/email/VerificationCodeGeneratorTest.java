package com.capstone.email;

import static org.junit.jupiter.api.Assertions.*;

import com.capstone.configuration.E2eProperties;
import org.junit.jupiter.api.Test;

class VerificationCodeGeneratorTest {

  @Test
  void shouldReturnFixedCodeWhenConfigured() {
    E2eProperties properties = new E2eProperties();
    properties.setFixedVerificationCode("123456");

    VerificationCodeGenerator generator = new VerificationCodeGenerator(properties);

    assertEquals("123456", generator.generate());
    assertEquals("123456", generator.generate());
  }

  @Test
  void shouldGenerateSixDigitCodeWhenFixedCodeIsNotConfigured() {
    E2eProperties properties = new E2eProperties();

    VerificationCodeGenerator generator = new VerificationCodeGenerator(properties);

    String code = generator.generate();
    assertTrue(code.matches("\\d{6}"));
  }
}
