package com.capstone.email;

public class ExpiredEmailVerificationCodeException extends EmailVerificationCodeException {

  public ExpiredEmailVerificationCodeException() {
    super("Verification code has expired");
  }
}
