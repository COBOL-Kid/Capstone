package com.capstone.models;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserVinId implements Serializable {

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "vin_num", columnDefinition = "char(17)")
	private String vin;

	public UserVinId() {
	}

	public UserVinId(Long userId, String vin) {
		this.userId = userId;
		this.vin = vin;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getVin() {
		return vin;
	}

	public void setVin(String vin) {
		this.vin = vin;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UserVinId that)) {
			return false;
		}
		return Objects.equals(userId, that.userId) && Objects.equals(vin, that.vin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, vin);
	}
}
