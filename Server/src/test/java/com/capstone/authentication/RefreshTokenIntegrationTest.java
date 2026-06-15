package com.capstone.authentication;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.capstone.data.UserRepositoryJPA;
import com.capstone.models.User;
import com.capstone.support.IntegrationTestProperties;
import com.capstone.support.MailjetTestSupport;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.errors.MailjetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class RefreshTokenIntegrationTest {

  private static final long SEED_USER_ID = 1L;

  @DynamicPropertySource
  static void h2FlywaySeedProperties(DynamicPropertyRegistry registry) {
    for (String property : IntegrationTestProperties.h2FlywaySeed("refresh-token-integration")) {
      int separator = property.indexOf('=');
      registry.add(property.substring(0, separator), () -> property.substring(separator + 1));
    }
  }

  @MockitoBean private MailjetClient mailjetClient;

  @Autowired private AuthenticationService authenticationService;
  @Autowired private UserRepositoryJPA userRepository;

  @BeforeEach
  void stubMailjetSuccess() throws MailjetException {
    MailjetTestSupport.stubSuccessfulSend(mailjetClient);
  }

  @Test
  void refreshTokenRotatesAndRejectsReplay() {
    User user = userRepository.findById(SEED_USER_ID).orElseThrow();
    AuthenticationResponse session = authenticationService.createSession(user);
    String oldRefresh = session.getRefreshToken();

    AuthenticationResponse rotated = authenticationService.refreshToken(oldRefresh);
    String newRefresh = rotated.getRefreshToken();

    assertNotEquals(oldRefresh, newRefresh);
    assertThrows(
        InvalidRefreshTokenException.class, () -> authenticationService.refreshToken(oldRefresh));
  }
}
