package com.capstone.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuditLog {

  private static final Logger LOG = LoggerFactory.getLogger("AUDIT");

  private AuditLog() {}

  public static void info(String event) {
    LOG.info("event={}", event);
  }

  public static void info(String event, String key, Object value) {
    LOG.info("event={} {}={}", event, key, value);
  }

  public static void info(String event, String key1, Object value1, String key2, Object value2) {
    LOG.info("event={} {}={} {}={}", event, key1, value1, key2, value2);
  }

  public static void info(
      String event,
      String key1,
      Object value1,
      String key2,
      Object value2,
      String key3,
      Object value3) {
    LOG.info("event={} {}={} {}={} {}={}", event, key1, value1, key2, value2, key3, value3);
  }

  public static void warn(String event) {
    LOG.warn("event={}", event);
  }

  public static void warn(String event, String key, Object value) {
    LOG.warn("event={} {}={}", event, key, value);
  }

  public static void warn(String event, String key1, Object value1, String key2, Object value2) {
    LOG.warn("event={} {}={} {}={}", event, key1, value1, key2, value2);
  }

  public static void warn(
      String event,
      String key1,
      Object value1,
      String key2,
      Object value2,
      String key3,
      Object value3) {
    LOG.warn("event={} {}={} {}={} {}={}", event, key1, value1, key2, value2, key3, value3);
  }
}
