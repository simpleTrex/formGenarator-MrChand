package com.formgenerator.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.formgenerator.api.models.domain.DomainGroupMember;

public interface DomainGroupMemberRepository extends MongoRepository<DomainGroupMember, String> {

    List<DomainGroupMember> findByDomainIdAndUserId(String domainId, String userId);

    List<DomainGroupMember> findByDomainGroupId(String domainGroupId);

    Optional<DomainGroupMember> findByDomainGroupIdAndUserId(String domainGroupId, String userId);
}
