package com.formgenerator.api.controllers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formgenerator.api.models.CustomForm;
import com.formgenerator.api.models.CustomOptions;
import com.formgenerator.api.models.CustomRegularExpression;
import com.formgenerator.api.models.FormField;
import com.formgenerator.api.repository.CustomFormRepository;
import com.formgenerator.api.repository.CustomOptionRepository;
import com.formgenerator.api.repository.CustomRegularExpressionRepository;
import com.formgenerator.api.repository.FormFieldRepository;

@RestController
@RequestMapping(value = "/custom_form")
public class CustomFormController {

	@Autowired
	public CustomFormRepository customFormRepository;

	@Autowired
	MongoTemplate mongoTemplate;

	@Autowired
	public CustomOptionRepository customOptionRepository;

	@Autowired
	public CustomRegularExpressionRepository customRegularExpressionRepository;

	@Autowired
	public FormFieldRepository formFieldRepository;

	/** Load all Model objects on startup */
	@GetMapping(value = "/model/all", produces = "application/json")
	public ResponseEntity<List<CustomForm>> loadAllModelForms() {
		List<CustomForm> availableForms = this.customFormRepository.findAll();
		return new ResponseEntity<>(availableForms, HttpStatus.OK);
	}

	/** Load all Model objects on startup */
	@GetMapping(value = "/data/all", produces = "application/json")
	public ResponseEntity<List<CustomForm>> loadAllDataForms() {
		List<CustomForm> availableForms = this.customFormRepository.findAll();
		return new ResponseEntity<>(availableForms, HttpStatus.OK);
	}

	@GetMapping(value = "/model/{id}", produces = "application/json")
	public ResponseEntity<CustomForm> getModelById(@PathVariable(value = "id") Long id) {
		Optional<CustomForm> form = this.customFormRepository.findById(id);
		System.out.println("getModelById>" + id + ">" + form.get().getName());
		return new ResponseEntity<CustomForm>(form.get(), HttpStatus.OK);
	}

	CustomForm getModelByName(String name) {
		System.out.println("getModelByName:" + name);
		List<CustomForm> allModelForms = this.customFormRepository.findAll();
		for (CustomForm form : allModelForms) {
			if (form.getName().equals(name)) {
				System.out.println(form + "!");
				return form;
			}
		}
		return null;
	}

	/***
	 * <code>
	&#64;GetMapping(value = "/data", produces = "application/json")
	public ResponseEntity<List<String>> getAllDataForms() {
	
		// List<String> dbSet = mongoTemplate.getCollectionNames().stream().filter(s ->
		// s.contains(".")).toList();
		List<String> dbSet = new ArrayList<String>();
		for (CustomForm form : this.customFormRepository.findAll()) {
			dbSet.add(form.getName());
		}
	
		System.out.println(dbSet);
		return new ResponseEntity<>(dbSet, HttpStatus.OK);
	}
	</code>
	 */

	/**
	 * get Model of a data object loop in model object's fields find data object's
	 * value if exists on data object and copy
	 */
	@Deprecated
	List<CustomForm> convertOldDataListToNewModel(CustomForm modelObjectReference, List<CustomForm> dataListInput) {
		// TODO can simplify this by using a version number in the model/data
		// @CustomForm object

		System.out.println("Converting data to new model << " + modelObjectReference.getName());
		List<CustomForm> dataListOutput = new ArrayList<CustomForm>();
		for (CustomForm form : dataListInput) {
			if (modelObjectReference.getName() == form.getName()) {
				List<FormField> modelFields = modelObjectReference.getFields();
				for (FormField modelField : modelFields) {
					// TODO Need to avoid this N2 by moving fields vector to Map
					for (FormField dataField : form.getFields()) {
						if (modelField.getName() == dataField.getName()) {
							System.out.println("Copy field: " + modelField.getName() + "'s value "
									+ dataField.getValue() + " to object");
							/**
							 * Copy value from dataListInput object field to respective field object in a
							 * model object
							 */
							modelField.setValue(dataField.getValue());
							modelFields.add(modelField);
							break;
						}
					}
					form.setFields(modelFields);
				}
			}
			dataListOutput.add(form);
		}
		if (dataListOutput.size() > 0) {
			return dataListOutput;
		} else {
			System.err.println("convertOldDataToNewModel Failed");
		}
		return dataListInput;
	}

	/** Load all data in data table for this model by model name */
	@GetMapping(value = "/data/{name}", produces = "application/json")
	public ResponseEntity<List<CustomForm>> loadDataofSelectedModel(@PathVariable(value = "name") String name) {
		List<CustomForm> dataList = mongoTemplate.findAll(CustomForm.class, name);
		/**
		 * <code>
		CustomForm modelForm = getModelByName(name);
		if (modelForm != null) {
			dataList = convertOldDataListToNewModel(modelForm, dataList);
		}
		</code>
		 */
		System.out.println(">>" + name + ">>");
		ObjectMapper mapper = new ObjectMapper();
		try {
			System.out.println(mapper.writeValueAsString(dataList));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResponseEntity<List<CustomForm>>(dataList, HttpStatus.OK);
	}

	/*** Load data object from data table for the select item in list data table */
	@GetMapping(value = "/data/{name}/{id}", produces = "application/json")
	public ResponseEntity<CustomForm> loadSeletedObject(@PathVariable(value = "name") String name,
			@PathVariable(value = "id") Long id) {
		CustomForm data = mongoTemplate.findById(id, CustomForm.class, name);
		if (data != null) {
			data.updateToModel(getModelByName(name));
			System.out.println(">>" + name + ">" + id + ">" + data);
			return new ResponseEntity<CustomForm>(data, HttpStatus.OK);
		} else {
			return new ResponseEntity<CustomForm>(data, HttpStatus.NOT_FOUND);
		}
	}

	/** add a new data to data table of this model */
	@PostMapping(value = "/data/add", produces = "application/json")
	public ResponseEntity<JsonObject> insertData(@RequestBody CustomForm form) {
		if (form.getId() == 0) {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			form.setId(timestamp.getTime());
		}
		mongoTemplate.save(form, form.getName());
		JsonObject object = new JsonObject("{message:'created successfully'}");
		return new ResponseEntity<JsonObject>(object, HttpStatus.OK);
	}

	void updateField(FormField field) {
		field.getOptions().forEach(option -> {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			option.setId(timestamp.getTime());
			CustomOptions createdOption = this.customOptionRepository.insert(option);
			option.setId(createdOption.getId());
		});
		field.getRegularExpression().forEach(reg -> {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			reg.setId(timestamp.getTime());
			CustomRegularExpression createdRegularExpression = this.customRegularExpressionRepository.insert(reg);
			reg.setId(createdRegularExpression.getId());
		});
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		field.setId(timestamp.getTime());
		FormField createdField = this.formFieldRepository.insert(field);
		field.setId(createdField.getId());
	}

	/** Add/Update Model Form */
	@PostMapping(value = "/model/create", produces = "application/json")
	public ResponseEntity<JsonObject> saveFormTemplate(@RequestBody CustomForm form) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			System.out.println(mapper.writeValueAsString(form));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		form.getFields().forEach(field -> updateField(field));
		updateField(form.getProcess().getAction());

		if (form.getId() == 0) {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			form.setId(timestamp.getTime());
		} else {
			form.setVersion(form.getVersion() + 1);
		}
		/*
		 * Document doc = CustomForm.class.getAnnotation(Document.class);
		 * System.out.println(doc.collection());
		 */
		this.customFormRepository.save(form);

		// mongoTemplate.save(form, form.getName());

		JsonObject object = new JsonObject("{message:'created successfully'}");
		return new ResponseEntity<JsonObject>(object, HttpStatus.OK);
	}
}
