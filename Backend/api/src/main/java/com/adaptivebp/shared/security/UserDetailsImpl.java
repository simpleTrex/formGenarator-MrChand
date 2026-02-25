package com.adaptivebp.shared.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.adaptivebp.modules.identity.model.User;
import com.adaptivebp.modules.identity.model.Role;

public class UserDetailsImpl implements UserDetails {
	private static final long serialVersionUID = 1L;
	private String id;
	private String domainId;
	private String username;
	private String email;
	@JsonIgnore
	private String password;
	private Collection<? extends GrantedAuthority> authorities;

	public UserDetailsImpl(String id, String domainId, String username, String email, String password,
			Collection<? extends GrantedAuthority> authorities) {
		this.id = id;
		this.domainId = domainId;
		this.username = username;
		this.email = email;
		this.password = password;
		this.authorities = authorities;
	}

	public static UserDetailsImpl build(User user) {
		List<GrantedAuthority> authorities = List.of();
		return new UserDetailsImpl(
			user.getId(),
			user.getDomainId(),
			user.getUsername(),
			user.getEmail(),
			user.getPassword(),
			authorities
		);
	}

	public static UserDetailsImpl build(User user, Set<Role> resolvedRoles) {
		List<GrantedAuthority> authorities = resolvedRoles.stream()
				.map(role -> {
					String authorityName = (role.getRoleName() != null && !role.getRoleName().isEmpty())
						? role.getRoleName()
						: role.getName().name();
					return new SimpleGrantedAuthority(authorityName);
				})
				.collect(Collectors.toList());
		return new UserDetailsImpl(
			user.getId(),
			user.getDomainId(),
			user.getUsername(),
			user.getEmail(),
			user.getPassword(),
			authorities
		);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	public String getId() {
		return id;
	}

	public String getDomainId() {
		return domainId;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		UserDetailsImpl user = (UserDetailsImpl) o;
		return Objects.equals(id, user.id);
	}
}
