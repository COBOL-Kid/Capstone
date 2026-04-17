package com.capstone.models;

public class VinResponse {

    private VehicleInfo data;

    public VinResponse(VehicleInfo data) {
        this.data = data;
    }

    public VinResponse() {
    }

    public VehicleInfo getData() {
        return data;
    }

    public void setData(VehicleInfo data) {
        this.data = data;
    }
}
