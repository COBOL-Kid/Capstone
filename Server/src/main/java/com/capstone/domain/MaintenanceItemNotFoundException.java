package com.capstone.domain;

public class MaintenanceItemNotFoundException extends ResourceNotFoundException {

  public MaintenanceItemNotFoundException() {
    super("Maintenance item not found");
  }
}
