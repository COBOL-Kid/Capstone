package com.capstone;

import com.capstone.configuration.AppProperties;
import com.capstone.configuration.EmailVerificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AppProperties.class, EmailVerificationProperties.class})
public class App {
  static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}
