package com.formgenerator.platform.auth;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.formgenerator.platform.auth.JwtPrincipalClaims;

public class AuthTokenFilter extends OncePerRequestFilter {

	@Autowired
	private JwtUtils jwtUtils;

	@Autowired
	private UserDetailsServiceImpl userDetailsService;

	@Autowired
	private com.formgenerator.api.services.AdaptivePrincipalService adaptivePrincipalService;

	private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		try {
			String jwt = parseJwt(request);

			if (jwt != null) {
				System.out.println("DEBUG: AuthTokenFilter processing JWT: "
						+ jwt.substring(0, Math.min(jwt.length(), 20)) + "...");
			} else {
				System.out.println("DEBUG: AuthTokenFilter - No JWT found in request");
			}

			if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
				JwtPrincipalClaims claims = jwtUtils.parseClaims(jwt);
				System.out.println("DEBUG: AuthTokenFilter Claims - PID: " + claims.getPrincipalId() +
						", Type: " + claims.getPrincipalType() +
						", Domain: " + claims.getDomainId());

				if (claims.getPrincipalType() != null && claims.getPrincipalId() != null) {
					System.out.println("DEBUG: AuthTokenFilter - Loading principal by ID...");
					java.util.Optional<AdaptiveUserDetails> principalOpt = adaptivePrincipalService.loadById(
							claims.getPrincipalId(), claims.getPrincipalType(),
							claims.getDomainId());

					if (principalOpt.isPresent()) {
						AdaptiveUserDetails principal = principalOpt.get();
						System.out.println("DEBUG: AuthTokenFilter - Principal Found: " + principal.getUsername()
								+ ", Authorities: " + principal.getAuthorities());
						UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
								principal, null, principal.getAuthorities());
						authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authentication);
						System.out.println("DEBUG: AuthTokenFilter - Authentication set in SecurityContext");
					} else {
						System.out.println(
								"DEBUG: AuthTokenFilter - Principal NOT found for ID: " + claims.getPrincipalId());
					}
				} else {
					System.out.println("DEBUG: AuthTokenFilter - Claims missing PID or Type, trying username...");
					String username = jwtUtils.getUserNameFromJwtToken(jwt);
					UserDetails userDetails = userDetailsService.loadUserByUsername(username);
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			} else {
				if (jwt != null)
					System.out.println("DEBUG: AuthTokenFilter - JWT Validation Failed");
			}
		} catch (Exception e) {
			logger.error("Cannot set user authentication: {}", e);
			e.printStackTrace();
		}
		// response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");

		filterChain.doFilter(request, response);
	}

	private String parseJwt(HttpServletRequest request) {
		String jwt = jwtUtils.getJwtFromCookies(request);
		System.out.println(">>auth token: " + jwt);
		return jwt;
	}
}
