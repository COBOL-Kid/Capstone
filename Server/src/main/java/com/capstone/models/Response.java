package com.capstone.models;

public class Response {

    private VehicleInfo data;


    public Response(VehicleInfo data) {
        this.data = data;
    }

    public Response() {
    }

    public VehicleInfo getData() {
        return data;
    }

    public void setData(VehicleInfo data) {
        this.data = data;
    }
}
