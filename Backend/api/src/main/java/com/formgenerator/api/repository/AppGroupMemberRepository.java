package com.formgenerator.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.formgenerator.api.models.app.AppGroupMember;

public interface AppGroupMemberRepository extends MongoRepository<AppGroupMember, String> {

    List<AppGroupMember> findByAppIdAndUserId(String appId, String userId);

    List<AppGroupMember> findByGroupId(String groupId);

    Optional<AppGroupMember> findByGroupIdAndUserId(String groupId, String userId);
}
