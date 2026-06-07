package com.capstone.domain;

public abstract class ForbiddenAccessException extends RuntimeException {

  protected ForbiddenAccessException(String message) {
    super(message);
  }
}
