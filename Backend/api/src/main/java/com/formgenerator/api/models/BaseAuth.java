package com.formgenerator.api.models;

import com.formgenerator.api.models.Auth.AUTH_ENUM;

public class BaseAuth {

	private Auth ownerAuth;
	private Auth groupAuth;
	private Auth otherAuth;

	BaseAuth() {
		ownerAuth.getAuth().add(AUTH_ENUM.R);
		ownerAuth.getAuth().add(AUTH_ENUM.W);

		ownerAuth.getAuth().add(AUTH_ENUM.R);

		otherAuth.getAuth().clear();
	}

	public Auth getOwnerAuth() {
		return ownerAuth;
	}

	public void setOwnerAuth(Auth ownerAuth) {
		this.ownerAuth = ownerAuth;
	}

	public Auth getGroupAuth() {
		return groupAuth;
	}

	public void setGroupAuth(Auth groupAuth) {
		this.groupAuth = groupAuth;
	}

	public Auth getOtherAuth() {
		return otherAuth;
	}

	public void setOtherAuth(Auth otherAuth) {
		this.otherAuth = otherAuth;
	}

}
