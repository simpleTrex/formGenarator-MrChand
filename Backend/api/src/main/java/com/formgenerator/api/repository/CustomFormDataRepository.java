package com.formgenerator.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.formgenerator.api.models.CustomForm;

@Repository
public interface CustomFormDataRepository extends MongoRepository<CustomForm, Long> {

}
