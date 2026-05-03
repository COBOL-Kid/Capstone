package com.capstone.domain;

public class MaintenanceItemNotFoundException extends RuntimeException {

    public MaintenanceItemNotFoundException() {
        super("Maintenance item not found");
    }
}
