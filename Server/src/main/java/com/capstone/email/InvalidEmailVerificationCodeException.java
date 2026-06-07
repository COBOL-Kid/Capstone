package com.capstone.email;

public class InvalidEmailVerificationCodeException extends EmailVerificationCodeException {

  public InvalidEmailVerificationCodeException() {
    super("Invalid verification code");
  }
}
