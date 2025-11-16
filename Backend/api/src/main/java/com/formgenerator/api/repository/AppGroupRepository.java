package com.formgenerator.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.formgenerator.api.models.app.AppGroup;

public interface AppGroupRepository extends MongoRepository<AppGroup, String> {

    List<AppGroup> findByAppId(String appId);

    Optional<AppGroup> findByAppIdAndName(String appId, String name);
}
