package com.adaptivebp.modules.appmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.appmanagement.model.AppGroupMember;

public interface AppGroupMemberRepository extends MongoRepository<AppGroupMember, String> {
    List<AppGroupMember> findByAppIdAndUserId(String appId, String userId);
    List<AppGroupMember> findByGroupId(String groupId);
    Optional<AppGroupMember> findByGroupIdAndUserId(String groupId, String userId);
}
