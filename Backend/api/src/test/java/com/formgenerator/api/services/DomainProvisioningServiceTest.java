package com.formgenerator.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.formgenerator.api.models.domain.DomainGroup;
import com.formgenerator.api.repository.DomainGroupMemberRepository;
import com.formgenerator.api.repository.DomainGroupRepository;
import com.formgenerator.platform.auth.Domain;

@ExtendWith(MockitoExtension.class)
class DomainProvisioningServiceTest {

    @Mock
    private DomainGroupRepository domainGroupRepository;

    @Mock
    private DomainGroupMemberRepository domainGroupMemberRepository;

    @InjectMocks
    private DomainProvisioningService service;

    @Test
    void provisionDefaults_createsDefaultGroupsAndAssignsOwner() {
        Domain domain = new Domain("Acme", "acme", "owner-1");
        domain.setId("domain-1");

        when(domainGroupRepository.findByDomainId(domain.getId())).thenReturn(Collections.emptyList());

        service.provisionDefaults(domain, "user-owner");

        ArgumentCaptor<List<DomainGroup>> captor = ArgumentCaptor.forClass(List.class);
        verify(domainGroupRepository).saveAll(captor.capture());
        List<DomainGroup> saved = captor.getValue();
        assertEquals(3, saved.size());
        verify(domainGroupMemberRepository).save(any());
    }

    @Test
    void provisionDefaults_doesNothingWhenGroupsExist() {
        Domain domain = new Domain("Acme", "acme", "owner-1");
        domain.setId("domain-1");

        DomainGroup existing = new DomainGroup();
        existing.setDomainId(domain.getId());
        existing.setName("Existing");

        when(domainGroupRepository.findByDomainId(domain.getId())).thenReturn(List.of(existing));

        service.provisionDefaults(domain, "user-owner");

        verify(domainGroupRepository, never()).saveAll(any());
        verify(domainGroupMemberRepository, never()).save(any());
    }
}
