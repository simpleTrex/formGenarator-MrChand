package com.formgenerator.platform.auth;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.formgenerator.api.repository.UserRepository;
import com.formgenerator.api.repository.RoleRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Override
	@Transactional
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

		// user.roles now contains ObjectId references inserted by the generator.
		// Resolve them to Role entities and set as authorities in UserDetailsImpl.
		Set<ObjectId> roleIds = user.getRoles();
		Set<Role> roles = new HashSet<>();
		if (roleIds != null && !roleIds.isEmpty()) {
			roles = roleIds.stream()
					.map(id -> roleRepository.findById(id.toHexString()).orElse(null))
					.filter(r -> r != null)
					.collect(Collectors.toSet());
		}

		// Build UserDetails with resolved Role entities (authorities).
		return UserDetailsImpl.build(user, roles);
	}
}