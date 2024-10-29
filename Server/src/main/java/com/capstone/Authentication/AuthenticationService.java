package com.capstone.authentication;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.capstone.data.OwnerRepositoryJPA;
import com.capstone.models.Owner;
import com.capstone.models.Role;

@Service
public class AuthenticationService {

    private final OwnerRepositoryJPA repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(OwnerRepositoryJPA repository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }


    public AuthenticationResponse register(RegisterRequest request) {
        Owner owner = new Owner();
        owner.setFirstName(request.getFirstname());
        owner.setLastName(request.getLastname());
        owner.setEmail(request.getEmail());
        owner.setPassword(passwordEncoder.encode(request.getPassword()));
        owner.setRole(Role.USER);

        repository.save(owner);

        Map<String, Object> extraClaims = buildExtraClaims(owner);

        String jwtToken = jwtService.generateToken(extraClaims, owner);

        AuthenticationResponse newResponse = new AuthenticationResponse();
        newResponse.setToken(jwtToken);
        return newResponse;
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        Owner owner = repository.getOwnerByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

        Map<String, Object> extraClaims = buildExtraClaims(owner);

        String jwtToken = jwtService.generateToken(extraClaims, owner);

        AuthenticationResponse response = new AuthenticationResponse();
        response.setToken(jwtToken);
        return response;
    }

    private Map<String, Object> buildExtraClaims(Owner owner) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("firstName", owner.getFirstName());
        extraClaims.put("lastName", owner.getLastName());
        extraClaims.put("ownerId", owner.getOwnerId());
        return extraClaims;
    }

}
