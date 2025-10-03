package com.formgenerator.api.models;

import java.util.List;

/** This class handles the Business Process mapping for @CustomForm objects */
public class Process {

	/** Actions on this Object */
	private FormField action;

	/**
	 * Authentication object to control access, execute, etc permission to this
	 * object
	 */
	private ModelAuth mAuth;

	/** Authentication object to control access to the data object of this model */
	private DataAuth dAuth;

	/** Status of Model object of this model */
	private ModelStatus mStatus;

	/** Status of data object of this model */
	private DataStatus dStatus;

	/** Connected objects */
	private List<CustomForm> objects;

	public List<CustomForm> getObjects() {
		return objects;
	}

	public void setObjects(List<CustomForm> objects) {
		this.objects = objects;
	}

	public FormField getAction() {
		return action;
	}

	public void setAction(FormField action) {
		this.action = action;
	}

	public ModelAuth getmAuth() {
		return mAuth;
	}

	public void setmAuth(ModelAuth mAuth) {
		this.mAuth = mAuth;
	}

	public DataAuth getdAuth() {
		return dAuth;
	}

	public void setdAuth(DataAuth dAuth) {
		this.dAuth = dAuth;
	}

	public ModelStatus getmStatus() {
		return mStatus;
	}

	public void setmStatus(ModelStatus mStatus) {
		this.mStatus = mStatus;
	}

	public DataStatus getdStatus() {
		return dStatus;
	}

	public void setdStatus(DataStatus dStatus) {
		this.dStatus = dStatus;
	}

}
