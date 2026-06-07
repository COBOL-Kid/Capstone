package com.capstone.authentication;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final String AUTHENTICATE_PATH = "/api/auth/authenticate";

  private final int maxAttempts;
  private final Duration window;
  private final Map<String, AttemptWindow> attemptsByIp = new ConcurrentHashMap<>();

  public LoginRateLimitFilter(
      @Value("${security.login.max-attempts-per-ip:5}") int maxAttempts,
      @Value("${security.login.rate-limit-window-minutes:15}") int windowMinutes) {
    this.maxAttempts = maxAttempts;
    this.window = Duration.ofMinutes(windowMinutes);
  }

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (!isAuthenticateRequest(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = request.getRemoteAddr();
    AttemptWindow windowState =
        attemptsByIp.computeIfAbsent(clientIp, ignored -> new AttemptWindow());
    synchronized (windowState) {
      windowState.resetIfExpired(window);
      if (windowState.attempts() >= maxAttempts) {
        response.sendError(429, "Too many login attempts. Please try again later.");
        return;
      }
      windowState.increment();
    }

    filterChain.doFilter(request, response);
  }

  private static boolean isAuthenticateRequest(HttpServletRequest request) {
    return HttpMethod.POST.matches(request.getMethod())
        && AUTHENTICATE_PATH.equals(request.getRequestURI());
  }

  private static final class AttemptWindow {
    private int attempts;
    private Instant windowStart = Instant.now();

    private void resetIfExpired(Duration window) {
      if (Instant.now().isAfter(windowStart.plus(window))) {
        attempts = 0;
        windowStart = Instant.now();
      }
    }

    private int attempts() {
      return attempts;
    }

    private void increment() {
      attempts++;
    }
  }
}
