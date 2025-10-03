package com.formgenerator.api.repository;

import com.formgenerator.api.models.FormField;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FormFieldRepository extends MongoRepository<FormField, Long> {
}
