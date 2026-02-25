package com.adaptivebp.modules.organisation.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.adaptivebp.modules.organisation.model.DomainGroupMember;

public interface DomainGroupMemberRepository extends MongoRepository<DomainGroupMember, String> {
    List<DomainGroupMember> findByDomainIdAndUserId(String domainId, String userId);
    List<DomainGroupMember> findByDomainGroupId(String domainGroupId);
    Optional<DomainGroupMember> findByDomainGroupIdAndUserId(String domainGroupId, String userId);
}
