package com.capstone.domain;

public abstract class ConflictException extends RuntimeException {

  protected ConflictException(String message) {
    super(message);
  }
}
