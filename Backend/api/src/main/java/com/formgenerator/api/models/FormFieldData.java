package com.formgenerator.api.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "FormFieldData")
public class FormFieldData {

	@Id
	private long id;
	private String formFieldType;
	private String valueType;
	private String value;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getFormFieldType() {
		return formFieldType;
	}

	public void setFormFieldType(String formFieldType) {
		this.formFieldType = formFieldType;
	}

}
