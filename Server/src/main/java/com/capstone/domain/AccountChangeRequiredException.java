package com.capstone.domain;

public class AccountChangeRequiredException extends RuntimeException {

  public AccountChangeRequiredException() {
    super("Use account change verification to update your password");
  }
}
