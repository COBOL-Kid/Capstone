package com.capstone.domain;

public class VinNotAssociatedException extends ForbiddenAccessException {

  public VinNotAssociatedException() {
    super("VIN is not associated with this user");
  }
}
