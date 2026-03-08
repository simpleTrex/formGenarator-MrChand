package com.adaptivebp.modules.uibuilder.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.uibuilder.model.DomainRecord;

public interface DomainRecordRepository extends MongoRepository<DomainRecord, String> {
    List<DomainRecord> findByModelIdAndDomainSlug(String modelId, String domainSlug);

    List<DomainRecord> findByModelSlugAndDomainSlug(String modelSlug, String domainSlug);

    long countByModelIdAndDomainSlug(String modelId, String domainSlug);
}
