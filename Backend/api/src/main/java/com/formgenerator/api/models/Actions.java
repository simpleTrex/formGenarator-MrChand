package com.formgenerator.api.models;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * This handle what process can be done to the CustomForm object. Each Action
 * will render a button in UI. Additionally their will be two Actions for
 * Constructor, Destructor, FormValidator
 */
@Document
public class Actions {

	/** Button Name */
	private String name;

	/** Script to execute(java script */
	private String script;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

}
