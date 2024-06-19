package com.capstone.models;

import jakarta.persistence.*;

import java.util.Objects;

@Entity(name = "vehicle_info")
public class VehicleInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int vehicleInfoId;

    @Column(name = "year",
            nullable = false,
            columnDefinition = "integer")
    int year;

    @Column(name = "make",
            nullable = false,
            columnDefinition = "text")
    String make;

    @Column(name = "model",
            nullable = false,
            columnDefinition = "text")
    String model;

    @Column(name = "image",
            nullable = false,
            columnDefinition = "text")
    String image;

    public VehicleInfo() {
    }

    public VehicleInfo(int year, String make, String model, String image) {
        this.year = year;
        this.make = make;
        this.model = model;
        this.image = image;
    }

    public int getVehicleInfoId() {
        return vehicleInfoId;
    }

    public void setVehicleInfoId(int vehicleInfoId) {
        this.vehicleInfoId = vehicleInfoId;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleInfo that = (VehicleInfo) o;
        return vehicleInfoId == that.vehicleInfoId && year == that.year && Objects.equals(make, that.make) && Objects.equals(model, that.model) && Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleInfoId, year, make, model, image);
    }
}
