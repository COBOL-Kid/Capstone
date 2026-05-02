package com.capstone.authentication;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.capstone.configuration.JwtProperties;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/api/auth")
public class AuthenticationController {

	private final AuthenticationService service;
	private final JwtProperties jwtProperties;

	public AuthenticationController(AuthenticationService service, JwtProperties jwtProperties) {
		this.service = service;
		this.jwtProperties = jwtProperties;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthenticationResponse response = service.register(request);
		return ResponseEntity.ok().headers(createCookieHeader(response.getRefreshToken())).body(response);
	}

	@PostMapping("/authenticate")
	public ResponseEntity<AuthenticationResponse> authenticate(@Valid @RequestBody AuthenticationRequest request) {
		AuthenticationResponse response = service.authenticate(request);
		return ResponseEntity.ok().headers(createCookieHeader(response.getRefreshToken())).body(response);
	}
	
	@PostMapping("/refresh")
	public ResponseEntity<AuthenticationResponse> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
		if (refreshToken == null || refreshToken.isEmpty()) {
			return ResponseEntity.status(401).build();
		}
		try {
			AuthenticationResponse response = service.refreshToken(refreshToken);
			return ResponseEntity.ok().headers(createCookieHeader(response.getRefreshToken())).body(response);
		} catch (Exception e) {
			return ResponseEntity.status(401).headers(createCleanCookieHeader()).build();
		}
	}
	
	@PostMapping("/logout")
	public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
		if (refreshToken != null && !refreshToken.isEmpty()) {
			service.logout(refreshToken);
		}
		return ResponseEntity.ok().headers(createCleanCookieHeader()).build();
	}
	
	private HttpHeaders createCookieHeader(String refreshToken) {
		long maxAgeSeconds = jwtProperties.getRefreshExpirationDays() * 24 * 60 * 60;
		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
				.httpOnly(true)
				.secure(true)
				.path("/api/auth")
				.maxAge(maxAgeSeconds)
				.sameSite("Strict")
				.build();
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
		return headers;
	}

	private HttpHeaders createCleanCookieHeader() {
		ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.secure(true)
				.path("/api/auth")
				.maxAge(0)
				.sameSite("Strict")
				.build();
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
		return headers;
	}
}
