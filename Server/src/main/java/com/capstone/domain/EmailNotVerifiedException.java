package com.capstone.domain;

public class EmailNotVerifiedException extends RuntimeException {

  public EmailNotVerifiedException() {
    super("Email address must be verified before adding vehicles");
  }
}
