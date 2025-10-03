package com.formgenerator.api.models;

import java.util.EnumSet;

public class Auth {
	/**
	 * <code>
	 * E - Empty No permission [0]
	 * R-Read/View/Search [4]
	 * W-Write/Edit/Update [2]
	 * X-eXecute/Approve/Reject/Process [1]
	 * </code>
	 */

	public static enum AUTH_ENUM {
		X, W, R
	}

	private EnumSet<AUTH_ENUM> auth;

	Auth() {
		auth.clear();
	}

	public EnumSet<AUTH_ENUM> getAuth() {
		return auth;
	}

	public void setAuth(EnumSet<AUTH_ENUM> auth) {
		this.auth = auth;
	}

}
