package com.adaptivebp.modules.formbuilder.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.formbuilder.model.legacy.CustomForm;
import com.adaptivebp.modules.formbuilder.model.legacy.CustomOptions;
import com.adaptivebp.modules.formbuilder.model.legacy.CustomRegularExpression;
import com.adaptivebp.modules.formbuilder.model.legacy.FormField;
import com.adaptivebp.modules.formbuilder.repository.CustomFormRepository;
import com.adaptivebp.modules.formbuilder.repository.CustomOptionRepository;
import com.adaptivebp.modules.formbuilder.repository.CustomRegularExpressionRepository;
import com.adaptivebp.modules.formbuilder.repository.FormFieldRepository;

/**
 * Service layer for the formbuilder module.
 * Extracted from CustomFormController so the controller contains no
 * repository references or business logic — only HTTP mapping.
 */
@Service
public class CustomFormService {

    @Autowired
    private CustomFormRepository customFormRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomOptionRepository customOptionRepository;

    @Autowired
    private CustomRegularExpressionRepository customRegularExpressionRepository;

    @Autowired
    private FormFieldRepository formFieldRepository;

    public List<CustomForm> findAllForms() {
        return customFormRepository.findAll();
    }

    public Optional<CustomForm> findFormById(Long id) {
        return customFormRepository.findById(id);
    }

    public CustomForm findFormByName(String name) {
        return customFormRepository.findAll().stream()
                .filter(f -> name.equals(f.getName()))
                .findFirst()
                .orElse(null);
    }

    public List<CustomForm> findDataForModel(String modelName) {
        return mongoTemplate.findAll(CustomForm.class, modelName);
    }

    public CustomForm findDataObject(String modelName, Long id) {
        CustomForm data = mongoTemplate.findById(id, CustomForm.class, modelName);
        if (data != null) {
            data.updateToModel(findFormByName(modelName));
        }
        return data;
    }

    public void insertData(CustomForm form) {
        if (form.getId() == 0) {
            form.setId(new Timestamp(System.currentTimeMillis()).getTime());
        }
        mongoTemplate.save(form, form.getName());
    }

    /**
     * Persists a field and all its options / regular expressions, assigning
     * timestamp-based IDs for any entities that do not yet have one.
     */
    public void persistField(FormField field) {
        field.getOptions().forEach(option -> {
            option.setId(new Timestamp(System.currentTimeMillis()).getTime());
            CustomOptions saved = customOptionRepository.insert(option);
            option.setId(saved.getId());
        });
        field.getRegularExpression().forEach(reg -> {
            reg.setId(new Timestamp(System.currentTimeMillis()).getTime());
            CustomRegularExpression saved = customRegularExpressionRepository.insert(reg);
            reg.setId(saved.getId());
        });
        field.setId(new Timestamp(System.currentTimeMillis()).getTime());
        FormField saved = formFieldRepository.insert(field);
        field.setId(saved.getId());
    }

    public void saveFormTemplate(CustomForm form) {
        form.getFields().forEach(this::persistField);
        persistField(form.getProcess().getAction());
        if (form.getId() == 0) {
            form.setId(new Timestamp(System.currentTimeMillis()).getTime());
        } else {
            form.setVersion(form.getVersion() + 1);
        }
        customFormRepository.save(form);
    }
}
