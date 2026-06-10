package com.capstone.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class VehicleWarrantyId implements Serializable {

  @Column(name = "vehicle_year", length = 4)
  private String vehicleYear;

  @Column(name = "vehicle_make", columnDefinition = "varchar(60)")
  private String vehicleMake;

  @Column(name = "vehicle_model", columnDefinition = "varchar(80)")
  private String vehicleModel;

  public VehicleWarrantyId() {}

  public VehicleWarrantyId(String vehicleYear, String vehicleMake, String vehicleModel) {
    this.vehicleYear = vehicleYear;
    this.vehicleMake = vehicleMake;
    this.vehicleModel = vehicleModel;
  }

  public String getVehicleYear() {
    return vehicleYear;
  }

  public void setVehicleYear(String vehicleYear) {
    this.vehicleYear = vehicleYear;
  }

  public String getVehicleMake() {
    return vehicleMake;
  }

  public void setVehicleMake(String vehicleMake) {
    this.vehicleMake = vehicleMake;
  }

  public String getVehicleModel() {
    return vehicleModel;
  }

  public void setVehicleModel(String vehicleModel) {
    this.vehicleModel = vehicleModel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VehicleWarrantyId that)) {
      return false;
    }
    return Objects.equals(vehicleYear, that.vehicleYear)
        && Objects.equals(vehicleMake, that.vehicleMake)
        && Objects.equals(vehicleModel, that.vehicleModel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vehicleYear, vehicleMake, vehicleModel);
  }
}
