package com.capstone.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vin")
public class Vin {

  @Id
  @Column(name = "vin_num", nullable = false, length = 17)
  private String vin;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vehicle_type_id", nullable = false)
  private VehicleType vehicleType;

  @JsonIgnore
  @OneToMany(mappedBy = "vin")
  private Set<UserVin> userVins = new HashSet<>();

  public Vin() {}

  public Vin(String vin, VehicleType vehicleType) {
    this.vin = vin;
    this.vehicleType = vehicleType;
  }

  public Vin(String vin, VehicleType vehicleType, Set<UserVin> userVins) {
    this.vin = vin;
    this.vehicleType = vehicleType;
    this.userVins = userVins != null ? userVins : new HashSet<>();
  }

  public String getVin() {
    return vin;
  }

  public void setVin(String vin) {
    this.vin = vin;
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
