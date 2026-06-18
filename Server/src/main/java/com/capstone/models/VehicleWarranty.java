package com.capstone.models;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vehicle_warranty")
public class VehicleWarranty {

  @EmbeddedId private VehicleWarrantyId id = new VehicleWarrantyId();

  @Column(name = "fetched_at", nullable = false)
  private Instant fetchedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "coverages", nullable = false)
  private Map<String, String> coverages = new LinkedHashMap<>();

  public VehicleWarranty() {}

  public VehicleWarranty(String vehicleYear, String vehicleMake, String vehicleModel) {
    this.id = new VehicleWarrantyId(vehicleYear, vehicleMake, vehicleModel);
    this.fetchedAt = Instant.now();
  }

  public VehicleWarrantyId getId() {
    return id;
  }

  public void setId(VehicleWarrantyId id) {
    this.id = id != null ? id : new VehicleWarrantyId();
  }

  public String getVehicleYear() {
    return id.getVehicleYear();
  }

  public String getVehicleMake() {
    return id.getVehicleMake();
  }

  public String getVehicleModel() {
    return id.getVehicleModel();
  }

  public Instant getFetchedAt() {
    return fetchedAt;
  }

  public void setFetchedAt(Instant fetchedAt) {
    this.fetchedAt = fetchedAt;
  }

  public Map<String, String> getCoverages() {
    return coverages;
  }

  public void setCoverages(Map<String, String> coverages) {
    this.coverages = coverages != null ? coverages : new LinkedHashMap<>();
  }

  public void addCoverage(String coverageName, String coverageValue) {
    coverages.put(coverageName, coverageValue);
  }
}
