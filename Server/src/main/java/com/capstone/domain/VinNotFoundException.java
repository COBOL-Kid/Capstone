package com.capstone.domain;

public class VinNotFoundException extends RuntimeException {

  public VinNotFoundException() {
    super("Vehicle data is not available for this VIN");
  }
}
