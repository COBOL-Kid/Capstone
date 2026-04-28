package com.capstone.Authentication;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.capstone.data.UserRepositoryJPA;
import com.capstone.models.Role;
import com.capstone.models.User;

@Service
public class AuthenticationService {

	private final UserRepositoryJPA repository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	public AuthenticationService(UserRepositoryJPA repository, PasswordEncoder passwordEncoder, JwtService jwtService,
			AuthenticationManager authenticationManager) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.authenticationManager = authenticationManager;
	}

	public AuthenticationResponse register(RegisterRequest request) {
		User user = new User();
		user.setFirstName(request.getFirstname());
		user.setLastName(request.getLastname());
		user.setUserEmail(request.getEmail());
		user.setUserPw(passwordEncoder.encode(request.getPassword()));
		user.setRole(Role.USER);

		repository.save(user);

		Map<String, Object> extraClaims = buildExtraClaims(user);

		String jwtToken = jwtService.generateToken(extraClaims, user);

		AuthenticationResponse newResponse = new AuthenticationResponse();
		newResponse.setToken(jwtToken);
		return newResponse;
	}

	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
		User user = repository.findByUserEmail(request.getEmail())
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

		Map<String, Object> extraClaims = buildExtraClaims(user);

		String jwtToken = jwtService.generateToken(extraClaims, user);

		AuthenticationResponse response = new AuthenticationResponse();
		response.setToken(jwtToken);
		return response;
	}

	private Map<String, Object> buildExtraClaims(User user) {
		Map<String, Object> extraClaims = new HashMap<>();
		extraClaims.put("firstName", user.getFirstName());
		extraClaims.put("lastName", user.getLastName());
		extraClaims.put("userId", user.getUserId());
		return extraClaims;
	}

}
