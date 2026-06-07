package com.capstone.domain;

public class VinNotFoundException extends ResourceNotFoundException {

  public VinNotFoundException() {
    super("Vehicle data is not available for this VIN");
  }
}
