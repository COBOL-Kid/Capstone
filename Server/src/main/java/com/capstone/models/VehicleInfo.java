package com.capstone.models;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "vehicle_info")
public class VehicleInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long vehicleInfoId;

    @Column(name = "year",
            nullable = false,
            columnDefinition = "integer")
    private int year;

    @Column(name = "make",
            nullable = false,
            columnDefinition = "text")
    private String make;

    @Column(name = "model",
            nullable = false,
            columnDefinition = "text")
    private String model;

    @Column(name = "image",
            nullable = false,
            columnDefinition = "text")
    private String image;

    public VehicleInfo() {
    }

    public VehicleInfo(int year, String make, String model, String image) {
        this.year = year;
        this.make = make;
        this.model = model;
        this.image = image;
    }

    public long getVehicleInfoId() {
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
        return getVehicleInfoId() == that.getVehicleInfoId() && getYear() == that.getYear() && Objects.equals(getMake(), that.getMake()) && Objects.equals(getModel(), that.getModel()) && Objects.equals(getImage(), that.getImage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVehicleInfoId(), getYear(), getMake(), getModel(), getImage());
    }
}
