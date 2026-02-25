package com.adaptivebp.modules.formbuilder.controller;

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
import com.adaptivebp.modules.formbuilder.model.legacy.CustomForm;
import com.adaptivebp.modules.formbuilder.model.legacy.CustomOptions;
import com.adaptivebp.modules.formbuilder.model.legacy.CustomRegularExpression;
import com.adaptivebp.modules.formbuilder.model.legacy.FormField;
import com.adaptivebp.modules.formbuilder.repository.CustomFormRepository;
import com.adaptivebp.modules.formbuilder.repository.CustomOptionRepository;
import com.adaptivebp.modules.formbuilder.repository.CustomRegularExpressionRepository;
import com.adaptivebp.modules.formbuilder.repository.FormFieldRepository;

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
        return new ResponseEntity<>(form.get(), HttpStatus.OK);
    }

    CustomForm getModelByName(String name) {
        List<CustomForm> allModelForms = this.customFormRepository.findAll();
        for (CustomForm form : allModelForms) {
            if (form.getName().equals(name)) {
                return form;
            }
        }
        return null;
    }

    @Deprecated
    List<CustomForm> convertOldDataListToNewModel(CustomForm modelObjectReference, List<CustomForm> dataListInput) {
        List<CustomForm> dataListOutput = new ArrayList<>();
        for (CustomForm form : dataListInput) {
            if (modelObjectReference.getName() == form.getName()) {
                List<FormField> modelFields = modelObjectReference.getFields();
                for (FormField modelField : modelFields) {
                    for (FormField dataField : form.getFields()) {
                        if (modelField.getName() == dataField.getName()) {
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
        if (!dataListOutput.isEmpty()) {
            return dataListOutput;
        }
        return dataListInput;
    }

    /** Load all data in data table for this model by model name */
    @GetMapping(value = "/data/{name}", produces = "application/json")
    public ResponseEntity<List<CustomForm>> loadDataofSelectedModel(@PathVariable(value = "name") String name) {
        List<CustomForm> dataList = mongoTemplate.findAll(CustomForm.class, name);
        return new ResponseEntity<>(dataList, HttpStatus.OK);
    }

    /** Load data object from data table for the selected item */
    @GetMapping(value = "/data/{name}/{id}", produces = "application/json")
    public ResponseEntity<CustomForm> loadSeletedObject(@PathVariable(value = "name") String name,
            @PathVariable(value = "id") Long id) {
        CustomForm data = mongoTemplate.findById(id, CustomForm.class, name);
        if (data != null) {
            data.updateToModel(getModelByName(name));
            return new ResponseEntity<>(data, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(data, HttpStatus.NOT_FOUND);
        }
    }

    /** Add a new data to data table of this model */
    @PostMapping(value = "/data/add", produces = "application/json")
    public ResponseEntity<JsonObject> insertData(@RequestBody CustomForm form) {
        if (form.getId() == 0) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            form.setId(timestamp.getTime());
        }
        mongoTemplate.save(form, form.getName());
        JsonObject object = new JsonObject("{message:'created successfully'}");
        return new ResponseEntity<>(object, HttpStatus.OK);
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
        form.getFields().forEach(field -> updateField(field));
        updateField(form.getProcess().getAction());

        if (form.getId() == 0) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            form.setId(timestamp.getTime());
        } else {
            form.setVersion(form.getVersion() + 1);
        }
        this.customFormRepository.save(form);

        JsonObject object = new JsonObject("{message:'created successfully'}");
        return new ResponseEntity<>(object, HttpStatus.OK);
    }
}
