package com.adaptivebp.modules.formbuilder.controller;

import java.util.List;
import java.util.Optional;

import org.bson.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adaptivebp.modules.formbuilder.model.legacy.CustomForm;
import com.adaptivebp.modules.formbuilder.service.CustomFormService;

@RestController
@RequestMapping(value = "/custom_form")
public class CustomFormController {

    @Autowired
    private CustomFormService customFormService;

    @GetMapping(value = "/model/all", produces = "application/json")
    public ResponseEntity<List<CustomForm>> loadAllModelForms() {
        return new ResponseEntity<>(customFormService.findAllForms(), HttpStatus.OK);
    }

    @GetMapping(value = "/data/all", produces = "application/json")
    public ResponseEntity<List<CustomForm>> loadAllDataForms() {
        return new ResponseEntity<>(customFormService.findAllForms(), HttpStatus.OK);
    }

    @GetMapping(value = "/model/{id}", produces = "application/json")
    public ResponseEntity<CustomForm> getModelById(@PathVariable(value = "id") Long id) {
        Optional<CustomForm> form = customFormService.findFormById(id);
        return new ResponseEntity<>(form.get(), HttpStatus.OK);
    }

    @GetMapping(value = "/data/{name}", produces = "application/json")
    public ResponseEntity<List<CustomForm>> loadDataofSelectedModel(@PathVariable(value = "name") String name) {
        return new ResponseEntity<>(customFormService.findDataForModel(name), HttpStatus.OK);
    }

    @GetMapping(value = "/data/{name}/{id}", produces = "application/json")
    public ResponseEntity<CustomForm> loadSeletedObject(@PathVariable(value = "name") String name,
            @PathVariable(value = "id") Long id) {
        CustomForm data = customFormService.findDataObject(name, id);
        if (data != null) {
            return new ResponseEntity<>(data, HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    @PostMapping(value = "/data/add", produces = "application/json")
    public ResponseEntity<JsonObject> insertData(@RequestBody CustomForm form) {
        customFormService.insertData(form);
        return new ResponseEntity<>(new JsonObject("{message:'created successfully'}"), HttpStatus.OK);
    }

    @PostMapping(value = "/model/create", produces = "application/json")
    public ResponseEntity<JsonObject> saveFormTemplate(@RequestBody CustomForm form) {
        customFormService.saveFormTemplate(form);
        return new ResponseEntity<>(new JsonObject("{message:'created successfully'}"), HttpStatus.OK);
    }
}
