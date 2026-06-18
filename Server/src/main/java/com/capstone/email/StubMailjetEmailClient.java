package com.capstone.email;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "security.e2e", name = "stub-email", havingValue = "true")
public class StubMailjetEmailClient {

  private static final Logger log = LoggerFactory.getLogger(StubMailjetEmailClient.class);

  public EmailSendResult sendEmail(
      String toEmail, String toName, String subject, String textPart, String htmlPart) {
    log.debug(
        "E2E stub email to={} subject={} textLength={} htmlLength={}",
        toEmail,
        subject,
        textPart == null ? 0 : textPart.length(),
        htmlPart == null ? 0 : htmlPart.length());
    return new EmailSendResult(
        "success", List.of(new EmailRecipientResult(toEmail, "e2e-stub-message-uuid", 0L, "")));
  }
}
