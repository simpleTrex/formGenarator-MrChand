package com.adaptivebp.modules.formbuilder.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.formbuilder.model.DomainFieldType;
import com.adaptivebp.modules.formbuilder.model.DomainModel;
import com.adaptivebp.modules.formbuilder.model.DomainModelField;
import com.adaptivebp.modules.formbuilder.model.ModelRecord;
import com.adaptivebp.modules.formbuilder.port.ModelRecordQueryPort;
import com.adaptivebp.modules.formbuilder.repository.DomainModelRepository;
import com.adaptivebp.modules.formbuilder.repository.ModelRecordRepository;

@Service
public class ModelRecordService implements ModelRecordQueryPort {

    @Autowired private ModelRecordRepository recordRepository;
    @Autowired private DomainModelRepository modelRepository;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public ModelRecord save(ModelRecord record) {
        validateRecord(record);
        if (record.getId() == null) {
            record.setCreatedAt(Instant.now());
        }
        record.setUpdatedAt(Instant.now());
        return recordRepository.save(record);
    }

    public Optional<ModelRecord> findById(String recordId) {
        return recordRepository.findById(recordId);
    }

    public List<ModelRecord> findAllByModelId(String modelId) {
        return recordRepository.findByModelId(modelId);
    }

    public void deleteById(String recordId) {
        recordRepository.deleteById(recordId);
    }

    /**
     * Validates a ModelRecord before saving.
     * Checks EMPLOYEE_REFERENCE fields to ensure they point to valid employee records.
     */
    private void validateRecord(ModelRecord record) {
        if (record.getModelId() == null || record.getData() == null) {
            return;
        }

        DomainModel model = modelRepository.findById(record.getModelId()).orElse(null);
        if (model == null || model.getFields() == null) {
            return;
        }

        for (DomainModelField field : model.getFields()) {
            if (field.getType() == DomainFieldType.EMPLOYEE_REFERENCE) {
                Object value = record.getData().get(field.getKey());
                if (value != null) {
                    String employeeId = value.toString();
                    boolean employeeExists = recordRepository.findById(employeeId).isPresent();
                    if (!employeeExists) {
                        throw new IllegalArgumentException("Employee reference not found: " + employeeId);
                    }
                }
            }
        }
    }

    public ModelRecord create(String modelId, String domainId, String appId,
            String instanceId, String createdBy, Map<String, Object> data) {
        ModelRecord record = new ModelRecord();
        record.setModelId(modelId);
        record.setDomainId(domainId);
        record.setAppId(appId);
        record.setInstanceId(instanceId);
        record.setCreatedBy(createdBy);
        record.setData(data != null ? new HashMap<>(data) : new HashMap<>());
        record.setCreatedAt(Instant.now());
        record.setUpdatedAt(Instant.now());
        return recordRepository.save(record);
    }

    public ModelRecord update(String recordId, Map<String, Object> newData) {
        ModelRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found: " + recordId));
        if (newData != null) {
            record.getData().putAll(newData);
        }
        record.setUpdatedAt(Instant.now());
        return recordRepository.save(record);
    }

    public void delete(String recordId) {
        recordRepository.deleteById(recordId);
    }

    @Override
    public List<ModelRecord> findByModel(String modelId, String domainId) {
        return recordRepository.findByModelIdAndDomainId(modelId, domainId);
    }

    // ── Schema migration ──────────────────────────────────────────────────────

    /**
     * Called when a DomainModel's fields are updated.
     * For each existing record:
     *   - New fields added to schema → add null placeholder so the key exists
     *   - Removed fields → kept (non-destructive; data is preserved)
     *   - Type changes → no automatic conversion (value stays as-is)
     */
    public void migrateRecordsForModel(String modelId, List<DomainModelField> newFields) {
        List<ModelRecord> records = recordRepository.findByModelId(modelId);
        if (records.isEmpty()) return;

        for (ModelRecord record : records) {
            boolean changed = false;
            for (DomainModelField field : newFields) {
                if (!record.getData().containsKey(field.getKey())) {
                    record.getData().put(field.getKey(), null);
                    changed = true;
                }
            }
            if (changed) {
                record.setUpdatedAt(Instant.now());
                recordRepository.save(record);
            }
        }
    }
}
