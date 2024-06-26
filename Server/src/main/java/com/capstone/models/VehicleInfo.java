package com.capstone.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class VehicleInfo {


    private Long vehicleInfoId;


    private int year;


    private String make;


    private String model;


    private String image;


    private List<Vin> vins = new ArrayList<>();


    public VehicleInfo() {
    }

    public VehicleInfo(int year, String make, String model, String image) {
        this.year = year;
        this.make = make;
        this.model = model;
        this.image = image;
    }

    public Long getVehicleInfoId() {
        return vehicleInfoId;
    }

    public void setVehicleInfoId(Long vehicleInfoId) {
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

    public boolean isEmpty() {
        return (make == null||make.isEmpty()) && (model == null || model.isEmpty()) && (image == null || image.isEmpty()) && year == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleInfo that = (VehicleInfo) o;
        return Objects.equals(getVehicleInfoId(), that.getVehicleInfoId()) && getYear() == that.getYear() && Objects.equals(getMake(), that.getMake()) && Objects.equals(getModel(), that.getModel()) && Objects.equals(getImage(), that.getImage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getVehicleInfoId(), getYear(), getMake(), getModel(), getImage());
    }

    public List<Vin> getVins() {
        return vins;
    }

    public void addVin(Vin vin) {
        if (!vins.contains(vin)) {
            vins.add(vin);
        }
    }

    public void removeVin(Vin vin) {
        vins.remove(vin);
    }
}
