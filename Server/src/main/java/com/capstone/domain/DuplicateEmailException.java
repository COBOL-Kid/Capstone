package com.capstone.domain;

public class DuplicateEmailException extends ConflictException {

  public DuplicateEmailException() {
    super("Unable to complete registration");
  }
}
