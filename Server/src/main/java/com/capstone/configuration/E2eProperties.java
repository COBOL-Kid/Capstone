package com.capstone.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.e2e")
public class E2eProperties {

  private boolean stubEmail = false;
  private String fixedVerificationCode = "";

  public boolean isStubEmail() {
    return stubEmail;
  }

  public void setStubEmail(boolean stubEmail) {
    this.stubEmail = stubEmail;
  }

  public String getFixedVerificationCode() {
    return fixedVerificationCode;
  }

  public void setFixedVerificationCode(String fixedVerificationCode) {
    this.fixedVerificationCode = fixedVerificationCode;
  }
}
