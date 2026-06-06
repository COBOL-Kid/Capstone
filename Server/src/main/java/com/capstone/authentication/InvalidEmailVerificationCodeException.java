package com.capstone.authentication;

public class InvalidEmailVerificationCodeException extends RuntimeException {

  public InvalidEmailVerificationCodeException() {
    super("Invalid verification code");
  }
}
