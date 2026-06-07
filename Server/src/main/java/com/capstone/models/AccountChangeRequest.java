package com.capstone.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "account_change_request")
public class AccountChangeRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, unique = true)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false, length = 20)
  private AccountChangeType changeType;

  @Column(name = "new_email", length = 254)
  private String newEmail;

  @Column(name = "new_password_hash", length = 100)
  private String newPasswordHash;

  @Column(name = "new_user_sms", length = 20)
  private String newUserSms;

  @Column(name = "code_hash", nullable = false, length = 100)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public AccountChangeRequest() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public AccountChangeType getChangeType() {
    return changeType;
  }

  public void setChangeType(AccountChangeType changeType) {
    this.changeType = changeType;
  }

  public String getNewEmail() {
    return newEmail;
  }

  public void setNewEmail(String newEmail) {
    this.newEmail = newEmail;
  }

  public String getNewPasswordHash() {
    return newPasswordHash;
  }

  public void setNewPasswordHash(String newPasswordHash) {
    this.newPasswordHash = newPasswordHash;
  }

  public String getNewUserSms() {
    return newUserSms;
  }

  public void setNewUserSms(String newUserSms) {
    this.newUserSms = newUserSms;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public void setCodeHash(String codeHash) {
    this.codeHash = codeHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
