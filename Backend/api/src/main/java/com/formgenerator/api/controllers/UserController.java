package com.formgenerator.api.controllers;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.formgenerator.api.models.ChangePasswordModel;
import com.formgenerator.api.models.CreateRoleModel;
import com.formgenerator.api.repository.RoleRepository;
import com.formgenerator.api.repository.UserRepository;
import com.formgenerator.platform.auth.ERole;
import com.formgenerator.platform.auth.MessageResponse;
import com.formgenerator.platform.auth.Role;
import com.formgenerator.platform.auth.User;

import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping("/custom_form/user")
public class UserController {

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	PasswordEncoder encoder;

	@PostMapping("/change-password")
	public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordModel changePasswordModel) {

		if (userRepository.existsByUsername(changePasswordModel.getUsername())) {

			if (userRepository.existsByEmail(changePasswordModel.getEmail())) {

				User user = userRepository.findByUsername(changePasswordModel.getUsername()).get();

				if (user.getPassword() == encoder.encode(changePasswordModel.getOldPassword())) {

					user.setPassword(encoder.encode(changePasswordModel.getNewPassword()));
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
	public ResponseEntity<?> createUserRole(@Valid @RequestBody CreateRoleModel createRoleModel) {

		if (createRoleModel.getRoleName().isEmpty() || createRoleModel.getRoleName().isBlank()) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Role Name is not valid!"));
		} else {

			Role role = new Role();
			role.setRoleName(createRoleModel.getRoleName().toUpperCase());
			role.setName(ERole.ROLE_ADMIN);
			roleRepository.save(role);

			return ResponseEntity.ok(new MessageResponse("User Role Created successfully!"));
		}
	}

	@PostMapping(path = "/{userIdentity}/roles")
	public ResponseEntity<?> assignRoleToUser(@PathVariable String userIdentity,
			@Valid @RequestBody CreateRoleModel createRoleModel) {

		User user = userRepository.findById(userIdentity).get();
		Set<Role> roles = user.getRoles();

		Role newRole = roleRepository.findByRoleName(createRoleModel.getRoleName().toUpperCase()).get();

		if (newRole == null) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Role Name is not valid!"));
		} else {

			if (roles.contains(newRole)) {
				return ResponseEntity.badRequest().body(new MessageResponse("Error: Role is already assigned!"));
			} else {
				roles.add(newRole);
				user.setRoles(roles);
				userRepository.save(user);
				return ResponseEntity.ok(new MessageResponse("User Role Updated successfully!"));
			}

		}

	}

}
