package com.adaptivebp.modules.formbuilder.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.adaptivebp.modules.formbuilder.model.legacy.CustomOptions;

@Repository
public interface CustomOptionRepository extends MongoRepository<CustomOptions, Long> {
}
