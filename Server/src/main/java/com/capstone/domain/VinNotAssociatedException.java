package com.capstone.domain;

public class VinNotAssociatedException extends RuntimeException {

    public VinNotAssociatedException() {
        super("VIN is not associated with this user");
    }
}
