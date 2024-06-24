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

    @Column(name = "vehicle_info_id",
            nullable = false,
            columnDefinition = "integer")
    private Long vehicleInfoId;

    @Column(name = "vin",
            nullable = false,
            unique = true,
            columnDefinition = "text")
    private String vin;

    @Column(name = "mileage",
            nullable = false,
            columnDefinition = "integer")
    private int mileage;


    public Vin() {
    }

    public Vin(Long ownerId, Long vehicleInfoId, String vin, int mileage) {
        this.ownerId = ownerId;
        this.vehicleInfoId = vehicleInfoId;
        this.vin = vin;
        this.mileage = mileage;
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

    public void setVehicleInfoId(Long vehicleInfoId) {
        this.vehicleInfoId = vehicleInfoId;
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

    public boolean isInvalid() {
        return ownerId <= 0 || vehicleInfoId <= 0 || mileage <= 0 || this.vin == null || this.vin.isEmpty();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vin vin1 = (Vin) o;
        return Objects.equals(getOwnerId(), vin1.getOwnerId()) && Objects.equals(getVehicleInfoId(), vin1.getVehicleInfoId()) && getMileage() == vin1.getMileage() && Objects.equals(getVin(), vin1.getVin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerId(), getVehicleInfoId(), getVin(), getMileage());
    }
}
