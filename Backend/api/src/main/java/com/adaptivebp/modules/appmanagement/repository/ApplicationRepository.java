package com.adaptivebp.modules.appmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.appmanagement.model.Application;

public interface ApplicationRepository extends MongoRepository<Application, String> {
    Optional<Application> findByDomainIdAndSlug(String domainId, String slug);
    List<Application> findByDomainId(String domainId);
    boolean existsByDomainIdAndSlug(String domainId, String slug);
}
