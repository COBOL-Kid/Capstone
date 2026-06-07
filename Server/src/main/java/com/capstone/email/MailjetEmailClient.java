package com.capstone.email;

import com.capstone.configuration.MailjetProperties;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(MailjetClient.class)
public class MailjetEmailClient {

  private static final Logger log = LoggerFactory.getLogger(MailjetEmailClient.class);
  private static final String SUCCESS_STATUS = "success";

  private final MailjetClient mailjetClient;
  private final MailjetProperties mailjetProperties;

  public MailjetEmailClient(MailjetClient mailjetClient, MailjetProperties mailjetProperties) {
    this.mailjetClient = mailjetClient;
    this.mailjetProperties = mailjetProperties;
  }

  public EmailSendResult sendEmail(
      String toEmail, String toName, String subject, String textPart, String htmlPart) {
    MailjetRequest request =
        new MailjetRequest(Emailv31.resource)
            .property(
                Emailv31.MESSAGES,
                new JSONArray()
                    .put(
                        new JSONObject()
                            .put(
                                Emailv31.Message.FROM,
                                new JSONObject()
                                    .put("Email", mailjetProperties.getFromEmail())
                                    .put("Name", mailjetProperties.getFromName()))
                            .put(
                                Emailv31.Message.TO,
                                new JSONArray()
                                    .put(
                                        new JSONObject().put("Email", toEmail).put("Name", toName)))
                            .put(Emailv31.Message.SUBJECT, subject)
                            .put(Emailv31.Message.TEXTPART, textPart)
                            .put(Emailv31.Message.HTMLPART, htmlPart)));

    try {
      MailjetResponse response = mailjetClient.post(request);
      return parseResponse(response, toEmail);
    } catch (MailjetException ex) {
      log.error("Mailjet send failed for recipient={}", toEmail, ex);
      throw new EmailDeliveryException("Failed to send email via Mailjet", ex);
    }
  }

  private EmailSendResult parseResponse(MailjetResponse response, String toEmail) {
    int status = response.getStatus();
    if (status < HttpStatus.OK.value() || status >= HttpStatus.MULTIPLE_CHOICES.value()) {
      log.warn(
          "Mailjet HTTP error status={} recipient={} data={}", status, toEmail, response.getData());
      throw new EmailDeliveryException("Mailjet returned HTTP status " + status);
    }

    JSONArray messages = response.getData();
    if (messages == null || messages.isEmpty()) {
      throw new EmailDeliveryException("Mailjet response Messages array is empty");
    }

    JSONObject message = messages.getJSONObject(0);
    String messageStatus = message.optString("Status", "");
    if (!SUCCESS_STATUS.equalsIgnoreCase(messageStatus)) {
      log.warn(
          "Mailjet message not successful status={} recipient={} response={}",
          messageStatus,
          toEmail,
          response.getRawResponseContent());
      throw new EmailDeliveryException("Mailjet message status was " + messageStatus);
    }

    List<EmailRecipientResult> recipients = parseRecipients(message);
    for (EmailRecipientResult recipient : recipients) {
      log.info(
          "Mailjet send succeeded recipient={} messageId={}",
          recipient.email(),
          recipient.messageId());
    }

    return new EmailSendResult(messageStatus, recipients);
  }

  private static List<EmailRecipientResult> parseRecipients(JSONObject message) {
    List<EmailRecipientResult> recipients = new ArrayList<>();
    if (!message.has("To")) {
      return recipients;
    }

    JSONArray toArray = message.getJSONArray("To");
    for (int i = 0; i < toArray.length(); i++) {
      JSONObject to = toArray.getJSONObject(i);
      recipients.add(
          new EmailRecipientResult(
              to.optString("Email", ""),
              to.optString("MessageUUID", ""),
              to.optLong("MessageID", 0L),
              to.optString("MessageHref", "")));
    }
    return recipients;
  }
}
