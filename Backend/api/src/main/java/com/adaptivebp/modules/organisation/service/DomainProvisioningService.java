package com.adaptivebp.modules.organisation.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.adaptivebp.modules.formbuilder.model.DomainFieldType;
import com.adaptivebp.modules.formbuilder.model.DomainModel;
import com.adaptivebp.modules.formbuilder.model.DomainModelField;
import com.adaptivebp.modules.formbuilder.model.ModelScope;
import com.adaptivebp.modules.formbuilder.repository.DomainModelRepository;
import com.adaptivebp.modules.organisation.model.Organisation;
import com.adaptivebp.modules.organisation.model.DomainGroup;
import com.adaptivebp.modules.organisation.model.DomainGroupMember;
import com.adaptivebp.modules.organisation.model.enums.DomainGroupType;
import com.adaptivebp.modules.organisation.permission.DomainPermission;
import com.adaptivebp.modules.organisation.repository.DomainGroupMemberRepository;
import com.adaptivebp.modules.organisation.repository.DomainGroupRepository;

@Service
public class DomainProvisioningService {

    @Autowired
    private DomainGroupRepository domainGroupRepository;

    @Autowired
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @Autowired
    private DomainModelRepository domainModelRepository;

    public void provisionDefaults(Organisation organisation, String ownerDomainUserId) {
        if (organisation == null || organisation.getId() == null) {
            return;
        }

        // Create default groups
        List<DomainGroup> existing = domainGroupRepository.findByDomainId(organisation.getId());
        if (existing.isEmpty()) {
            DomainGroup adminGroup = buildGroup(organisation.getId(), "Domain Admin",
                    EnumSet.allOf(DomainPermission.class), true);
            DomainGroup contributorGroup = buildGroup(organisation.getId(), "Domain Contributor",
                    EnumSet.of(DomainPermission.DOMAIN_MANAGE_APPS, DomainPermission.DOMAIN_USE_APP,
                            DomainPermission.DOMAIN_MANAGE_WORKFLOWS), true);
            domainGroupRepository.saveAll(List.of(adminGroup, contributorGroup));
            if (ownerDomainUserId != null) {
                assignUser(adminGroup, ownerDomainUserId, ownerDomainUserId);
            }
        }

        // Create Employee model (system-wide, shared across all apps)
        boolean employeeModelExists = domainModelRepository
                .findByDomainIdAndSlug(organisation.getId(), "employees")
                .isPresent();
        if (!employeeModelExists) {
            DomainModel employeeModel = buildEmployeeModel(organisation.getId());
            domainModelRepository.save(employeeModel);
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
        group.setGroupType(DomainGroupType.ACCESS);
        group.setDefaultGroup(defaultGroup);
        return group;
    }

    /**
     * Creates the built-in Employee model for the domain.
     * This model is system-wide and shared across all applications.
     */
    private DomainModel buildEmployeeModel(String domainId) {
        DomainModel model = new DomainModel();
        model.setDomainId(domainId);
        model.setSlug("employees");
        model.setName("Employees");
        model.setDescription("Built-in employee directory for the organization");
        model.setScope(ModelScope.SYSTEM_WIDE);
        model.setSystemModel(true);
        model.setSharedWithAllApps(true);

        List<DomainModelField> fields = new ArrayList<>();
        fields.add(createField("employeeId", "Employee ID", DomainFieldType.STRING, true));
        fields.add(createField("firstName", "First Name", DomainFieldType.STRING, true));
        fields.add(createField("lastName", "Last Name", DomainFieldType.STRING, true));
        fields.add(createField("email", "Email Address", DomainFieldType.STRING, true));
        fields.add(createField("department", "Department", DomainFieldType.STRING, false));
        fields.add(createField("position", "Job Title", DomainFieldType.STRING, false));
        fields.add(createField("hireDate", "Hire Date", DomainFieldType.DATE, false));
        fields.add(createField("status", "Employment Status", DomainFieldType.STRING, true));

        model.setFields(fields);
        return model;
    }

    private DomainModelField createField(String name, String displayName, DomainFieldType type, boolean required) {
        DomainModelField field = new DomainModelField();
        field.setKey(name);
        // Store displayName in config since DomainModelField doesn't have displayName
        field.getConfig().put("displayName", displayName);
        field.setType(type);
        field.setRequired(required);
        return field;
    }
}
