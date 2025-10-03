package com.formgenerator.api.repository;

import com.formgenerator.api.models.CustomForm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomFormRepository extends MongoRepository<CustomForm, Long> {

}
