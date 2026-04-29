package com.capstone.Authentication;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.DuplicateEmailException;
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

	@Transactional
	public AuthenticationResponse register(RegisterRequest request) {
		String email = EmailNormalizer.normalize(request.getEmail());
		if (repository.existsByUserEmail(email)) {
			throw new DuplicateEmailException();
		}
		User user = new User();
		user.setFirstName(clean(request.getFirstname()));
		user.setLastName(clean(request.getLastname()));
		user.setUserEmail(email);
		user.setUserPw(passwordEncoder.encode(request.getPassword()));
		user.setRole(Role.USER);

		try {
			repository.saveAndFlush(user);
		} catch (DataIntegrityViolationException ex) {
			throw new DuplicateEmailException();
		}

		Map<String, Object> extraClaims = buildExtraClaims(user);

		String jwtToken = jwtService.generateToken(extraClaims, user);

		AuthenticationResponse newResponse = new AuthenticationResponse();
		newResponse.setToken(jwtToken);
		return newResponse;
	}

	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		String email = EmailNormalizer.normalize(request.getEmail());
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
		User user = repository.findByUserEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

		Map<String, Object> extraClaims = buildExtraClaims(user);

		String jwtToken = jwtService.generateToken(extraClaims, user);

		AuthenticationResponse response = new AuthenticationResponse();
		response.setToken(jwtToken);
		return response;
	}

	private Map<String, Object> buildExtraClaims(User user) {
		Map<String, Object> extraClaims = new HashMap<>();
		extraClaims.put("userId", user.getUserId());
		return extraClaims;
	}

	private String clean(String value) {
		return value != null ? value.trim() : null;
	}

}
