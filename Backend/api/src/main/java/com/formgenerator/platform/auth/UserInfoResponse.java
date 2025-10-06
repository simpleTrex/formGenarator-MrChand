package com.formgenerator.platform.auth;

import java.util.List;

public class UserInfoResponse {

	private String id;
	private String username;
	private String email;
	private String token;
	List<String> roles;

	public UserInfoResponse(String id, String username, String email, List<String> roles, String token) {
		this.id = id;
		this.username = username;
		this.email = email;
		this.roles = roles;
		this.token = token;//TODO remove once cocooie read
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

}
