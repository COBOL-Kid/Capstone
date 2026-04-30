package com.capstone.Authentication;

import java.util.Objects;

public class AuthenticationResponse {

	private String token;

	public AuthenticationResponse() {
	}

	public AuthenticationResponse(String token) {
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		AuthenticationResponse that = (AuthenticationResponse) o;
		return Objects.equals(getToken(), that.getToken());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getToken());
	}

	@Override
	public String toString() {
		return "AuthenticationResponse{token='[PROTECTED]'}";
	}
}
