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

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinTable(name = "user_vin", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "VIN_NUM"))
    private Set<Vin> userVins = new HashSet<>();

}