package com.capstone.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.capstone.data.UserRepositoryJPA;
import com.capstone.models.Role;
import com.capstone.models.User;

class AuthenticationServiceTest {

    @Test
    void shouldRegisterUserWithEncodedPasswordAndTokenClaims() {
        UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AuthenticationService service = new AuthenticationService(repository, passwordEncoder, jwtService,
                authenticationManager);
        RegisterRequest request = new RegisterRequest("Pat", "Driver", "driver@example.com", "secret");

        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("jwt-token");

        var response = service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("Pat", savedUser.getFirstName());
        assertEquals("Driver", savedUser.getLastName());
        assertEquals("driver@example.com", savedUser.getUserEmail());
        assertEquals("encoded-secret", savedUser.getUserPw());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals("jwt-token", response.getToken());
        assertTokenClaims(jwtService, savedUser);
    }

    @Test
    void shouldAuthenticateCredentialsBeforeGeneratingToken() {
        UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        AuthenticationService service = new AuthenticationService(repository, passwordEncoder, jwtService,
                authenticationManager);
        AuthenticationRequest request = new AuthenticationRequest("driver@example.com", "secret");
        User user = new User();
        user.setUserId(1L);
        user.setFirstName("Pat");
        user.setLastName("Driver");
        user.setUserEmail("driver@example.com");
        user.setUserPw("encoded-secret");
        user.setRole(Role.USER);

        when(repository.findByUserEmail("driver@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("jwt-token");

        var response = service.authenticate(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor = ArgumentCaptor
                .forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        assertEquals("driver@example.com", authenticationCaptor.getValue().getPrincipal());
        assertEquals("secret", authenticationCaptor.getValue().getCredentials());
        assertEquals("jwt-token", response.getToken());
        assertTokenClaims(jwtService, user);
    }

    @SuppressWarnings("unchecked")
    private void assertTokenClaims(JwtService jwtService, User user) {
        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jwtService).generateToken(claimsCaptor.capture(), org.mockito.ArgumentMatchers.same(user));
        Map<String, Object> claims = claimsCaptor.getValue();
        assertEquals(user.getFirstName(), claims.get("firstName"));
        assertEquals(user.getLastName(), claims.get("lastName"));
        assertEquals(user.getUserId(), claims.get("userId"));
    }
}
