package com.adaptivebp.shared.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtTokenProvider {

	private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

	@Value("${uigenerator.app.jwtSecret}")
	private String jwtSecret;

	@Value("${uigenerator.app.jwtExpirationMs}")
	private int jwtExpirationMs;

	@Value("${uigenerator.app.jwtCookieName}")
	private String jwtCookie;

	private SecretKey signingKey;

	@PostConstruct
	private void init() {
		this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	public String getJwtFromCookies(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, jwtCookie);
		String token = request.getHeader("token");
		if (cookie != null) {
			return cookie.getValue();
		} else if (token != null) {
			return token;
		}
		return null;
	}

	public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
		String jwt = generateTokenFromUsername(userPrincipal.getUsername());
		return ResponseCookie.from(jwtCookie, jwt)
				.path("/").maxAge(24 * 60 * 60).httpOnly(true).sameSite("None").build();
	}

	public ResponseCookie generateOwnerJwtCookie(AdaptiveUserDetails principal) {
		String jwt = generateToken(principal);
		return ResponseCookie.from(jwtCookie, jwt)
				.path("/").maxAge(24 * 60 * 60).httpOnly(true).sameSite("None").build();
	}

	public ResponseCookie generateDomainJwtCookie(AdaptiveUserDetails principal) {
		return generateOwnerJwtCookie(principal);
	}

	public ResponseCookie getCleanJwtCookie() {
		return ResponseCookie.from(jwtCookie, "").path("/").build();
	}

	public String getUserNameFromJwtToken(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}

	public JwtPrincipalClaims parseClaims(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		String principalId = claims.get("pid", String.class);
		String principalType = claims.get("ptype", String.class);
		String domainId = claims.get("domainId", String.class);
		String username = claims.getSubject();

		PrincipalType type = null;
		if (principalType != null) {
			try {
				type = PrincipalType.valueOf(principalType);
			} catch (IllegalArgumentException ignored) {
			}
		}
		return new JwtPrincipalClaims(principalId, type, domainId, username);
	}

	public boolean validateJwtToken(String authToken) {
		try {
			Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(authToken);
			return true;
		} catch (SecurityException e) {
			logger.error("Invalid JWT signature: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			logger.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			logger.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			logger.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("JWT claims string is empty: {}", e.getMessage());
		}
		return false;
	}

	public String generateTokenFromUsername(String username) {
		Date now = new Date();
		return Jwts.builder()
				.subject(username)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + jwtExpirationMs))
				.signWith(signingKey)
				.compact();
	}

	public String generateToken(AdaptiveUserDetails principal) {
		Date now = new Date();
		return Jwts.builder()
				.subject(principal.getUsername())
				.claim("pid", principal.getId())
				.claim("ptype", principal.getPrincipalType().name())
				.claim("domainId", principal.getDomainId())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + jwtExpirationMs))
				.signWith(signingKey)
				.compact();
	}
}
