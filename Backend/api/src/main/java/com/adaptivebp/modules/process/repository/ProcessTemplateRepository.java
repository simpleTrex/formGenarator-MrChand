package com.adaptivebp.modules.process.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.adaptivebp.modules.process.model.ProcessTemplate;

public interface ProcessTemplateRepository extends MongoRepository<ProcessTemplate, String> {
    List<ProcessTemplate> findByCategory(String category);
}