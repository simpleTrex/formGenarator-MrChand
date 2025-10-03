package com.formgenerator.api.repository;

import com.formgenerator.api.models.CustomOptions;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomOptionRepository extends MongoRepository<CustomOptions, Long> {
}
