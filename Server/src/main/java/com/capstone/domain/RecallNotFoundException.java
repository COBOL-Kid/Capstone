package com.capstone.domain;

public class RecallNotFoundException extends RuntimeException {

    public RecallNotFoundException() {
        super("Recall not found");
    }
}
