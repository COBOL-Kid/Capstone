package com.capstone.configuration;

import com.capstone.authentication.EmailVerifiedFilter;
import com.capstone.authentication.JwtAuthenticationFilter;
import com.capstone.authentication.LoginRateLimitFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final RequestLoggingFilter requestLoggingFilter;
  private final LoginRateLimitFilter loginRateLimitFilter;
  private final JwtAuthenticationFilter jwtAuthFilter;
  private final EmailVerifiedFilter emailVerifiedFilter;
  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;

  @Value("${security.cors.allowed-origins}")
  private List<String> allowedOrigins;

  public SecurityConfig(
      RequestLoggingFilter requestLoggingFilter,
      LoginRateLimitFilter loginRateLimitFilter,
      JwtAuthenticationFilter jwtAuthFilter,
      EmailVerifiedFilter emailVerifiedFilter,
      UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder) {
    this.requestLoggingFilter = requestLoggingFilter;
    this.loginRateLimitFilter = loginRateLimitFilter;
    this.jwtAuthFilter = jwtAuthFilter;
    this.emailVerifiedFilter = emailVerifiedFilter;
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    DaoAuthenticationProvider authenticationProvider =
        new DaoAuthenticationProvider(userDetailsService);
    authenticationProvider.setPasswordEncoder(passwordEncoder);

    http.csrf(csrf -> csrf.spa())
        .headers(
            headers ->
                headers
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .httpStrictTransportSecurity(
                        hsts -> hsts.maxAgeInSeconds(31_536_000).includeSubDomains(true)))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            authorizeRequests ->
                authorizeRequests
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/email-verification/resend",
                        "/api/auth/email-verification/verify")
                    .hasAuthority("USER")
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/api/external/**")
                    .hasAuthority("ADMIN")
                    .requestMatchers("/api/account/**")
                    .hasAuthority("USER")
                    .requestMatchers("/api/maintenance/**")
                    .hasAuthority("USER")
                    .requestMatchers("/api/recall/**")
                    .hasAuthority("USER")
                    .requestMatchers("/api/vin/**")
                    .hasAuthority("USER")
                    .requestMatchers("/api/reminder/**")
                    .hasAuthority("USER")
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasAuthority("ADMIN")
                    .anyRequest()
                    .authenticated())
        .sessionManagement(
            sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(requestLoggingFilter, LoginRateLimitFilter.class)
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(emailVerifiedFilter, UsernamePasswordAuthenticationFilter.class)
        .authenticationProvider(authenticationProvider);
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "Accept", "X-XSRF-TOKEN"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
