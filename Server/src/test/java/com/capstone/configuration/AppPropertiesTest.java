package com.capstone.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("prod")
class AppPropertiesTest {

  @Autowired private AppProperties appProperties;

  @Test
  void prodProfileUsesHonestCarPublicUrlByDefault() {
    assertEquals("https://honest-car.co", appProperties.getPublicUrl());
  }
}
