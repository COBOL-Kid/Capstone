package com.capstone.domain;

public class PendingAccountChangeNotFoundException extends RuntimeException {

  public PendingAccountChangeNotFoundException() {
    super("No pending account change request");
  }
}
