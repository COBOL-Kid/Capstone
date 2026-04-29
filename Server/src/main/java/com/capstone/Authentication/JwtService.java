package com.capstone.Authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
	private static final String secretKey = System.getenv("SECRET_KEY");

	public String extractUserEmail(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = getClaimsFromToken(token);
		return claimsResolver.apply(claims);
	}

	public String generateToken(UserDetails owner) {
		return generateToken(new HashMap<>(), owner);
	}

	public String generateToken(Map<String, Object> extraClaims, UserDetails ownerDetails) {
		return Jwts.builder().claims(extraClaims).subject(ownerDetails.getUsername())
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24))
				.signWith(getSignInKey(), Jwts.SIG.HS256).compact();
	}

	public boolean validateToken(String token, UserDetails ownerDetails) {
		final String userEmail = extractUserEmail(token);
		return (userEmail.equals(ownerDetails.getUsername()) && !isTokenExpired(token));
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private Claims getClaimsFromToken(String token) {
		return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
