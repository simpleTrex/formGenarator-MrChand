package com.adaptivebp.modules.uibuilder.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.uibuilder.model.AppPage;

public interface AppPageRepository extends MongoRepository<AppPage, String> {
    List<AppPage> findByAppIdOrderByOrderAsc(String appId);

    List<AppPage> findByAppIdAndDomainSlug(String appId, String domainSlug);

    boolean existsByAppIdAndSlug(String appId, String slug);
}
