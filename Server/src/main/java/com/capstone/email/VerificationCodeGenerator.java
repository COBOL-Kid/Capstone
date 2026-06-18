package com.capstone.email;

import com.capstone.configuration.E2eProperties;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class VerificationCodeGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final String fixedVerificationCode;

  public VerificationCodeGenerator(E2eProperties e2eProperties) {
    this.fixedVerificationCode =
        StringUtils.hasText(e2eProperties.getFixedVerificationCode())
            ? e2eProperties.getFixedVerificationCode().trim()
            : null;
  }

  public String generate() {
    if (fixedVerificationCode != null) {
      return fixedVerificationCode;
    }
    int value = RANDOM.nextInt(900_000) + 100_000;
    return Integer.toString(value);
  }
}
