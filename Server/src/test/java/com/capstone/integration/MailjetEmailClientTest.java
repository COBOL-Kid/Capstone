package com.capstone.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(
    properties = {
      "mailjet.enabled=true",
      "mailjet.api-key-public=test-public",
      "mailjet.api-key-private=test-private",
      "mailjet.from-email=sender@example.com",
      "mailjet.from-name=Honest Car"
    })
class MailjetEmailClientTest {

  @MockitoBean private MailjetClient mailjetClient;

  @Autowired private MailjetEmailClient mailjetEmailClient;

  @Test
  void shouldMapSuccessfulMailjetResponse() throws MailjetException {
    MailjetResponse response = mock(MailjetResponse.class);
    when(response.getStatus()).thenReturn(HttpStatus.OK.value());
    when(response.getData()).thenReturn(successResponseData());
    when(mailjetClient.post(any())).thenReturn(response);

    EmailSendResult result =
        mailjetEmailClient.sendEmail(
            "passenger1@mailjet.com",
            "passenger 1",
            "Your email flight plan!",
            "Dear passenger 1, welcome to Mailjet!",
            "<h3>Welcome to Mailjet</h3>");

    assertEquals("success", result.status());
    assertEquals(1, result.to().size());
    EmailRecipientResult recipient = result.to().getFirst();
    assertEquals("passenger1@mailjet.com", recipient.email());
    assertEquals("123", recipient.messageUuid());
    assertEquals(456L, recipient.messageId());
    assertEquals("https://api.mailjet.com/v3/message/456", recipient.messageHref());
  }

  @Test
  void shouldThrowWhenMessageStatusIsNotSuccess() throws MailjetException {
    MailjetResponse response = mock(MailjetResponse.class);
    when(response.getStatus()).thenReturn(HttpStatus.OK.value());
    when(response.getData())
        .thenReturn(
            new JSONArray().put(new JSONObject().put("Status", "error").put("Errors", "failed")));
    when(mailjetClient.post(any())).thenReturn(response);

    assertThrows(
        EmailDeliveryException.class,
        () ->
            mailjetEmailClient.sendEmail(
                "passenger1@mailjet.com", "passenger 1", "Subject", "text", "<p>html</p>"));
  }

  @Test
  void shouldThrowWhenMailjetClientFails() throws MailjetException {
    when(mailjetClient.post(any())).thenThrow(new MailjetException("connection failed"));

    EmailDeliveryException ex =
        assertThrows(
            EmailDeliveryException.class,
            () ->
                mailjetEmailClient.sendEmail(
                    "passenger1@mailjet.com", "passenger 1", "Subject", "text", "<p>html</p>"));

    assertEquals("Failed to send email via Mailjet", ex.getMessage());
    assertNotNull(ex.getCause());
  }

  private static JSONArray successResponseData() {
    return new JSONArray()
        .put(
            new JSONObject()
                .put("Status", "success")
                .put(
                    "To",
                    new JSONArray()
                        .put(
                            new JSONObject()
                                .put("Email", "passenger1@mailjet.com")
                                .put("MessageUUID", "123")
                                .put("MessageID", 456)
                                .put("MessageHref", "https://api.mailjet.com/v3/message/456"))));
  }
}
