package com.capstone.domain;

public class PendingAccountChangeNotFoundException extends ResourceNotFoundException {

  public PendingAccountChangeNotFoundException() {
    super("No pending account change request");
  }
}
