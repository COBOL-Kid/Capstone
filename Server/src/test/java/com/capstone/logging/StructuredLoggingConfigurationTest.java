package com.capstone.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("prod")
class StructuredLoggingConfigurationTest {

  @Autowired private Environment environment;

  @Test
  void prodProfileEnablesStructuredJsonLogging() {
    assertEquals("logstash", environment.getProperty("logging.structured.format.console"));
    assertEquals("INFO", environment.getProperty("logging.level.root"));
    assertEquals("INFO", environment.getProperty("logging.level.AUDIT"));
    assertEquals("INFO", environment.getProperty("logging.level.ACCESS"));
  }
}
