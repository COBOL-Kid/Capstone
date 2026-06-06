package com.capstone.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@Tag("live")
@EnabledIfEnvironmentVariable(named = "RUN_MAILJET_LIVE_TESTS", matches = "true")
@SpringBootTest
@TestPropertySource(properties = "mailjet.enabled=true")
class MailjetEmailLiveSmokeTest {

  private static final Logger log = LoggerFactory.getLogger(MailjetEmailLiveSmokeTest.class);

  @Autowired(required = false)
  private MailjetEmailClient mailjetEmailClient;

  @Value("${mailjet.api-key-public}")
  private String apiKeyPublic;

  @Value("${mailjet.api-key-private}")
  private String apiKeyPrivate;

  @Test
  void shouldSendEmailViaMailjet() {
    assumeCredentialsPresent();
    assertNotNull(mailjetEmailClient);

    String toEmail = System.getenv("MAILJET_TEST_TO_EMAIL");
    assumeTrue(toEmail != null && !toEmail.isBlank(), "MAILJET_TEST_TO_EMAIL must be set");

    EmailSendResult result =
        mailjetEmailClient.sendEmail(
            toEmail,
            "Live Test",
            "Honest Car Mailjet live smoke test",
            "This is a live smoke test from Honest Car.",
            "<p>This is a <strong>live smoke test</strong> from Honest Car.</p>");

    assertEquals("success", result.status());
    assertFalse(result.to().isEmpty());
    log.info(
        "Live Mailjet send succeeded recipient={} messageId={}",
        result.to().getFirst().email(),
        result.to().getFirst().messageId());
  }

  private void assumeCredentialsPresent() {
    assumeTrue(
        apiKeyPublic != null
            && !apiKeyPublic.isBlank()
            && apiKeyPrivate != null
            && !apiKeyPrivate.isBlank(),
        "MAILJET_API_KEY_PUBLIC and MAILJET_API_KEY_PRIVATE must be set");
  }
}
