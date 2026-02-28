package com.adaptivebp.modules.organisation.service;

import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.model.DomainGroup;
import com.adaptivebp.modules.organisation.model.DomainGroupMember;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.repository.DomainGroupMemberRepository;
import com.adaptivebp.modules.organisation.repository.DomainGroupRepository;

@Service
public class DomainProvisioningService {

    @Autowired
    private DomainGroupRepository domainGroupRepository;

    @Autowired
    private DomainGroupMemberRepository domainGroupMemberRepository;

    public void provisionDefaults(Organisation organisation, String ownerDomainUserId) {
        if (organisation == null || organisation.getId() == null) {
            return;
        }
        List<DomainGroup> existing = domainGroupRepository.findByDomainId(organisation.getId());
        if (existing.isEmpty()) {
            DomainGroup adminGroup = buildGroup(organisation.getId(), "Domain Admin",
                    EnumSet.allOf(DomainPermission.class), true);
            DomainGroup contributorGroup = buildGroup(organisation.getId(), "Domain Contributor",
                    EnumSet.of(DomainPermission.DOMAIN_MANAGE_APPS, DomainPermission.DOMAIN_USE_APP), true);
            domainGroupRepository.saveAll(List.of(adminGroup, contributorGroup));
            if (ownerDomainUserId != null) {
                assignUser(adminGroup, ownerDomainUserId, ownerDomainUserId);
            }
        }
    }

    public void assignUser(DomainGroup group, String userId, String assignedBy) {
        if (group == null || userId == null) {
            return;
        }
        boolean exists = domainGroupMemberRepository.findByDomainGroupIdAndUserId(group.getId(), userId).isPresent();
        if (!exists) {
            DomainGroupMember member = new DomainGroupMember();
            member.setDomainGroupId(group.getId());
            member.setDomainId(group.getDomainId());
            member.setUserId(userId);
            member.setAssignedBy(assignedBy);
            domainGroupMemberRepository.save(member);
        }
    }

    private DomainGroup buildGroup(String domainId, String name, EnumSet<DomainPermission> permissions, boolean defaultGroup) {
        DomainGroup group = new DomainGroup();
        group.setDomainId(domainId);
        group.setName(name);
        group.setPermissions(permissions);
        group.setDefaultGroup(defaultGroup);
        return group;
    }
}
