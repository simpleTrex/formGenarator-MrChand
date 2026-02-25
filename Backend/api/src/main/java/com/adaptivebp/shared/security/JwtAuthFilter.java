package com.adaptivebp.shared.security;

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

import com.adaptivebp.modules.identity.service.AdaptivePrincipalService;

public class JwtAuthFilter extends OncePerRequestFilter {

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private UserDetailsServiceImpl userDetailsService;

	@Autowired
	private AdaptivePrincipalService adaptivePrincipalService;

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		try {
			String jwt = parseJwt(request);
			if (jwt != null && jwtTokenProvider.validateJwtToken(jwt)) {
				JwtPrincipalClaims claims = jwtTokenProvider.parseClaims(jwt);
				if (claims.getPrincipalType() != null && claims.getPrincipalId() != null) {
					adaptivePrincipalService.loadById(claims.getPrincipalId(), claims.getPrincipalType(),
							claims.getDomainId()).ifPresent(principal -> {
								UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
										principal, null, principal.getAuthorities());
								authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
								SecurityContextHolder.getContext().setAuthentication(authentication);
							});
				} else {
					String username = jwtTokenProvider.getUserNameFromJwtToken(jwt);
					UserDetails userDetails = userDetailsService.loadUserByUsername(username);
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
		} catch (Exception e) {
			logger.error("Cannot set user authentication: {}", e);
		}

		filterChain.doFilter(request, response);
	}

	private String parseJwt(HttpServletRequest request) {
		return jwtTokenProvider.getJwtFromCookies(request);
	}
}
