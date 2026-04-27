package com.capstone.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;

import java.util.HashSet;
import java.util.Set;

@Entity(name = "USER")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Email
    @Column(name = "user_email", nullable = false, columnDefinition = "varchar(50)")
    private String userEmail;

    @Column(name = "user_sms", columnDefinition = "varchar(12)")
    private String userSms;

    @Column(name = "user_pw", nullable = false, columnDefinition = "varchar(20)")
    private String userPw;

    @OneToMany(mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = true)
    private Set<UserVin> userVins = new HashSet<>();

    public User() {
    }

    public User(String userEmail, String userSms, String userPw) {
        this.userEmail = userEmail;
        this.userSms = userSms;
        this.userPw = userPw;
    }

    public User(Long userId, String userEmail, String userSms, String userPw, Set<UserVin> userVins) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.userSms = userSms;
        this.userPw = userPw;
        this.userVins = userVins != null ? userVins : new HashSet<>();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserSms() {
        return userSms;
    }

    public void setUserSms(String userSms) {
        this.userSms = userSms;
    }

    public String getUserPw() {
        return userPw;
    }

    public void setUserPw(String userPw) {
        this.userPw = userPw;
    }

    public Set<UserVin> getUserVins() {
        return userVins;
    }

    public void setUserVins(Set<UserVin> userVins) {
        this.userVins = userVins != null ? userVins : new HashSet<>();
    }

}