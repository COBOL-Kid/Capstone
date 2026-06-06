package com.capstone.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.email-verification")
public class EmailVerificationProperties {

  private int codeExpirationMinutes = 5;
  private int unverifiedAccountRetentionHours = 24;

  public int getCodeExpirationMinutes() {
    return codeExpirationMinutes;
  }

  public void setCodeExpirationMinutes(int codeExpirationMinutes) {
    this.codeExpirationMinutes = codeExpirationMinutes;
  }

  public int getUnverifiedAccountRetentionHours() {
    return unverifiedAccountRetentionHours;
  }

  public void setUnverifiedAccountRetentionHours(int unverifiedAccountRetentionHours) {
    this.unverifiedAccountRetentionHours = unverifiedAccountRetentionHours;
  }
}
