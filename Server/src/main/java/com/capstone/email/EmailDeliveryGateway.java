package com.capstone.email;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EmailDeliveryGateway {

  private final Optional<MailjetEmailClient> mailjetEmailClient;
  private final Optional<StubMailjetEmailClient> stubMailjetEmailClient;

  public EmailDeliveryGateway(
      Optional<MailjetEmailClient> mailjetEmailClient,
      Optional<StubMailjetEmailClient> stubMailjetEmailClient) {
    this.mailjetEmailClient = mailjetEmailClient;
    this.stubMailjetEmailClient = stubMailjetEmailClient;
  }

  public EmailSendResult sendEmail(
      String toEmail, String toName, String subject, String textPart, String htmlPart) {
    if (stubMailjetEmailClient.isPresent()) {
      return stubMailjetEmailClient.get().sendEmail(toEmail, toName, subject, textPart, htmlPart);
    }
    return mailjetEmailClient
        .orElseThrow(() -> new EmailDeliveryException("Email delivery is not configured"))
        .sendEmail(toEmail, toName, subject, textPart, htmlPart);
  }
}
