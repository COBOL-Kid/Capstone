package com.capstone.domain;

public class AccountChangeRequiredException extends ConflictException {

  public AccountChangeRequiredException() {
    super("Use account change verification to update your password");
  }
}
