package com.capstone.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.capstone.configuration.JwtProperties;
import com.capstone.data.RefreshTokenRepository;
import com.capstone.data.UserRepositoryJPA;
import com.capstone.domain.DuplicateEmailException;
import com.capstone.models.RefreshToken;
import com.capstone.models.Role;
import com.capstone.models.User;

class AuthenticationServiceTest {

	@Test
	void shouldRegisterUserWithEncodedPasswordAndTokenClaims() {
		UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		JwtService jwtService = mock(JwtService.class);
		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
		JwtProperties jwtProperties = mock(JwtProperties.class);
		AuthenticationService service = new AuthenticationService(repository, refreshTokenRepository, passwordEncoder, jwtService,
				authenticationManager, jwtProperties);
		RegisterRequest request = new RegisterRequest(" Pat ", " Driver ", " DRIVER@Example.COM ", "secret");

		when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
		when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("jwt-token");

		var response = service.register(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(repository).saveAndFlush(userCaptor.capture());
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
	void shouldRejectDuplicateRegistrationEmail() {
		UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		AuthenticationService service = new AuthenticationService(repository, mock(RefreshTokenRepository.class), passwordEncoder, mock(JwtService.class),
				mock(AuthenticationManager.class), mock(JwtProperties.class));

		when(repository.existsByUserEmail("driver@example.com")).thenReturn(true);

		assertThrows(DuplicateEmailException.class,
				() -> service.register(new RegisterRequest("Pat", "Driver", " Driver@Example.COM ", "secret")));
		verify(repository, never()).saveAndFlush(any(User.class));
		verify(passwordEncoder, never()).encode(any());
	}

	@Test
	void shouldTranslateDataIntegrityViolationOnConcurrentRegistrationToDuplicateEmail() {
		UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		AuthenticationService service = new AuthenticationService(repository, mock(RefreshTokenRepository.class), passwordEncoder, mock(JwtService.class),
				mock(AuthenticationManager.class), mock(JwtProperties.class));

		when(repository.existsByUserEmail("driver@example.com")).thenReturn(false);
		when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
		when(repository.saveAndFlush(any(User.class)))
				.thenThrow(new DataIntegrityViolationException("uk_user_email"));

		assertThrows(DuplicateEmailException.class,
				() -> service.register(new RegisterRequest("Pat", "Driver", "driver@example.com", "secret")));
	}

	@Test
	void shouldAuthenticateCredentialsBeforeGeneratingToken() {
		UserRepositoryJPA repository = mock(UserRepositoryJPA.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		JwtService jwtService = mock(JwtService.class);
		AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
		RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
		JwtProperties jwtProperties = mock(JwtProperties.class);
		AuthenticationService service = new AuthenticationService(repository, refreshTokenRepository, passwordEncoder, jwtService,
				authenticationManager, jwtProperties);
		AuthenticationRequest request = new AuthenticationRequest(" DRIVER@Example.COM ", "secret");
		User user = new User();
		user.setUserId(1L);
		user.setFirstName("Pat");
		user.setLastName("Driver");
		user.setUserEmail("driver@example.com");
		user.setUserPw("encoded-secret");
		user.setRole(Role.USER);

		when(repository.findByUserEmail("driver@example.com")).thenReturn(Optional.of(user));
		when(jwtProperties.getRefreshExpirationDays()).thenReturn(7L);
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
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

	@Test
	void shouldRedactSensitiveValuesFromToString() {
		assertFalse(new RegisterRequest("Pat", "Driver", "driver@example.com", "secret").toString().contains("secret"));
		assertFalse(new AuthenticationRequest("driver@example.com", "secret").toString().contains("secret"));
		assertFalse(new AuthenticationResponse("jwt-token").toString().contains("jwt-token"));
	}

	@SuppressWarnings("unchecked")
	private void assertTokenClaims(JwtService jwtService, User user) {
		ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(jwtService).generateToken(claimsCaptor.capture(), org.mockito.ArgumentMatchers.same(user));
		Map<String, Object> claims = claimsCaptor.getValue();
		assertFalse(claims.containsKey("firstName"));
		assertFalse(claims.containsKey("lastName"));
		assertEquals(user.getUserId(), claims.get("userId"));
	}
}
