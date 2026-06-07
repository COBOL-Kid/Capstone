package com.capstone.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_verification_code")
public class EmailVerificationCode {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
  private User user;

  @Column(name = "code_hash", nullable = false, length = 100)
  private String codeHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "sign_in_challenge_hash", length = 100)
  private String signInChallengeHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  public EmailVerificationCode() {}

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

  public String getSignInChallengeHash() {
    return signInChallengeHash;
  }

  public void setSignInChallengeHash(String signInChallengeHash) {
    this.signInChallengeHash = signInChallengeHash;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public void setFailedAttempts(int failedAttempts) {
    this.failedAttempts = failedAttempts;
  }
}
