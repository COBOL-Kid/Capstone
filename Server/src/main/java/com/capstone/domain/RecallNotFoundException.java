package com.capstone.domain;

public class RecallNotFoundException extends ResourceNotFoundException {

  public RecallNotFoundException() {
    super("Recall not found");
  }
}
