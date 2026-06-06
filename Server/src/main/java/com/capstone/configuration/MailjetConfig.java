package com.capstone.configuration;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mailjet", name = "enabled", havingValue = "true")
public class MailjetConfig {

  @Bean
  public MailjetClient mailjetClient(MailjetProperties properties) {
    ClientOptions options =
        ClientOptions.builder()
            .apiKey(properties.getApiKeyPublic())
            .apiSecretKey(properties.getApiKeyPrivate())
            .build();
    return new MailjetClient(options);
  }
}
