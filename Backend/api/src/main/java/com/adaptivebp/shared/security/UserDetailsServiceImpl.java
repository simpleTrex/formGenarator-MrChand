package com.adaptivebp.shared.security;

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

import com.adaptivebp.modules.identity.model.User;
import com.adaptivebp.modules.identity.model.Role;
import com.adaptivebp.modules.identity.repository.UserRepository;
import com.adaptivebp.modules.identity.repository.RoleRepository;

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

		Set<ObjectId> roleIds = user.getRoles();
		Set<Role> roles = new HashSet<>();
		if (roleIds != null && !roleIds.isEmpty()) {
			roles = roleIds.stream()
					.map(id -> roleRepository.findById(id.toHexString()).orElse(null))
					.filter(r -> r != null)
					.collect(Collectors.toSet());
		}

		return UserDetailsImpl.build(user, roles);
	}
}
