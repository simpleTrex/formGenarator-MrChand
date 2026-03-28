package com.adaptivebp.modules.workflow.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.formbuilder.model.DomainFieldType;
import com.adaptivebp.modules.formbuilder.model.DomainModel;
import com.adaptivebp.modules.formbuilder.model.DomainModelField;
import com.adaptivebp.modules.formbuilder.repository.DomainModelRepository;
import com.adaptivebp.modules.workflow.exception.InvalidFormDataException;
import com.adaptivebp.modules.workflow.exception.ModelNotFoundException;

@Service("workflowModelRecordService")
public class ModelRecordService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DomainModelRepository domainModelRepository;

    public String getCollectionName(DomainModel model) {
        return "domain_" + model.getDomainId() + "_model_" + model.getSlug() + "_records";
    }

    public DomainModel getModel(String modelId) {
        return domainModelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException(modelId));
    }

    public String createRecord(String modelId, Map<String, Object> data, String createdBy) {
        DomainModel model = getModel(modelId);
        Map<String, Object> payload = data != null ? new HashMap<>(data) : new HashMap<>();

        Map<String, String> errors = validateDataAgainstModel(model, payload, true, null);
        if (!errors.isEmpty()) {
            throw new InvalidFormDataException("Model validation failed", errors);
        }

        payload.put("_createdBy", createdBy);
        payload.put("_createdAt", Instant.now());
        payload.put("_updatedAt", Instant.now());

        Document doc = new Document(payload);
        mongoTemplate.insert(doc, getCollectionName(model));
        Object id = doc.get("_id");
        if (id instanceof ObjectId objectId) {
            return objectId.toHexString();
        }
        return String.valueOf(id);
    }

    public void updateRecord(String modelId, String recordId, Map<String, Object> data) {
        DomainModel model = getModel(modelId);
        Map<String, Object> payload = data != null ? new HashMap<>(data) : new HashMap<>();

        Map<String, String> errors = validateDataAgainstModel(model, payload, false, recordId);
        if (!errors.isEmpty()) {
            throw new InvalidFormDataException("Model validation failed", errors);
        }

        payload.put("_updatedAt", Instant.now());

        Query query = byId(recordId);
        Update update = new Update();
        payload.forEach(update::set);
        mongoTemplate.updateFirst(query, update, getCollectionName(model));
    }

    public Map<String, Object> getRecord(String modelId, String recordId) {
        DomainModel model = getModel(modelId);
        Document doc = mongoTemplate.findOne(byId(recordId), Document.class, getCollectionName(model));
        return toMap(doc);
    }

    public List<Map<String, Object>> queryByField(
            String modelId,
            String fieldKey,
            Object value,
            List<String> fieldsToFetch) {
        DomainModel model = getModel(modelId);

        Query query = new Query(Criteria.where(fieldKey).is(value));
        if (fieldsToFetch != null && !fieldsToFetch.isEmpty()) {
            fieldsToFetch.forEach(f -> query.fields().include(f));
            query.fields().include("_id");
        }

        List<Document> docs = mongoTemplate.find(query, Document.class, getCollectionName(model));
        List<Map<String, Object>> response = new ArrayList<>();
        for (Document doc : docs) {
            response.add(toMap(doc));
        }
        return response;
    }

    public Map<String, String> validateData(String modelId, Map<String, Object> data, boolean requireModelRequiredFields) {
        DomainModel model = getModel(modelId);
        return validateDataAgainstModel(model, data != null ? data : Map.of(), requireModelRequiredFields, null);
    }

    public Map<String, String> validateData(String modelId,
            Map<String, Object> data,
            boolean requireModelRequiredFields,
            String existingRecordId) {
        DomainModel model = getModel(modelId);
        return validateDataAgainstModel(model, data != null ? data : Map.of(), requireModelRequiredFields, existingRecordId);
    }

    private Map<String, String> validateDataAgainstModel(DomainModel model,
            Map<String, Object> data,
            boolean requireModelRequiredFields,
            String existingRecordId) {
        Map<String, String> errors = new LinkedHashMap<>();
        String collectionName = getCollectionName(model);

        for (DomainModelField field : model.getFields()) {
            Object value = data.get(field.getKey());

            if (requireModelRequiredFields && field.isRequired() && isEmpty(value)) {
                errors.put(field.getKey(), "Field is required");
                continue;
            }

            if (value == null) {
                continue;
            }

            if (!matchesType(field.getType(), value)) {
                errors.put(field.getKey(), "Invalid type for " + field.getType());
                continue;
            }

            if (field.isUnique()) {
                Query uniqueQuery = new Query(Criteria.where(field.getKey()).is(value));
                if (existingRecordId != null) {
                    uniqueQuery.addCriteria(Criteria.where("_id").ne(parseMongoId(existingRecordId)));
                }
                boolean exists = mongoTemplate.exists(uniqueQuery, collectionName);
                if (exists) {
                    errors.put(field.getKey(), "Value must be unique");
                }
            }
        }

        return errors;
    }

    private Query byId(String recordId) {
        return new Query(Criteria.where("_id").is(parseMongoId(recordId)));
    }

    private Object parseMongoId(String value) {
        if (value != null && ObjectId.isValid(value)) {
            return new ObjectId(value);
        }
        return value;
    }

    private boolean matchesType(DomainFieldType type, Object value) {
        return switch (type) {
            case STRING -> value instanceof String;
            case NUMBER -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case DATE, DATETIME -> value instanceof Date
                    || value instanceof Instant
                    || value instanceof LocalDate
                    || value instanceof LocalDateTime
                    || value instanceof String;
            case REFERENCE, EMPLOYEE_REFERENCE -> value instanceof String;
            case OBJECT -> value instanceof Map;
            case ARRAY -> value instanceof List;
        };
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String string) {
            return string.trim().isEmpty();
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        return false;
    }

    private Map<String, Object> toMap(Document doc) {
        if (doc == null) {
            return null;
        }
        Map<String, Object> copy = new HashMap<>(doc);
        Object id = copy.get("_id");
        if (id instanceof ObjectId objectId) {
            copy.put("_id", objectId.toHexString());
        }
        return copy;
    }

    public boolean isAccessibleToApp(String modelId, String appId) {
        DomainModel model = getModel(modelId);
        return model.isAccessibleByAppId(appId);
    }

    public boolean matchesModelDomain(String modelId, String domainId) {
        DomainModel model = getModel(modelId);
        return Objects.equals(model.getDomainId(), domainId);
    }
}
