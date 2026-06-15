package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.capstone.data.EmailVerificationCodeRepositoryJPA;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.email.EmailNormalizer;
import com.capstone.models.User;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    properties = {
      "spring.profiles.active=test",
      "spring.datasource.url=jdbc:h2:mem:registration-integration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.flyway.enabled=false",
      "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
      "security.jwt.expiration-minutes=15",
      "security.jwt.refresh-expiration-days=7",
      "mailjet.enabled=true",
      "mailjet.api-key-public=test-public",
      "mailjet.api-key-private=test-private",
      "mailjet.from-email=sender@example.com",
      "mailjet.from-name=Honest Car"
    })
class RegistrationIntegrationTest {

  @MockitoBean private MailjetClient mailjetClient;

  @Autowired private AuthenticationService authenticationService;
  @Autowired private UserRepositoryJPA userRepository;
  @Autowired private EmailVerificationCodeRepositoryJPA verificationCodeRepository;

  @BeforeEach
  void stubMailjetSuccess() throws MailjetException {
    MailjetResponse response = org.mockito.Mockito.mock(MailjetResponse.class);
    when(response.getStatus()).thenReturn(HttpStatus.OK.value());
    when(response.getData()).thenReturn(successResponseData());
    when(mailjetClient.post(any())).thenReturn(response);
  }

  @Test
  void registerUsesRealRepositoriesForDuplicateCheckAndVerificationCode() {
    String email = "register-" + System.nanoTime() + "@example.com";
    String normalizedEmail = EmailNormalizer.normalize(email);

    AuthenticationResponse response =
        authenticationService.register(new RegisterRequest(" Pat ", " Driver ", email, "secret"));

    assertEquals(Boolean.FALSE, response.getEmailVerified());
    assertNotNull(response.getToken());
    assertNotNull(response.getRefreshToken());

    User savedUser = userRepository.findByUserEmail(normalizedEmail).orElseThrow();
    assertEquals("Pat", savedUser.getFirstName());
    assertEquals("Driver", savedUser.getLastName());
    assertFalse(savedUser.isEmailVerified());
    assertFalse(userRepository.existsByUserEmail("missing-" + System.nanoTime() + "@example.com"));
    assertTrue(verificationCodeRepository.findTopByUserOrderByCreatedAtDesc(savedUser).isPresent());
  }

  private static JSONArray successResponseData() {
    return new JSONArray()
        .put(
            new JSONObject()
                .put("Status", "success")
                .put(
                    "To",
                    new JSONArray()
                        .put(
                            new JSONObject()
                                .put("Email", "register@example.com")
                                .put("MessageUUID", "123")
                                .put("MessageID", 456)
                                .put("MessageHref", "https://api.mailjet.com/v3/message/456"))));
  }
}
