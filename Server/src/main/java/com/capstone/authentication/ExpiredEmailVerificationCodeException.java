package com.capstone.authentication;

public class ExpiredEmailVerificationCodeException extends RuntimeException {

  public ExpiredEmailVerificationCodeException() {
    super("Verification code has expired");
  }
}
