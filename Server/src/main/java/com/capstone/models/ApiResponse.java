package com.capstone.models;

import java.util.List;

public class ApiResponse {

    private List<Maintenance> data;

    public ApiResponse() {
    }

    public ApiResponse(List<Maintenance> data) {
        this.data = data;
    }

    public List<Maintenance> getData() {
        return data;
    }

    public void setData(List<Maintenance> data) {
        this.data = data;
    }
}
