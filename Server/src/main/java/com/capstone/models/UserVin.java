package com.capstone.models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

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
    @JoinColumn(name = "vin_num", referencedColumnName = "vin_num", nullable = false, columnDefinition = "char(17)")
    private Vin vin;

    @Column(name = "current_mileage", nullable = false, columnDefinition = "int")
    private int currentMileage;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "available_image_urls", nullable = false, columnDefinition = "text")
    private List<String> availableImageUrls = new ArrayList<>();

    @Column(name = "selected_image_url", columnDefinition = "varchar(500)")
    private String selectedImageUrl;

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

    public List<String> getAvailableImageUrls() {
        return availableImageUrls;
    }

    public void setAvailableImageUrls(List<String> availableImageUrls) {
        this.availableImageUrls = availableImageUrls != null ? new ArrayList<>(availableImageUrls) : new ArrayList<>();
    }

    public String getSelectedImageUrl() {
        return selectedImageUrl;
    }

    public void setSelectedImageUrl(String selectedImageUrl) {
        this.selectedImageUrl = selectedImageUrl;
    }
}
