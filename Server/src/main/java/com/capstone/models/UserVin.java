package com.capstone.models;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_vin")
public class UserVin {

    @EmbeddedId
    private UserVinId id = new UserVinId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("vin")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vin_num", referencedColumnName = "vin_num", nullable = false)
    private Vin vin;

    @Column(name = "current_mileage", nullable = false, columnDefinition = "int")
    private int currentMileage;

    public UserVin() {
    }

    public UserVin(User user, Vin vin) {
        setUser(user);
        setVin(vin);
    }

    public UserVin(User user, Vin vin, int currentMileage) {
        setUser(user);
        setVin(vin);
        this.currentMileage = currentMileage;
    }

    public UserVin(UserVinId id, User user, Vin vin) {
        this.id = id != null ? id : new UserVinId();
        setUser(user);
        setVin(vin);
    }

    public UserVinId getId() {
        return id;
    }

    public void setId(UserVinId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (this.id == null) {
            this.id = new UserVinId();
        }
        this.id.setUserId(user != null ? user.getUserId() : null);
    }

    public Vin getVin() {
        return vin;
    }

    public void setVin(Vin vin) {
        this.vin = vin;
        if (this.id == null) {
            this.id = new UserVinId();
        }
        this.id.setVin(vin != null ? vin.getVin() : null);
    }

    public int getCurrentMileage() {
        return currentMileage;
    }

    public void setCurrentMileage(int currentMileage) {
        this.currentMileage = currentMileage;
    }
}