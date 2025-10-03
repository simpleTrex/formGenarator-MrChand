package com.formgenerator.api.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "FormField")
public class FormField {
	@Id
	private long id;
	private String name;
	private String elementType;
	private String placeHolder;
	private String value;
	private String model;
	private List<CustomOptions> options;
	private List<CustomRegularExpression> regularExpression;

	/*
	 * Validation requirements: Required Min length Max length pattern
	 * 
	 */

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getElementType() {
		return elementType;
	}

	public void setElementType(String elementType) {
		this.elementType = elementType;
	}

	public String getPlaceHolder() {
		return placeHolder;
	}

	public void setPlaceHolder(String placeHolder) {
		this.placeHolder = placeHolder;
	}

	public List<CustomOptions> getOptions() {
		return options;
	}

	public void setOptions(List<CustomOptions> options) {
		this.options = options;
	}

	public List<CustomRegularExpression> getRegularExpression() {
		return regularExpression;
	}

	public void setRegularExpression(List<CustomRegularExpression> regularExpression) {
		this.regularExpression = regularExpression;
	}

	public FormField() {
		this.options = new ArrayList<CustomOptions>();
		this.regularExpression = new ArrayList<CustomRegularExpression>();
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}
}
