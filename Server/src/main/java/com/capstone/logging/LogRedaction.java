package com.capstone.logging;

public final class LogRedaction {

  private LogRedaction() {}

  public static String maskVin(String vin) {
    if (vin == null || vin.length() <= 4) {
      return "****";
    }
    return "****" + vin.substring(vin.length() - 4);
  }

  public static String emailDomain(String email) {
    if (email == null || email.isBlank()) {
      return "unknown";
    }
    int at = email.lastIndexOf('@');
    if (at < 0 || at == email.length() - 1) {
      return "unknown";
    }
    return email.substring(at + 1);
  }
}
