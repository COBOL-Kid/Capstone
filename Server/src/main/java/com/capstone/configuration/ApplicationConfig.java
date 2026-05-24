package com.capstone.configuration;

import com.capstone.authentication.EmailNormalizer;
import com.capstone.data.UserRepositoryJPA;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

  private final UserRepositoryJPA repository;

  public ApplicationConfig(UserRepositoryJPA repository) {
    this.repository = repository;
  }

  @Bean
  public UserDetailsService userDetailsService() {
    return username -> {
      String normalized;
      try {
        normalized = EmailNormalizer.normalize(username);
      } catch (IllegalArgumentException ex) {
        throw new UsernameNotFoundException("User not found");
      }
      return repository
          .findByUserEmail(normalized)
          .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    };
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authenticationProvider =
        new DaoAuthenticationProvider(userDetailsService());
    authenticationProvider.setPasswordEncoder(passwordEncoder());
    return authenticationProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
