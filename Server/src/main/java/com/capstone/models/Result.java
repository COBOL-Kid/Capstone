package com.capstone.models;

import java.util.Objects;
import java.util.ArrayList;

public class Result<T> {

    private ArrayList<String> errors;

    private T payload;

    public Result() {
    }

    public ArrayList<String> getErrors() {
        return errors;
    }

    public void setErrors(ArrayList<String> errors) {
        this.errors = errors;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }
}
