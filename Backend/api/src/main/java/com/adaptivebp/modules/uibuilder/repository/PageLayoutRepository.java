package com.adaptivebp.modules.uibuilder.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.uibuilder.model.PageLayout;

public interface PageLayoutRepository extends MongoRepository<PageLayout, String> {
    Optional<PageLayout> findByPageIdAndAppId(String pageId, String appId);
}
