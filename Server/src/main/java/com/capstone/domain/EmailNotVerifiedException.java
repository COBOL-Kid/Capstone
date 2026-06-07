package com.capstone.domain;

public class EmailNotVerifiedException extends ForbiddenAccessException {

  public EmailNotVerifiedException() {
    super("Email address must be verified before adding vehicles");
  }
}
