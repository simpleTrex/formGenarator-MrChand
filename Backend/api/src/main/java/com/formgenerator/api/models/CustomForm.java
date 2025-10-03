package com.formgenerator.api.models;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Dynamic Business Object Model
 * 
 */
@Document(collection = "CustomForm")
public class CustomForm {

	/**
	 * get Model of a data object loop in model object's fields find data object's
	 * value if exists on data object and copy
	 */
	public CustomForm updateToModel(CustomForm inModel) {

		System.out.println("Converting data to new model << " + inModel.getName());

		if (inModel.getName().equals(getName())) {
			List<FormField> modelFields = inModel.getFields();
			List<FormField> newFields = new ArrayList<FormField>();
			for (FormField modelField : modelFields) {
				// TODO Need to avoid this N2 by moving fields vector to Map
				for (FormField dataField : getFields()) {
					if (modelField.getName() != null && dataField.getName() != null
							&& modelField.getName().equals(dataField.getName())) {
						System.out.println("Copy field: " + modelField.getName() + "'s value " + dataField.getValue()
								+ " to object");
						/**
						 * Copy value from dataListInput object field to respective field object in a
						 * model object
						 */
						modelField.setValue(dataField.getValue());
						break;
					} else {
						System.out.println("Field copying failed:" + modelField.getName());
					}
				}
				newFields.add(modelField);
			}
			setFields(newFields);
		}

		return this;
	}

	CustomForm() {
	}

	@Id
	private long id;

	/*
	 * This nTh element in fields, identify/represents this object once this is a
	 * sub object in another CustomForm
	 */
	private short idField;

	/** Object Model name. This is mapped to MongoDB Collection/table name */
	// @Field(name = "_class")
	private String name;

	private long version = 0;
	/** new properties in this object */
	private List<FormField> fields;

	/** Connected objects */
	private List<CustomForm> objects;

	private Process process;

	/** Actions on this Object */
	// TODO remove this since it's moved to process object
	@Deprecated
	private FormField action;

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

	public List<FormField> getFields() {
		return fields;
	}

	public void setFields(List<FormField> fields) {
		this.fields = fields;
	}

	public List<CustomForm> getObjects() {
		return objects;
	}

	public void setObjects(List<CustomForm> objects) {
		this.objects = objects;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public short getIdField() {
		return idField;
	}

	public void setIdField(short idField) {
		this.idField = idField;
	}

	public FormField getAction() {
		return action;
	}

	public void setAction(FormField action) {
		this.action = action;
	}

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}

}
