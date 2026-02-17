package com.formgenerator.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.formgenerator.api.dto.model.CreateDomainModelRequest;
import com.formgenerator.api.models.app.Application;
import com.formgenerator.api.models.domain.model.DomainModel;
import com.formgenerator.api.permissions.AppPermission;
import com.formgenerator.api.repository.ApplicationRepository;
import com.formgenerator.api.repository.DomainModelRepository;
import com.formgenerator.api.repository.DomainRepository;
import com.formgenerator.api.services.PermissionService;
import com.formgenerator.platform.auth.Domain;

@ExtendWith(MockitoExtension.class)
class DomainModelControllerTest {

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private DomainModelRepository domainModelRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private DomainModelController controller;

    @Test
    void list_shouldReturn403_withoutAppWritePermission() {
        Domain domain = new Domain("acme", "acme", "owner");
        domain.setId("d1");

        Application app = new Application();
        app.setId("a1");
        app.setDomainId("d1");
        app.setSlug("app1");

        when(domainRepository.findBySlug("acme")).thenReturn(Optional.of(domain));
        when(applicationRepository.findByDomainIdAndSlug("d1", "app1")).thenReturn(Optional.of(app));
        when(permissionService.hasAppPermission("a1", AppPermission.APP_WRITE)).thenReturn(false);

        ResponseEntity<?> res = controller.list("acme", "app1");
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void create_shouldReturn403_withoutAppWritePermission() {
        Domain domain = new Domain("acme", "acme", "owner");
        domain.setId("d1");

        Application app = new Application();
        app.setId("a1");
        app.setDomainId("d1");
        app.setSlug("app1");

        when(domainRepository.findBySlug("acme")).thenReturn(Optional.of(domain));
        when(applicationRepository.findByDomainIdAndSlug("d1", "app1")).thenReturn(Optional.of(app));
        when(permissionService.hasAppPermission("a1", AppPermission.APP_WRITE)).thenReturn(false);

        CreateDomainModelRequest req = new CreateDomainModelRequest();
        req.setName("Product");
        req.setSlug("product");

        ResponseEntity<?> res = controller.create("acme", "app1", req);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }

    @Test
    void create_shouldSaveModel() {
        Domain domain = new Domain("acme", "acme", "owner");
        domain.setId("d1");

        Application app = new Application();
        app.setId("a1");
        app.setDomainId("d1");
        app.setSlug("app1");

        when(domainRepository.findBySlug("acme")).thenReturn(Optional.of(domain));
        when(applicationRepository.findByDomainIdAndSlug("d1", "app1")).thenReturn(Optional.of(app));
        when(permissionService.hasAppPermission("a1", AppPermission.APP_WRITE)).thenReturn(true);
        when(domainModelRepository.existsByDomainIdAndSlug("d1", "product")).thenReturn(false);

        DomainModel saved = new DomainModel();
        saved.setId("m1");
        saved.setDomainId("d1");
        saved.setSlug("product");
        saved.setName("Product");

        when(domainModelRepository.save(any(DomainModel.class))).thenReturn(saved);

        CreateDomainModelRequest req = new CreateDomainModelRequest();
        req.setName("Product");
        req.setSlug("product");

        ResponseEntity<?> res = controller.create("acme", "app1", req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }
}
