package com.capstone.domain;

public class InvalidAccountCredentialsException extends RuntimeException {

    public InvalidAccountCredentialsException() {
        super("Invalid account credentials");
    }
}
