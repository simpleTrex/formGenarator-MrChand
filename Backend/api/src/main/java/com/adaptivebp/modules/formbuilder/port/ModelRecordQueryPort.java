package com.adaptivebp.modules.formbuilder.port;

import java.util.List;

import com.adaptivebp.modules.formbuilder.model.ModelRecord;

/**
 * Public read/query port for model records.
 * Other modules should depend on this interface instead of formbuilder services.
 */
public interface ModelRecordQueryPort {
    ModelRecord create(String modelId, String domainId, String appId,
            String instanceId, String createdBy, java.util.Map<String, Object> data);

    ModelRecord update(String recordId, java.util.Map<String, Object> newData);

    void delete(String recordId);

    List<ModelRecord> findByModel(String modelId, String domainId);
}
