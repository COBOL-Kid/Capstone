package com.capstone.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MailjetProperties {

  private final boolean enabled;
  private final String apiKeyPublic;
  private final String apiKeyPrivate;
  private final String fromEmail;
  private final String fromName;

  public MailjetProperties(
      @Value("${mailjet.enabled}") boolean enabled,
      @Value("${mailjet.api-key-public}") String apiKeyPublic,
      @Value("${mailjet.api-key-private}") String apiKeyPrivate,
      @Value("${mailjet.from-email}") String fromEmail,
      @Value("${mailjet.from-name}") String fromName) {
    this.enabled = enabled;
    this.apiKeyPublic = apiKeyPublic;
    this.apiKeyPrivate = apiKeyPrivate;
    this.fromEmail = fromEmail;
    this.fromName = fromName;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getApiKeyPublic() {
    return apiKeyPublic;
  }

  public String getApiKeyPrivate() {
    return apiKeyPrivate;
  }

  public String getFromEmail() {
    return fromEmail;
  }

  public String getFromName() {
    return fromName;
  }
}
