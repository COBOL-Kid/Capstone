package com.capstone.models;

import java.util.List;

public class MaintenanceResponse {

    private List<Maintenance> data;

    public MaintenanceResponse() {
    }

    public MaintenanceResponse(List<Maintenance> data) {
        this.data = data;
    }

    public List<Maintenance> getData() {
        return data;
    }

    public void setData(List<Maintenance> data) {
        this.data = data;
    }
}
