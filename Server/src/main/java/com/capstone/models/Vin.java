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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_info_id",
            nullable = false,
            referencedColumnName = "vehicleInfoId",
            foreignKey = @ForeignKey(name = "vehicle_info_id_fk"))
    private VehicleInfo vehicleInfo;

    public Vin() {
    }

    public Vin(Long ownerId, String vin, int mileage, VehicleInfo vehicleInfo) {
        this.ownerId = ownerId;
        this.vin = vin;
        this.mileage = mileage;
        this.vehicleInfo = vehicleInfo;
    }


    public boolean isInvalid() {
        return ownerId <= 0 || vehicleInfo == null || mileage <= 0 || this.vin == null || this.vin.isEmpty();
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

    public VehicleInfo getVehicleInfo() {
        return vehicleInfo;
    }

    public void setVehicleInfo(VehicleInfo vehicleInfo) {
        this.vehicleInfo = vehicleInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vin vin1 = (Vin) o;
        return getMileage() == vin1.getMileage() && Objects.equals(getOwnerId(), vin1.getOwnerId()) && Objects.equals(getVin(), vin1.getVin()) && Objects.equals(getVehicleInfo(), vin1.getVehicleInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerId(), getVin(), getMileage(), getVehicleInfo());
    }
}



