package com.capstone.authentication;

import java.util.regex.Pattern;

public final class PasswordPolicy {

  public static final String COMPLEXITY_PATTERN =
      "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).+$";

  public static final String COMPLEXITY_MESSAGE =
      "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character";

  private static final Pattern COMPLEXITY = Pattern.compile(COMPLEXITY_PATTERN);

  private PasswordPolicy() {}

  public static void validate(String password) {
    if (password == null || password.isBlank()) {
      throw new IllegalArgumentException("Password is required");
    }
    if (password.length() < 8 || password.length() > 72) {
      throw new IllegalArgumentException("Password must be between 8 and 72 characters");
    }
    if (!COMPLEXITY.matcher(password).matches()) {
      throw new IllegalArgumentException(COMPLEXITY_MESSAGE);
    }
  }
}
