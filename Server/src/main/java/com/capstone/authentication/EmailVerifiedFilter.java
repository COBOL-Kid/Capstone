package com.capstone.authentication;

import com.capstone.models.Role;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class EmailVerifiedFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (user.role() != Role.USER || isAllowlisted(request) || user.emailVerified()) {
      filterChain.doFilter(request, response);
      return;
    }

    response.sendError(
        HttpServletResponse.SC_FORBIDDEN, "Email address must be verified before continuing");
  }

  private static boolean isAllowlisted(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.startsWith("/api/auth/")) {
      return true;
    }
    if (HttpMethod.GET.matches(request.getMethod()) && "/api/account/me".equals(path)) {
      return true;
    }
    if (HttpMethod.POST.matches(request.getMethod()) && "/api/account/delete".equals(path)) {
      return true;
    }
    return false;
  }
}
