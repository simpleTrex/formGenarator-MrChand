package com.formgenerator.api.repository;

import com.formgenerator.api.models.CustomRegularExpression;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomRegularExpressionRepository extends MongoRepository<CustomRegularExpression, Long> {
}
