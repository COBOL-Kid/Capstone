package com.capstone.email;

public abstract class EmailVerificationCodeException extends RuntimeException {

  protected EmailVerificationCodeException(String message) {
    super(message);
  }
}
