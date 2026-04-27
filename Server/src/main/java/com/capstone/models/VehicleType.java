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
@Table(name = "vehicle_type", uniqueConstraints = @UniqueConstraint(columnNames = { "vehicle_year", "vehicle_make",
    "vehicle_model", "vehicle_trim" }))
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

    @Column(name = "doors", columnDefinition = "varchar(10)")
    private String doors;

    @Column(name = "vehicle_size", columnDefinition = "varchar(50)")
    private String vehicleSize;

    @Column(name = "seating_capacity", columnDefinition = "varchar(10)")
    private String seatingCapacity;

    @Column(name = "engine_cylinders", columnDefinition = "varchar(10)")
    private String engineCylinders;

    @Column(name = "engine_size", columnDefinition = "varchar(20)")
    private String engineSize;

    @Column(name = "engine_description", columnDefinition = "varchar(120)")
    private String engineDescription;

    @Column(name = "engine_capacity", columnDefinition = "varchar(20)")
    private String engineCapacity;

    @Column(name = "engine_configuration", columnDefinition = "varchar(50)")
    private String engineConfiguration;

    @Column(name = "electrification_level", columnDefinition = "varchar(50)")
    private String electrificationLevel;

    @Column(name = "manufacturer_name", columnDefinition = "varchar(120)")
    private String manufacturerName;

    @Column(name = "manufacturer_region", columnDefinition = "varchar(60)")
    private String manufacturerRegion;

    @Column(name = "manufacturer_country", columnDefinition = "varchar(60)")
    private String manufacturerCountry;

    @Column(name = "plant_city", columnDefinition = "varchar(80)")
    private String plantCity;

    @Column(name = "transmission_style", columnDefinition = "varchar(50)")
    private String transmissionStyle;

    @Column(name = "restraint_details", columnDefinition = "text")
    private String restraintDetails;

    @Column(name = "gvwr", columnDefinition = "varchar(120)")
    private String gvwr;

    @Column(name = "drive_type", columnDefinition = "varchar(80)")
    private String driveType;

    @Column(name = "fuel_type", columnDefinition = "varchar(50)")
    private String fuelType;

    @Column(name = "secondary_fuel_type", columnDefinition = "varchar(50)")
    private String secondaryFuelType;

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
            String ownersManual, String doors, String vehicleSize, String seatingCapacity, String engineCylinders,
            String engineSize, String engineDescription, String engineCapacity, String engineConfiguration,
            String electrificationLevel, String manufacturerName, String manufacturerRegion, String manufacturerCountry,
            String plantCity, String transmissionStyle, String restraintDetails, String gvwr, String driveType,
            String fuelType, String secondaryFuelType) {
        this(vehicleTypeId, vins, maintItems, maintCosts, recalls, vehicleMake, vehicleModel, vehicleTrim, vehicleYear,
                ownersManual);
        this.doors = doors;
        this.vehicleSize = vehicleSize;
        this.seatingCapacity = seatingCapacity;
        this.engineCylinders = engineCylinders;
        this.engineSize = engineSize;
        this.engineDescription = engineDescription;
        this.engineCapacity = engineCapacity;
        this.engineConfiguration = engineConfiguration;
        this.electrificationLevel = electrificationLevel;
        this.manufacturerName = manufacturerName;
        this.manufacturerRegion = manufacturerRegion;
        this.manufacturerCountry = manufacturerCountry;
        this.plantCity = plantCity;
        this.transmissionStyle = transmissionStyle;
        this.restraintDetails = restraintDetails;
        this.gvwr = gvwr;
        this.driveType = driveType;
        this.fuelType = fuelType;
        this.secondaryFuelType = secondaryFuelType;
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

    public String getDoors() {
        return doors;
    }

    public void setDoors(String doors) {
        this.doors = doors;
    }

    public String getVehicleSize() {
        return vehicleSize;
    }

    public void setVehicleSize(String vehicleSize) {
        this.vehicleSize = vehicleSize;
    }

    public String getSeatingCapacity() {
        return seatingCapacity;
    }

    public void setSeatingCapacity(String seatingCapacity) {
        this.seatingCapacity = seatingCapacity;
    }

    public String getEngineCylinders() {
        return engineCylinders;
    }

    public void setEngineCylinders(String engineCylinders) {
        this.engineCylinders = engineCylinders;
    }

    public String getEngineSize() {
        return engineSize;
    }

    public void setEngineSize(String engineSize) {
        this.engineSize = engineSize;
    }

    public String getEngineDescription() {
        return engineDescription;
    }

    public void setEngineDescription(String engineDescription) {
        this.engineDescription = engineDescription;
    }

    public String getEngineCapacity() {
        return engineCapacity;
    }

    public void setEngineCapacity(String engineCapacity) {
        this.engineCapacity = engineCapacity;
    }

    public String getEngineConfiguration() {
        return engineConfiguration;
    }

    public void setEngineConfiguration(String engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
    }

    public String getElectrificationLevel() {
        return electrificationLevel;
    }

    public void setElectrificationLevel(String electrificationLevel) {
        this.electrificationLevel = electrificationLevel;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public String getManufacturerRegion() {
        return manufacturerRegion;
    }

    public void setManufacturerRegion(String manufacturerRegion) {
        this.manufacturerRegion = manufacturerRegion;
    }

    public String getManufacturerCountry() {
        return manufacturerCountry;
    }

    public void setManufacturerCountry(String manufacturerCountry) {
        this.manufacturerCountry = manufacturerCountry;
    }

    public String getPlantCity() {
        return plantCity;
    }

    public void setPlantCity(String plantCity) {
        this.plantCity = plantCity;
    }

    public String getTransmissionStyle() {
        return transmissionStyle;
    }

    public void setTransmissionStyle(String transmissionStyle) {
        this.transmissionStyle = transmissionStyle;
    }

    public String getRestraintDetails() {
        return restraintDetails;
    }

    public void setRestraintDetails(String restraintDetails) {
        this.restraintDetails = restraintDetails;
    }

    public String getGvwr() {
        return gvwr;
    }

    public void setGvwr(String gvwr) {
        this.gvwr = gvwr;
    }

    public String getDriveType() {
        return driveType;
    }

    public void setDriveType(String driveType) {
        this.driveType = driveType;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getSecondaryFuelType() {
        return secondaryFuelType;
    }

    public void setSecondaryFuelType(String secondaryFuelType) {
        this.secondaryFuelType = secondaryFuelType;
    }

    public String getOwnersManual() {
        return ownersManual;
    }

    public void setOwnersManual(String ownersManual) {
        this.ownersManual = ownersManual;
    }
}
