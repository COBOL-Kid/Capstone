package com.capstone.domain;

public class VinNotFoundException extends RuntimeException {

  public VinNotFoundException() {
    super("Vehicle not found for VIN");
  }
}
