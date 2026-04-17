package com.capstone.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "vin")
public class Vin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vinId;

    @Column(name = "owner_id", nullable = false, columnDefinition = "integer")
    private Long ownerId;

    @Column(name = "vehicle_info_id", nullable = false, insertable = false, updatable = false, columnDefinition = "integer")
    private Long vehicleInfoId;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehicle_info_id", nullable = false, columnDefinition = "integer")
    @JsonIgnore
    private VehicleInfo vehicleInfo;

    @Column(name = "vin", nullable = false, unique = true, columnDefinition = "text")
    private String vin;

    @Column(name = "mileage", nullable = false, columnDefinition = "integer")
    private int mileage;

    public Vin() {
        this.vehicleInfo = new VehicleInfo();
    }

    public Vin(Long ownerId, String vin, int mileage, int year, String make, String model, String image) {
        this.ownerId = ownerId;
        this.vin = vin;
        this.mileage = mileage;
        this.vehicleInfo = new VehicleInfo(year, make, model, image);
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

    public Long getVehicleInfoId() {
        return vehicleInfoId;
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
        return vehicleInfo == null ? 0 : vehicleInfo.getYear();
    }

    public void setYear(int year) {
        ensureVehicleInfo().setYear(year);
    }

    public String getMake() {
        return vehicleInfo == null ? null : vehicleInfo.getMake();
    }

    public void setMake(String make) {
        ensureVehicleInfo().setMake(make);
    }

    public String getModel() {
        return vehicleInfo == null ? null : vehicleInfo.getModel();
    }

    public void setModel(String model) {
        ensureVehicleInfo().setModel(model);
    }

    public String getImage() {
        return vehicleInfo == null ? null : vehicleInfo.getImage();
    }

    public void setImage(String image) {
        ensureVehicleInfo().setImage(image);
    }

    @JsonIgnore
    public VehicleInfo getVehicleInfo() {
        return vehicleInfo;
    }

    public void setVehicleInfo(VehicleInfo vehicleInfo) {
        this.vehicleInfo = vehicleInfo;
    }

    public boolean isInvalid() {
        return ownerId == null || ownerId <= 0 || mileage <= 0 || vin == null || vin.isEmpty() || getYear() <= 0
                || getModel() == null || getModel().isEmpty() || getImage() == null || getImage().isEmpty()
                || getMake() == null || getMake().isEmpty();
    }

    private VehicleInfo ensureVehicleInfo() {
        if (vehicleInfo == null) {
            vehicleInfo = new VehicleInfo();
        }
        return vehicleInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Vin vin1 = (Vin) o;
        return getMileage() == vin1.getMileage() && getYear() == vin1.getYear()
                && Objects.equals(getOwnerId(), vin1.getOwnerId()) && Objects.equals(getVin(), vin1.getVin())
                && Objects.equals(getMake(), vin1.getMake()) && Objects.equals(getModel(), vin1.getModel())
                && Objects.equals(getImage(), vin1.getImage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerId(), getVin(), getMileage(), getYear(), getMake(), getModel(), getImage());
    }
}
