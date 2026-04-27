package com.capstone.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity(name = "vehicle_type")
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

    @Column(name = "vehicle_make", nullable = false, columnDefinition = "varchar(25)")
    private String vehicleMake;

    @Column(name = "vehicle_model", nullable = false, columnDefinition = "varchar(20)")
    private String vehicleModel;

    @Column(name = "vehicle_trim", nullable = false, columnDefinition = "varchar(30)")
    private String vehicleTrim;

    @Column(name = "vehicle_year", nullable = false, columnDefinition = "char(4)")
    private String vehicleYear;

    @Column(name = "owners_manual", columnDefinition = "varchar(100)")
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

    public String getOwnersManual() {
        return ownersManual;
    }

    public void setOwnersManual(String ownersManual) {
        this.ownersManual = ownersManual;
    }
}
