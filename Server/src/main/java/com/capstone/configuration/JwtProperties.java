package com.capstone.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

  private String secret;
  private long expirationMinutes = 15;
  private long refreshExpirationDays = 7;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getExpirationMinutes() {
    return expirationMinutes;
  }

  public void setExpirationMinutes(long expirationMinutes) {
    this.expirationMinutes = expirationMinutes;
  }

  public long getRefreshExpirationDays() {
    return refreshExpirationDays;
  }

  public void setRefreshExpirationDays(long refreshExpirationDays) {
    this.refreshExpirationDays = refreshExpirationDays;
  }
}
