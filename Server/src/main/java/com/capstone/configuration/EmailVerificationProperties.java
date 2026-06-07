package com.capstone.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.email-verification")
public class EmailVerificationProperties {

  private int codeExpirationMinutes = 5;
  private int unverifiedAccountRetentionHours = 24;
  private int maxVerificationAttempts = 5;
  private int resendCooldownSeconds = 60;

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

  public int getMaxVerificationAttempts() {
    return maxVerificationAttempts;
  }

  public void setMaxVerificationAttempts(int maxVerificationAttempts) {
    this.maxVerificationAttempts = maxVerificationAttempts;
  }

  public int getResendCooldownSeconds() {
    return resendCooldownSeconds;
  }

  public void setResendCooldownSeconds(int resendCooldownSeconds) {
    this.resendCooldownSeconds = resendCooldownSeconds;
  }
}
