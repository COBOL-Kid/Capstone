package com.capstone.models;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "vin")
public class Vin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vinId;

    @Column(name = "owner_id",
            nullable = false,
            columnDefinition = "integer")
    private Long ownerId;

    @Column(name = "vin",
            nullable = false,
            unique = true,
            columnDefinition = "text")
    private String vin;

    @Column(name = "mileage",
            nullable = false,
            columnDefinition = "integer")
    private int mileage;

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

    public Vin() {
    }

    public Vin(Long ownerId, String vin, int mileage, int year, String make, String model, String image) {
        this.ownerId = ownerId;
        this.vin = vin;
        this.mileage = mileage;
        this.year = year;
        this.make = make;
        this.model = model;
        this.image = image;
    }

    public Long getVinId() {
        return vinId;
    }

    public void setVinId(Long vinId) {
        this.vinId = vinId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
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

    public boolean isInvalid() {
        return year == 0 || mileage == 0 || model == null || model.isEmpty() || image == null || image.isEmpty() || vin == null || vin.isEmpty() || make == null || make.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vin vin1 = (Vin) o;
        return getMileage() == vin1.getMileage() && getYear() == vin1.getYear() && Objects.equals(getOwnerId(), vin1.getOwnerId()) && Objects.equals(getVin(), vin1.getVin()) && Objects.equals(getMake(), vin1.getMake()) && Objects.equals(getModel(), vin1.getModel()) && Objects.equals(getImage(), vin1.getImage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerId(), getVin(), getMileage(), getYear(), getMake(), getModel(), getImage());
    }
}



