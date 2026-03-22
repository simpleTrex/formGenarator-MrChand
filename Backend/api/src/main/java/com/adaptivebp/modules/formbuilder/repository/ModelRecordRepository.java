package com.adaptivebp.modules.formbuilder.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.adaptivebp.modules.formbuilder.model.ModelRecord;

public interface ModelRecordRepository extends MongoRepository<ModelRecord, String> {

    List<ModelRecord> findByModelId(String modelId);

    List<ModelRecord> findByModelIdAndDomainId(String modelId, String domainId);

    List<ModelRecord> findByInstanceId(String instanceId);

    void deleteByModelId(String modelId);
}
