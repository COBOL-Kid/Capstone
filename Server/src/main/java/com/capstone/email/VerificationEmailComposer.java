package com.capstone.email;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

@Component
public class VerificationEmailComposer {

  private static final String TEMPLATE_PATH = "templates/email/verification-code.html";
  private static final String SUBJECT = "Verify your Honest Car account";

  private final String htmlTemplate;

  public VerificationEmailComposer() {
    this.htmlTemplate = loadTemplate();
  }

  public EmailContent compose(
      String recipientName, String verificationCode, int expirationMinutes) {
    String escapedName = HtmlUtils.htmlEscape(recipientName);
    String html =
        htmlTemplate
            .replace("{{recipientName}}", escapedName)
            .replace("{{codeCells}}", renderCodeCells(verificationCode))
            .replace("{{verificationCode}}", verificationCode)
            .replace("{{expirationMinutes}}", Integer.toString(expirationMinutes));

    String text =
        "Hi "
            + recipientName
            + ",\n\n"
            + "Your Honest Car verification code is "
            + verificationCode
            + ". It expires in "
            + expirationMinutes
            + " minutes.\n\n"
            + "Enter this 6-digit code in the app to verify your email address.\n\n"
            + "If you did not request this code, you can safely ignore this email.";

    return new EmailContent(SUBJECT, text, html);
  }

  private static final String CODE_CELL =
      "<td align=\"center\" valign=\"middle\" width=\"44\""
          + " style=\"width:44px;height:54px;background-color:#f0f5fc;border:1px solid #c7dbf2;"
          + "border-radius:10px;font-family:'Courier New',Courier,monospace;font-size:30px;"
          + "font-weight:700;color:#0b2545;text-align:center;\">%s</td>";

  private static final String CODE_CELL_SPACER =
      "<td style=\"width:10px;font-size:0;line-height:0;\">&#8202;</td>";

  private static String renderCodeCells(String verificationCode) {
    StringBuilder cells = new StringBuilder();
    for (int i = 0; i < verificationCode.length(); i++) {
      if (i > 0) {
        cells.append(CODE_CELL_SPACER);
      }
      cells.append(String.format(CODE_CELL, verificationCode.charAt(i)));
    }
    return cells.toString();
  }

  private static String loadTemplate() {
    try (InputStream inputStream = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
      return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load email template: " + TEMPLATE_PATH, ex);
    }
  }
}
