package com.capstone.email;

import java.util.Locale;

public final class EmailNormalizer {

  private EmailNormalizer() {}

  // Local-part is lowercased to match every major mail provider's case-insensitive treatment in
  // practice; this is more permissive than RFC 5321, which allows case-sensitive local-parts.
  public static String normalize(String email) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
