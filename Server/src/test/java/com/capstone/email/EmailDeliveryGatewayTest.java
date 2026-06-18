package com.capstone.email;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmailDeliveryGatewayTest {

  @Test
  void shouldUseStubClientWhenConfigured() {
    StubMailjetEmailClient stub = new StubMailjetEmailClient();
    EmailDeliveryGateway gateway = new EmailDeliveryGateway(Optional.empty(), Optional.of(stub));

    EmailSendResult result =
        gateway.sendEmail(
            "driver@example.com",
            "Pat",
            "Verify your email",
            "Your code is 123456",
            "<p>Your code is 123456</p>");

    assertEquals("success", result.status());
    assertEquals(1, result.to().size());
    assertEquals("driver@example.com", result.to().getFirst().email());
  }

  @Test
  void shouldUseMailjetClientWhenStubIsAbsent() {
    MailjetEmailClient mailjet = mock(MailjetEmailClient.class);
    when(mailjet.sendEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new EmailSendResult("success", java.util.List.of()));
    EmailDeliveryGateway gateway = new EmailDeliveryGateway(Optional.of(mailjet), Optional.empty());

    gateway.sendEmail("driver@example.com", "Pat", "Subject", "Text", "Html");

    verify(mailjet).sendEmail("driver@example.com", "Pat", "Subject", "Text", "Html");
  }

  @Test
  void shouldThrowWhenNoEmailClientIsConfigured() {
    EmailDeliveryGateway gateway = new EmailDeliveryGateway(Optional.empty(), Optional.empty());

    assertThrows(
        EmailDeliveryException.class,
        () -> gateway.sendEmail("driver@example.com", "Pat", "Subject", "Text", "Html"));
  }
}
