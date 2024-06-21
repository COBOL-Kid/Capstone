package com.capstone.models;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(name = "vin")
public class Vin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long vinId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "vehicle_info_id", nullable = false)
    private VehicleInfo vehicleInfo;

    @Column(name = "owner_id",
            nullable = false,
            columnDefinition = "integer")
    private long ownerId;

    @Column(name = "vehicle_info_id",
            nullable = false,
            columnDefinition = "integer")
    private long vehicleInfoId;

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

    public Vin(long ownerId, long vehicleInfoId, String vin, int mileage) {
        this.ownerId = ownerId;
        this.vehicleInfoId = vehicleInfoId;
        this.vin = vin;
        this.mileage = mileage;
    }

    public long getVinId() {
        return vinId;
    }

    public void setVinId(long vinId) {
        this.vinId = vinId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public long getVehicleInfoId() {
        return vehicleInfoId;
    }

    public void setVehicleInfoId(long vehicleInfoId) {
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
        return getOwnerId() == vin1.getOwnerId() && getVehicleInfoId() == vin1.getVehicleInfoId() && getMileage() == vin1.getMileage() && Objects.equals(getVin(), vin1.getVin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerId(), getVehicleInfoId(), getVin(), getMileage());
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public VehicleInfo getVehicleInfo() {
        return vehicleInfo;
    }

    public void setVehicleInfo(VehicleInfo vehicleInfo) {
        this.vehicleInfo = vehicleInfo;
    }
}
