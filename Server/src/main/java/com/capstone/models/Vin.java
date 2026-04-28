package com.capstone.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
	private VehicleType vehicleTypeId;

	@OneToMany(mappedBy = "vin")
	private Set<UserVin> userVins = new HashSet<>();

	public Vin() {
	}

	public Vin(String vin, int vinMileage, VehicleType vehicleTypeId) {
		this.vin = vin;
		this.vinMileage = vinMileage;
		this.vehicleTypeId = vehicleTypeId;
	}

	public Vin(String vin, int vinMileage, VehicleType vehicleTypeId, Set<UserVin> userVins) {
		this.vin = vin;
		this.vinMileage = vinMileage;
		this.vehicleTypeId = vehicleTypeId;
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

	public VehicleType getVehicleTypeId() {
		return vehicleTypeId;
	}

	public void setVehicleTypeId(VehicleType vehicleTypeId) {
		this.vehicleTypeId = vehicleTypeId;
	}

	public Set<UserVin> getUserVins() {
		return userVins;
	}

	public void setUserVins(Set<UserVin> userVins) {
		this.userVins = userVins != null ? userVins : new HashSet<>();
	}

}
