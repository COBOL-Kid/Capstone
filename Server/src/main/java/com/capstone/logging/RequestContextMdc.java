package com.capstone.logging;

import org.slf4j.MDC;

public final class RequestContextMdc {

  public static final String REQUEST_ID = "requestId";
  public static final String GCP_TRACE = "logging.googleapis.com/trace";

  private RequestContextMdc() {}

  public static String requestId() {
    return MDC.get(REQUEST_ID);
  }

  public static void putRequestId(String requestId) {
    MDC.put(REQUEST_ID, requestId);
  }

  public static void putGcpTrace(String gcpTrace) {
    if (gcpTrace != null && !gcpTrace.isBlank()) {
      MDC.put(GCP_TRACE, gcpTrace);
    }
  }

  public static void clear() {
    MDC.remove(REQUEST_ID);
    MDC.remove(GCP_TRACE);
  }
}
