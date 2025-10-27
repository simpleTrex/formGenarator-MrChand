package com.formgenerator.api.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.formgenerator.api.repository.RoleRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.repository.UserRepository;
import com.formgenerator.platform.auth.ERole;
import com.formgenerator.platform.auth.JwtUtils;
import com.formgenerator.platform.auth.LoginRequest;
import com.formgenerator.platform.auth.MessageResponse;
import com.formgenerator.platform.auth.Role;
import com.formgenerator.platform.auth.Domain;
import org.bson.types.ObjectId;
import com.formgenerator.platform.auth.SignupRequest;
import com.formgenerator.platform.auth.User;
import com.formgenerator.platform.auth.UserDetailsImpl;
import com.formgenerator.platform.auth.UserInfoResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/custom_form/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	UserRepository userRepository;
	@Autowired
	RoleRepository roleRepository;
	@Autowired
	DomainRepository domainRepository;
	@Autowired
	PasswordEncoder encoder;
	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/login")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		// Ensure user has a domain assigned; if not, reject login for now
		if (userDetails.getDomainId() == null || userDetails.getDomainId().isBlank()) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: User has no domain assigned."));
		}
		ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
		List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
				.collect(Collectors.toList());
		for (String role : roles) {
			System.out.println(role);
		}

		System.out.println(jwtCookie.toString());

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString()).body(new UserInfoResponse(
				userDetails.getId(), userDetails.getDomainId(), userDetails.getUsername(), userDetails.getEmail(), roles, jwtCookie.getValue()));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
		}
		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
		}
		// Create new user's account
		User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(),
				encoder.encode(signUpRequest.getPassword()));
		Set<String> strRoles = signUpRequest.getRoles();
	Set<Role> roles = new HashSet<>();
		if (strRoles == null) {
			Role defaultRole = roleRepository.findByRoleName("BUSINESS_USER")
					.orElseGet(() -> roleRepository.findByName(ERole.ROLE_BUSINESS_USER)
							.orElseThrow(() -> new RuntimeException("Error: Default role is not found.")));
			roles.add(defaultRole);
		} else {
			strRoles.forEach(role -> {
				switch (role) {
				case "BUSINESS_OWNER":
					Role businessOwnerRole = roleRepository.findByRoleName("BUSINESS_OWNER")
							.orElseGet(() -> roleRepository.findByName(ERole.ROLE_BUSINESS_OWNER)
									.orElseThrow(() -> new RuntimeException("Error: Role is not found.")));
					roles.add(businessOwnerRole);
					break;
				case "DOMAIN_ADMIN":
				case "admin":
					Role domainAdminRole = roleRepository.findByRoleName("DOMAIN_ADMIN")
							.orElseGet(() -> roleRepository.findByName(ERole.ROLE_DOMAIN_ADMIN)
									.orElseThrow(() -> new RuntimeException("Error: Role is not found.")));
					roles.add(domainAdminRole);
					break;
				case "APP_ADMIN":
					Role appAdminRole = roleRepository.findByRoleName("APP_ADMIN")
							.orElseGet(() -> roleRepository.findByName(ERole.ROLE_APP_ADMIN)
									.orElseThrow(() -> new RuntimeException("Error: Role is not found.")));
					roles.add(appAdminRole);
					break;
				case "BUSINESS_USER":
				case "user":
				default:
					Role businessUserRole = roleRepository.findByRoleName("BUSINESS_USER")
							.orElseGet(() -> roleRepository.findByName(ERole.ROLE_BUSINESS_USER)
									.orElseThrow(() -> new RuntimeException("Error: Role is not found.")));
					roles.add(businessUserRole);
				}
			});
		}
	// convert Role entities to ObjectId references for User.roles
	Set<ObjectId> roleIds = roles.stream().map(r -> new ObjectId(r.getId())).collect(Collectors.toSet());
	user.setRoles(roleIds);
		// First save to generate an ID for the user
		User saved = userRepository.save(user);
		// Assign a domainId according to simple policy
		String domainId = assignDomainForUser(saved, roles);
		saved.setDomainId(domainId);
		userRepository.save(saved);
		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}

	// Simple domain assignment policy:
	// - If user has BUSINESS_OWNER role, create a dedicated domain owned by the user (name based on username)
	// - Otherwise, assign (or create) a global domain named "global"
	private String assignDomainForUser(User savedUser, Set<Role> roles) {
		boolean isBusinessOwner = roles.stream().anyMatch(r ->
			("BUSINESS_OWNER".equalsIgnoreCase(r.getRoleName())) || r.getName() == ERole.ROLE_BUSINESS_OWNER);
		if (isBusinessOwner) {
			String baseName = savedUser.getUsername() != null ? savedUser.getUsername().toLowerCase() : "domain";
			String name = baseName;
			// Ensure uniqueness in a simple way
			if (domainRepository.existsByName(name)) {
				name = baseName + "-" + savedUser.getId().substring(0, Math.min(6, savedUser.getId().length()));
			}
			Domain d = new Domain(name, savedUser.getId());
			Domain created = domainRepository.save(d);
			return created.getId();
		} else {
			return domainRepository.findByName("global")
				.map(Domain::getId)
				.orElseGet(() -> {
					Domain global = new Domain("global", savedUser.getId());
					Domain created = domainRepository.save(global);
					return created.getId();
				});
		}
	}
}
