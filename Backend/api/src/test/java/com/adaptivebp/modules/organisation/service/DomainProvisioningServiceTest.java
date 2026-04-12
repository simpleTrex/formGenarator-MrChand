package com.adaptivebp.modules.organisation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.adaptivebp.modules.organisation.model.DomainGroup;
import com.adaptivebp.modules.formbuilder.model.DomainModel;
import com.adaptivebp.modules.formbuilder.repository.DomainModelRepository;
import com.adaptivebp.modules.organisation.repository.DomainGroupMemberRepository;
import com.adaptivebp.modules.organisation.repository.DomainGroupRepository;
import com.adaptivebp.modules.organisation.model.Organisation;

@ExtendWith(MockitoExtension.class)
class DomainProvisioningServiceTest {

    @Mock
    private DomainGroupRepository domainGroupRepository;

    @Mock
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @Mock
    private DomainModelRepository domainModelRepository;

    @InjectMocks
    private DomainProvisioningService service;

    @Test
    void provisionDefaults_createsDefaultGroupsAndAssignsOwner() {
        Organisation org = new Organisation("Acme", "acme", "owner-1");
        org.setId("domain-1");

        when(domainGroupRepository.findByDomainId(org.getId())).thenReturn(Collections.emptyList());
        when(domainModelRepository.findByDomainIdAndSlug(org.getId(), "employees")).thenReturn(java.util.Optional.empty());
        when(domainModelRepository.save(any(DomainModel.class))).thenAnswer(inv -> inv.getArgument(0));

        service.provisionDefaults(org, "user-owner");

        ArgumentCaptor<List<DomainGroup>> captor = ArgumentCaptor.forClass(List.class);
        verify(domainGroupRepository).saveAll(captor.capture());
        List<DomainGroup> saved = captor.getValue();
        assertEquals(2, saved.size());
        verify(domainGroupMemberRepository).save(any());
        verify(domainModelRepository).save(any(DomainModel.class));
    }

    @Test
    void provisionDefaults_doesNothingWhenGroupsExist() {
        Organisation org = new Organisation("Acme", "acme", "owner-1");
        org.setId("domain-1");

        DomainGroup existing = new DomainGroup();
        existing.setDomainId(org.getId());
        existing.setName("Existing");

        when(domainGroupRepository.findByDomainId(org.getId())).thenReturn(List.of(existing));
        when(domainModelRepository.findByDomainIdAndSlug(org.getId(), "employees")).thenReturn(java.util.Optional.of(new DomainModel()));

        service.provisionDefaults(org, "user-owner");

        verify(domainGroupRepository, never()).saveAll(any());
        verify(domainGroupMemberRepository, never()).save(any());
        verify(domainModelRepository, never()).save(any(DomainModel.class));
    }
}
