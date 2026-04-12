package com.adaptivebp.modules.formbuilder.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.adaptivebp.modules.formbuilder.model.ModelTemplate;

public interface ModelTemplateRepository extends MongoRepository<ModelTemplate, String> {
    List<ModelTemplate> findByCategory(String category);
}