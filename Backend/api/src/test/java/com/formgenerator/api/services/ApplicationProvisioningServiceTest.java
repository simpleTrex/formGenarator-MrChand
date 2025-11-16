package com.formgenerator.api.services;

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

import com.formgenerator.api.models.app.AppGroup;
import com.formgenerator.api.models.app.Application;
import com.formgenerator.api.repository.AppGroupMemberRepository;
import com.formgenerator.api.repository.AppGroupRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationProvisioningServiceTest {

    @Mock
    private AppGroupRepository appGroupRepository;

    @Mock
    private AppGroupMemberRepository appGroupMemberRepository;

    @InjectMocks
    private ApplicationProvisioningService service;

    @Test
    void provisionDefaultGroups_createsAppGroupsAndAssignsOwner() {
        Application app = new Application();
        app.setId("app-1");

        when(appGroupRepository.findByAppId(app.getId())).thenReturn(Collections.emptyList());

        service.provisionDefaultGroups(app, "user-1");

        ArgumentCaptor<List<AppGroup>> captor = ArgumentCaptor.forClass(List.class);
        verify(appGroupRepository).saveAll(captor.capture());
        assertEquals(3, captor.getValue().size());
        verify(appGroupMemberRepository).save(any());
    }

    @Test
    void provisionDefaultGroups_skipsWhenGroupsExist() {
        Application app = new Application();
        app.setId("app-1");

        AppGroup existing = new AppGroup();
        existing.setAppId(app.getId());

        when(appGroupRepository.findByAppId(app.getId())).thenReturn(List.of(existing));

        service.provisionDefaultGroups(app, "user-1");

        verify(appGroupRepository, never()).saveAll(any());
        verify(appGroupMemberRepository, never()).save(any());
    }
}
