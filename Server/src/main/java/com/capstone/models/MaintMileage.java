package com.capstone.models;

import jakarta.persistence.*;

@Entity(name = "MAINT_MILEAGE")
@Table(name = "maint_mileage", uniqueConstraints = @UniqueConstraint(columnNames = {"vehicle_type_id", "mileage_due",
        "maint_desc"}))
public class MaintMileage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maint_mileage_id")
    private Long maintMileageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleTypeId;

    @Column(name = "mileage_due", nullable = false, columnDefinition = "int")
    private int mileageDue;

    @Column(name = "maint_desc", nullable = false, columnDefinition = "varchar(255)")
    private String maintDesc;

    public MaintMileage() {
    }

    public MaintMileage(VehicleType vehicleTypeId, int mileageDue, String maintDesc) {
        this.vehicleTypeId = vehicleTypeId;
        this.mileageDue = mileageDue;
        this.maintDesc = maintDesc;
    }

    public MaintMileage(Long maintMileageId, VehicleType vehicleTypeId, int mileageDue, String maintDesc) {
        this.maintMileageId = maintMileageId;
        this.vehicleTypeId = vehicleTypeId;
        this.mileageDue = mileageDue;
        this.maintDesc = maintDesc;
    }

    public Long getMaintMileageId() {
        return maintMileageId;
    }

    public void setMaintMileageId(Long maintMileageId) {
        this.maintMileageId = maintMileageId;
    }

    public VehicleType getVehicleTypeId() {
        return vehicleTypeId;
    }

    public void setVehicleTypeId(VehicleType vehicleTypeId) {
        this.vehicleTypeId = vehicleTypeId;
    }

    public int getMileageDue() {
        return mileageDue;
    }

    public void setMileageDue(int mileageDue) {
        this.mileageDue = mileageDue;
    }

    public String getMaintDesc() {
        return maintDesc;
    }

    public void setMaintDesc(String maintDesc) {
        this.maintDesc = maintDesc;
    }
}
