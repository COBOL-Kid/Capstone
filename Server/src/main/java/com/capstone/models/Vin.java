package com.capstone.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vin")
public class Vin {

  @Id
  @Column(name = "vin_num", nullable = false, columnDefinition = "char(17)")
  private String vin;

  @Column(name = "vin_mileage", nullable = false, columnDefinition = "integer")
  private int vinMileage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id", nullable = false)
  private VehicleType vehicleType;

  @JsonIgnore
  @OneToMany(mappedBy = "vin")
  private Set<UserVin> userVins = new HashSet<>();

  public Vin() {}

  public Vin(String vin, int vinMileage, VehicleType vehicleType) {
    this.vin = vin;
    this.vinMileage = vinMileage;
    this.vehicleType = vehicleType;
  }

  public Vin(String vin, int vinMileage, VehicleType vehicleType, Set<UserVin> userVins) {
    this.vin = vin;
    this.vinMileage = vinMileage;
    this.vehicleType = vehicleType;
    this.userVins = userVins != null ? userVins : new HashSet<>();
  }

  public String getVin() {
    return vin;
  }

  public void setVin(String vin) {
    this.vin = vin;
  }

  public int getVinMileage() {
    return vinMileage;
  }

  public void setVinMileage(int vinMileage) {
    this.vinMileage = vinMileage;
  }

  public VehicleType getVehicleType() {
    return vehicleType;
  }

  public void setVehicleType(VehicleType vehicleType) {
    this.vehicleType = vehicleType;
  }

  public Set<UserVin> getUserVins() {
    return userVins;
  }

  public void setUserVins(Set<UserVin> userVins) {
    this.userVins = userVins != null ? userVins : new HashSet<>();
  }
}
