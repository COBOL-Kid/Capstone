package com.capstone.domain;

public class DuplicateEmailException extends ConflictException {

  public DuplicateEmailException() {
    super("Email is already registered");
  }
}
