package com.adaptivebp.modules.identity.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adaptivebp.modules.identity.dto.request.ChangePasswordRequest;
import com.adaptivebp.modules.identity.dto.request.CreateRoleRequest;
import com.adaptivebp.modules.identity.dto.response.MessageResponse;
import com.adaptivebp.modules.identity.model.ERole;
import com.adaptivebp.modules.identity.model.Role;
import com.adaptivebp.modules.identity.model.User;
import com.adaptivebp.modules.identity.repository.RoleRepository;
import com.adaptivebp.modules.identity.repository.UserRepository;

import org.bson.types.ObjectId;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/custom_form/user")
public class UserController {

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	PasswordEncoder encoder;

	@PostMapping("/change-password")
	public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
		if (userRepository.existsByUsername(changePasswordRequest.getUsername())) {
			if (userRepository.existsByEmail(changePasswordRequest.getEmail())) {
				User user = userRepository.findByUsername(changePasswordRequest.getUsername()).get();
				if (encoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
					user.setPassword(encoder.encode(changePasswordRequest.getNewPassword()));
					userRepository.save(user);
					return ResponseEntity.ok(new MessageResponse("User Password changed successfully!"));
				} else {
					return ResponseEntity.badRequest().body(new MessageResponse("Error: Old Password does not match."));
				}
			} else {
				return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is not valid!"));
			}
		} else {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is not valid!"));
		}
	}

	@PostMapping("/roles")
	public ResponseEntity<?> createUserRole(@Valid @RequestBody CreateRoleRequest createRoleRequest) {
		if (createRoleRequest.getRoleName().isEmpty() || createRoleRequest.getRoleName().isBlank()) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Role Name is not valid!"));
		} else {
			String roleName = createRoleRequest.getRoleName().toUpperCase();
			ERole mappedERole;
			switch (roleName) {
				case "BUSINESS_OWNER":
					mappedERole = ERole.ROLE_BUSINESS_OWNER;
					break;
				case "DOMAIN_ADMIN":
					mappedERole = ERole.ROLE_DOMAIN_ADMIN;
					break;
				case "APP_ADMIN":
					mappedERole = ERole.ROLE_APP_ADMIN;
					break;
				case "BUSINESS_USER":
					mappedERole = ERole.ROLE_BUSINESS_USER;
					break;
				default:
					return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid role name!"));
			}
			Role role = new Role();
			role.setRoleName(roleName);
			role.setName(mappedERole);
			roleRepository.save(role);
			return ResponseEntity.ok(new MessageResponse("User Role Created successfully!"));
		}
	}

	@PostMapping(path = "/{userIdentity}/roles")
	public ResponseEntity<?> assignRoleToUser(@PathVariable String userIdentity,
			@Valid @RequestBody CreateRoleRequest createRoleRequest) {
		User user = userRepository.findById(userIdentity).get();
		Set<Role> roles = user.getRoles().stream()
			.map(id -> roleRepository.findById(id.toHexString()).orElse(null))
			.filter(r -> r != null)
			.collect(Collectors.toSet());

		Role newRole = roleRepository.findByRoleName(createRoleRequest.getRoleName().toUpperCase()).get();
		if (newRole == null) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Role Name is not valid!"));
		} else {
			if (roles.contains(newRole)) {
				return ResponseEntity.badRequest().body(new MessageResponse("Error: Role is already assigned!"));
			} else {
				roles.add(newRole);
				Set<ObjectId> roleIds = roles.stream().map(r -> new ObjectId(r.getId())).collect(Collectors.toSet());
				user.setRoles(roleIds);
				userRepository.save(user);
				return ResponseEntity.ok(new MessageResponse("User Role Updated successfully!"));
			}
		}
	}
}
