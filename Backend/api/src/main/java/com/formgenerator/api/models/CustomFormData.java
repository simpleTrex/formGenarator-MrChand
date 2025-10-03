package com.formgenerator.api.models;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Deprecated
@Document(collection = "CustomFormData")
public class CustomFormData {
	@Id
	private long id;
	private String formName;
	private List<FormField> fields;

	public List<FormField> getFields() {
		return fields;
	}

	public void setFields(List<FormField> fields) {
		this.fields = fields;
	}

	public String getFormName() {
		return formName;
	}

	public void setFormName(String formName) {
		this.formName = formName;
	}

}
