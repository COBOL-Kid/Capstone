package com.capstone.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {

  @JsonIgnore private String token;

  private Boolean emailVerified;

  private Boolean verificationRequired;

  private String verificationChallenge;

  @JsonIgnore private String refreshToken;

  public AuthenticationResponse() {}

  public AuthenticationResponse(String token) {
    this.token = token;
  }

  public AuthenticationResponse(String token, String refreshToken) {
    this.token = token;
    this.refreshToken = refreshToken;
  }

  public static AuthenticationResponse verifiedSession(String token, String refreshToken) {
    AuthenticationResponse response = new AuthenticationResponse(token, refreshToken);
    response.setEmailVerified(true);
    return response;
  }

  public static AuthenticationResponse unverifiedSession(String token, String refreshToken) {
    AuthenticationResponse response = new AuthenticationResponse(token, refreshToken);
    response.setEmailVerified(false);
    return response;
  }

  public static AuthenticationResponse verificationRequired(String verificationChallenge) {
    AuthenticationResponse response = new AuthenticationResponse();
    response.setVerificationRequired(true);
    response.setVerificationChallenge(verificationChallenge);
    return response;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Boolean getEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(Boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public Boolean getVerificationRequired() {
    return verificationRequired;
  }

  public void setVerificationRequired(Boolean verificationRequired) {
    this.verificationRequired = verificationRequired;
  }

  public String getVerificationChallenge() {
    return verificationChallenge;
  }

  public void setVerificationChallenge(String verificationChallenge) {
    this.verificationChallenge = verificationChallenge;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public boolean hasRefreshToken() {
    return refreshToken != null && !refreshToken.isBlank();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AuthenticationResponse that = (AuthenticationResponse) o;
    return Objects.equals(getToken(), that.getToken())
        && Objects.equals(getEmailVerified(), that.getEmailVerified())
        && Objects.equals(getVerificationRequired(), that.getVerificationRequired())
        && Objects.equals(getVerificationChallenge(), that.getVerificationChallenge());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getToken(), getEmailVerified(), getVerificationRequired(), getVerificationChallenge());
  }

  @Override
  public String toString() {
    return "AuthenticationResponse{token='[PROTECTED]'}";
  }
}
