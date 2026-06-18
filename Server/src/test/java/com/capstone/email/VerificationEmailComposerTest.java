package com.capstone.email;

import static org.junit.jupiter.api.Assertions.*;

import com.capstone.models.AccountChangeType;
import org.junit.jupiter.api.Test;

class VerificationEmailComposerTest {

  private final VerificationEmailComposer composer = new VerificationEmailComposer();

  @Test
  void shouldRenderBrandedVerificationEmail() {
    EmailContent content = composer.compose("Pat", "123456", 5);

    assertEquals("Verify your Honest Car account", content.subject());
    assertTrue(content.textPart().contains("Hi Pat"));
    assertTrue(content.textPart().contains("123456"));
    assertTrue(content.textPart().contains("expires in 5 minutes"));

    String html = content.htmlPart();
    assertTrue(html.contains("Pat"));
    assertTrue(html.contains("123456"));
    assertTrue(html.contains("expires in 5 minutes") || html.contains("5 minutes"));
    assertTrue(html.contains("Honest Car"));
    assertTrue(html.contains("#ffffff"));
    assertTrue(html.contains("#2f6fb4"));
    assertTrue(html.contains("Verify your email"));
    assertTrue(html.contains("Your verification code"));
  }

  @Test
  void shouldRenderBrandedPasswordChangeEmail() {
    EmailContent content =
        composer.composeAccountChange("Pat", "123456", 5, AccountChangeType.PASSWORD);

    assertEquals("Confirm your Honest Car account change", content.subject());
    assertTrue(content.textPart().contains("Hi Pat"));
    assertTrue(content.textPart().contains("change your password"));
    assertTrue(content.textPart().contains("123456"));

    String html = content.htmlPart();
    assertTrue(html.contains("Confirm your password change"));
    assertTrue(html.contains("Account security"));
    assertTrue(html.contains("123456"));
    assertTrue(html.contains("Honest Car"));
    assertTrue(html.contains("#ffffff"));
    assertTrue(html.contains("#2f6fb4"));
  }

  @Test
  void shouldEscapeRecipientNameInHtml() {
    EmailContent content = composer.compose("Pat & Co", "654321", 10);

    assertTrue(content.htmlPart().contains("Pat &amp; Co"));
    assertFalse(content.htmlPart().contains("Pat & Co"));
    assertTrue(content.textPart().contains("Pat & Co"));
  }
}
