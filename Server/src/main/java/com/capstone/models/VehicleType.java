package com.capstone.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity(name = "vehicle_type")
@Table(name = "vehicle_type", uniqueConstraints = @UniqueConstraint(columnNames = {"vehicle_year", "vehicle_make",
		"vehicle_model", "vehicle_trim", "vehicle_style"}))
public class VehicleType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "vehicle_type_id")
	private Long vehicleTypeId;

	@OneToMany(mappedBy = "vehicleTypeId")
	private Set<Vin> vins = new HashSet<>();

	@OneToMany(mappedBy = "vehicleTypeId")
	private Set<MaintMileage> maintItems;

	@OneToMany(mappedBy = "vehicleTypeId")
	private Set<MaintCost> maintCosts;

	@OneToMany(mappedBy = "vehicleTypeId")
	private Set<Recall> recalls = new HashSet<>();

	@Column(name = "vehicle_make", nullable = false, columnDefinition = "varchar(60)")
	private String vehicleMake;

	@Column(name = "vehicle_model", nullable = false, columnDefinition = "varchar(80)")
	private String vehicleModel;

	@Column(name = "vehicle_trim", nullable = false, columnDefinition = "varchar(120)")
	private String vehicleTrim;

	@Column(name = "vehicle_year", nullable = false, columnDefinition = "char(4)")
	private String vehicleYear;

	@Column(name = "vehicle_style", nullable = false, columnDefinition = "varchar(160)")
	private String vehicleStyle = "UNKNOWN";

	@Column(name = "source_vin", columnDefinition = "char(17)")
	private String sourceVin;

	@Column(name = "origin", columnDefinition = "varchar(60)")
	private String origin;

	@Column(name = "body", columnDefinition = "varchar(80)")
	private String body;

	@Column(name = "engine_description", columnDefinition = "varchar(120)")
	private String engineDescription;

	@Column(name = "transmission_style", columnDefinition = "varchar(50)")
	private String transmissionStyle;

	@Column(name = "drive_type", columnDefinition = "varchar(80)")
	private String driveType;

	@Column(name = "owners_manual", columnDefinition = "varchar(500)")
	private String ownersManual;

	public VehicleType() {
	}

	public VehicleType(String vehicleMake, String vehicleModel, String vehicleTrim, String vehicleYear) {
		this.vehicleMake = vehicleMake;
		this.vehicleModel = vehicleModel;
		this.vehicleTrim = vehicleTrim;
		this.vehicleYear = vehicleYear;
	}

	public VehicleType(Long vehicleTypeId, Set<Vin> vins, Set<MaintMileage> maintItems, Set<MaintCost> maintCosts,
			Set<Recall> recalls, String vehicleMake, String vehicleModel, String vehicleTrim, String vehicleYear,
			String ownersManual) {
		this.vehicleTypeId = vehicleTypeId;
		this.vins = vins != null ? vins : new HashSet<>();
		this.maintItems = maintItems != null ? maintItems : new HashSet<>();
		this.maintCosts = maintCosts != null ? maintCosts : new HashSet<>();
		this.recalls = recalls != null ? recalls : new HashSet<>();
		this.vehicleMake = vehicleMake;
		this.vehicleModel = vehicleModel;
		this.vehicleTrim = vehicleTrim;
		this.vehicleYear = vehicleYear;
		this.ownersManual = ownersManual;
	}

	public VehicleType(Long vehicleTypeId, Set<Vin> vins, Set<MaintMileage> maintItems, Set<MaintCost> maintCosts,
			Set<Recall> recalls, String vehicleMake, String vehicleModel, String vehicleTrim, String vehicleYear,
			String ownersManual, String sourceVin, String origin, String vehicleStyle, String body,
			String engineDescription, String transmissionStyle, String driveType) {
		this(vehicleTypeId, vins, maintItems, maintCosts, recalls, vehicleMake, vehicleModel, vehicleTrim, vehicleYear,
				ownersManual);
		this.sourceVin = sourceVin;
		this.origin = origin;
		this.vehicleStyle = vehicleStyle;
		this.body = body;
		this.engineDescription = engineDescription;
		this.transmissionStyle = transmissionStyle;
		this.driveType = driveType;
	}

	public Long getVehicleTypeId() {
		return vehicleTypeId;
	}

	public void setVehicleTypeId(Long vehicleTypeId) {
		this.vehicleTypeId = vehicleTypeId;
	}

	public Set<Vin> getVins() {
		return vins;
	}

	public void setVins(Set<Vin> vins) {
		this.vins = vins != null ? vins : new HashSet<>();
	}

	public Set<MaintMileage> getMaintItems() {
		return maintItems;
	}

	public void setMaintItems(Set<MaintMileage> maintItems) {
		this.maintItems = maintItems != null ? maintItems : new HashSet<>();
	}

	public Set<MaintCost> getMaintCosts() {
		return maintCosts;
	}

	public void setMaintCosts(Set<MaintCost> maintCosts) {
		this.maintCosts = maintCosts != null ? maintCosts : new HashSet<>();
	}

	public Set<Recall> getRecalls() {
		return recalls;
	}

	public void setRecalls(Set<Recall> recalls) {
		this.recalls = recalls != null ? recalls : new HashSet<>();
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

	public String getVehicleTrim() {
		return vehicleTrim;
	}

	public void setVehicleTrim(String vehicleTrim) {
		this.vehicleTrim = vehicleTrim;
	}

	public String getVehicleYear() {
		return vehicleYear;
	}

	public void setVehicleYear(String vehicleYear) {
		this.vehicleYear = vehicleYear;
	}

	public String getVehicleStyle() {
		return vehicleStyle;
	}

	public void setVehicleStyle(String vehicleStyle) {
		this.vehicleStyle = vehicleStyle;
	}

	public String getSourceVin() {
		return sourceVin;
	}

	public void setSourceVin(String sourceVin) {
		this.sourceVin = sourceVin;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getEngineDescription() {
		return engineDescription;
	}

	public void setEngineDescription(String engineDescription) {
		this.engineDescription = engineDescription;
	}

	public String getTransmissionStyle() {
		return transmissionStyle;
	}

	public void setTransmissionStyle(String transmissionStyle) {
		this.transmissionStyle = transmissionStyle;
	}

	public String getDriveType() {
		return driveType;
	}

	public void setDriveType(String driveType) {
		this.driveType = driveType;
	}

	public String getOwnersManual() {
		return ownersManual;
	}

	public void setOwnersManual(String ownersManual) {
		this.ownersManual = ownersManual;
	}
}
