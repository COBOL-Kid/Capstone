package com.capstone.email;

import com.capstone.models.AccountChangeType;
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
  private static final String ACCOUNT_CHANGE_SUBJECT = "Confirm your Honest Car account change";

  private final String htmlTemplate;

  public VerificationEmailComposer() {
    this.htmlTemplate = loadTemplate();
  }

  public EmailContent compose(
      String recipientName, String verificationCode, int expirationMinutes) {
    return render(
        recipientName,
        verificationCode,
        expirationMinutes,
        "Verify your Honest Car account",
        "Your Honest Car verification code is",
        "Account verification",
        "Verify your email",
        "enter this code in the Honest Car app to finish verifying your account.",
        "Your Honest Car verification code is "
            + verificationCode
            + ". It expires in "
            + expirationMinutes
            + " minutes.\n\n"
            + "Enter this 6-digit code in the app to verify your email address.\n\n"
            + "If you did not request this code, you can safely ignore this email.");
  }

  public EmailContent composeAccountChange(
      String recipientName,
      String verificationCode,
      int expirationMinutes,
      AccountChangeType changeType) {
    AccountChangeVerbiage verbiage = accountChangeVerbiage(changeType);
    return render(
        recipientName,
        verificationCode,
        expirationMinutes,
        verbiage.pageTitle(),
        verbiage.preheaderLead(),
        verbiage.badgeLabel(),
        verbiage.heading(),
        verbiage.introMessage(),
        "Your verification code to change your "
            + verbiage.changeLabel()
            + " is "
            + verificationCode
            + ". It expires in "
            + expirationMinutes
            + " minutes.\n\n"
            + "Enter this 6-digit code in the app to "
            + verbiage.textAction()
            + ".\n\n"
            + "If you did not request this code, you can safely ignore this email.");
  }

  private EmailContent render(
      String recipientName,
      String verificationCode,
      int expirationMinutes,
      String pageTitle,
      String preheaderLead,
      String badgeLabel,
      String heading,
      String introMessage,
      String textBody) {
    String escapedName = HtmlUtils.htmlEscape(recipientName);
    String html =
        htmlTemplate
            .replace("{{pageTitle}}", HtmlUtils.htmlEscape(pageTitle))
            .replace("{{preheaderLead}}", HtmlUtils.htmlEscape(preheaderLead))
            .replace("{{badgeLabel}}", HtmlUtils.htmlEscape(badgeLabel))
            .replace("{{heading}}", HtmlUtils.htmlEscape(heading))
            .replace("{{introMessage}}", HtmlUtils.htmlEscape(introMessage))
            .replace("{{recipientName}}", escapedName)
            .replace("{{codeCells}}", renderCodeCells(verificationCode))
            .replace("{{verificationCode}}", verificationCode)
            .replace("{{expirationMinutes}}", Integer.toString(expirationMinutes));

    String subject =
        pageTitle.equals("Verify your Honest Car account") ? pageTitle : ACCOUNT_CHANGE_SUBJECT;
    String text = "Hi " + recipientName + ",\n\n" + textBody;

    return new EmailContent(subject, text, html);
  }

  private static AccountChangeVerbiage accountChangeVerbiage(AccountChangeType changeType) {
    return switch (changeType) {
      case EMAIL ->
          new AccountChangeVerbiage(
              "Confirm your Honest Car email change",
              "Your verification code to change your email address is",
              "Account security",
              "Confirm your email change",
              "enter this code in the Honest Car app to confirm your new email address.",
              "email address",
              "confirm your new email address");
      case PASSWORD ->
          new AccountChangeVerbiage(
              "Confirm your Honest Car password change",
              "Your verification code to change your password is",
              "Account security",
              "Confirm your password change",
              "enter this code in the Honest Car app to confirm your new password.",
              "password",
              "confirm your new password");
      case SMS ->
          new AccountChangeVerbiage(
              "Confirm your Honest Car SMS change",
              "Your verification code to change your SMS phone number is",
              "Account security",
              "Confirm your SMS number change",
              "enter this code in the Honest Car app to confirm your new SMS phone number.",
              "SMS phone number",
              "confirm your new SMS phone number");
    };
  }

  private record AccountChangeVerbiage(
      String pageTitle,
      String preheaderLead,
      String badgeLabel,
      String heading,
      String introMessage,
      String changeLabel,
      String textAction) {}

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
