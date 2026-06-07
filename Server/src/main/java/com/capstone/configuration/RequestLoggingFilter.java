package com.capstone.configuration;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.logging.RequestContextMdc;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS");
  private static final String TRACE_HEADER = "X-Cloud-Trace-Context";

  private final String gcpProjectId;

  public RequestLoggingFilter(@Value("${gcp.project-id:}") String gcpProjectId) {
    this.gcpProjectId = gcpProjectId;
  }

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString();
    RequestContextMdc.putRequestId(requestId);
    putGcpTrace(request);

    StatusCapturingResponseWrapper wrappedResponse = new StatusCapturingResponseWrapper(response);
    long startNanos = System.nanoTime();
    try {
      filterChain.doFilter(request, wrappedResponse);
    } finally {
      long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
      ACCESS_LOG.info(
          "http_access method={} path={} status={} durationMs={} userId={} clientIp={} requestId={}",
          request.getMethod(),
          request.getRequestURI(),
          wrappedResponse.getCapturedStatus(),
          durationMs,
          currentUserId(),
          request.getRemoteAddr(),
          requestId);
      RequestContextMdc.clear();
    }
  }

  private void putGcpTrace(HttpServletRequest request) {
    String traceHeader = request.getHeader(TRACE_HEADER);
    if (traceHeader == null || traceHeader.isBlank()) {
      return;
    }
    String traceId = parseTraceId(traceHeader);
    if (traceId == null || gcpProjectId == null || gcpProjectId.isBlank()) {
      return;
    }
    RequestContextMdc.putGcpTrace("projects/" + gcpProjectId + "/traces/" + traceId);
  }

  static String parseTraceId(String traceHeader) {
    if (traceHeader == null || traceHeader.isBlank()) {
      return null;
    }
    int slash = traceHeader.indexOf('/');
    String traceId = slash < 0 ? traceHeader : traceHeader.substring(0, slash);
    int semicolon = traceId.indexOf(';');
    if (semicolon >= 0) {
      traceId = traceId.substring(0, semicolon);
    }
    return traceId.isBlank() ? null : traceId;
  }

  private static Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
      return null;
    }
    return user.userId();
  }

  private static final class StatusCapturingResponseWrapper extends HttpServletResponseWrapper {

    private int status = HttpServletResponse.SC_OK;

    private StatusCapturingResponseWrapper(HttpServletResponse response) {
      super(response);
    }

    @Override
    public void setStatus(int sc) {
      status = sc;
      super.setStatus(sc);
    }

    @Override
    public void sendError(int sc) throws IOException {
      status = sc;
      super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
      status = sc;
      super.sendError(sc, msg);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
      status = HttpServletResponse.SC_FOUND;
      super.sendRedirect(location);
    }

    private int getCapturedStatus() {
      return status;
    }
  }
}
