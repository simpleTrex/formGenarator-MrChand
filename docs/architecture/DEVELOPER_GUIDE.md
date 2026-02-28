# AdaptiveBP — Developer Rules & Patterns Guide

> **Purpose:** Practical rules and code examples for maintaining the modular monolith architecture.  
> **Companion to:** [PROJECT_ARCHITECTURE.md](./PROJECT_ARCHITECTURE.md) (detailed structure reference)  
> **Last updated:** 2026-02-27

---

## Table of Contents

1. [Golden Rules](#1-golden-rules)
2. [Backend Rules & Patterns](#2-backend-rules--patterns)
3. [Frontend Rules & Patterns](#3-frontend-rules--patterns)
4. [Walkthrough: Adding a New Feature End-to-End](#4-walkthrough-adding-a-new-feature-end-to-end)
5. [Anti-Patterns (What NOT to Do)](#5-anti-patterns-what-not-to-do)
6. [Quick Reference Cheat Sheet](#6-quick-reference-cheat-sheet)

---

## 1. Golden Rules

These 7 rules apply everywhere. Break them and the architecture degrades.

### Rule 1: Modules Own Their Data

Each module owns its **models, repositories, and database collections**. No module should directly read or write another module's collections.

```
✅ organisation module → OrganisationRepository → "organisations" collection
✅ appmanagement module → ApplicationRepository → "applications" collection
❌ appmanagement module → OrganisationRepository (direct access to org's data)
```

### Rule 2: Cross-Module Communication Uses Defined Interfaces

When module A needs data from module B, use one of these patterns:
- **Port/Interface** — Module B exposes a port interface (e.g., `OrganisationLookupPort`), module A injects it
- **Service injection** — Module A injects module B's service (e.g., `PermissionService`)

```java
// ✅ CORRECT — appmanagement uses a port to look up organisations
@Autowired
private OrganisationLookupPort organisationLookupPort;  // defined by organisation module

// ❌ WRONG — appmanagement directly accesses organisation's repository
@Autowired
private OrganisationRepository organisationRepository;  // belongs to organisation module
```

### Rule 3: No Circular Dependencies

Dependencies flow in one direction:

```
shared ← identity ← organisation ← appmanagement ← formbuilder
```

- `shared` depends on **nothing** (except legacy coupling to identity models)
- `identity` depends on `shared` only
- `organisation` depends on `shared` + may use `identity`
- `appmanagement` depends on `shared` + `organisation`
- `formbuilder` depends on `shared` + `organisation` + `appmanagement`

**A module must NEVER depend on a module to its right.**

### Rule 4: DTOs at the Boundary, Models Inside

Controllers receive **Request DTOs** and return **Response DTOs**. Models (`@Document` entities) never leak to the API consumer.

```java
// ✅ CORRECT — Controller returns a DTO
return ResponseEntity.ok(new OrganisationResponse(savedOrg));

// ❌ WRONG — Controller returns the raw model
return ResponseEntity.ok(savedOrg);
```

> **Exception:** Some controllers currently return raw models (e.g., `ApplicationController`). This is technical debt to fix.

### Rule 5: Business Logic Lives in Services, Not Controllers

Controllers handle: routing, input validation, authorization, and response formatting.  
Services handle: business rules, provisioning, data transformations.

```java
// ✅ CORRECT — Controller delegates to service
Application saved = applicationRepository.save(application);
applicationProvisioningService.provisionDefaultGroups(saved, ownerUserId);

// ❌ WRONG — Controller creates groups directly
AppGroup admin = new AppGroup();
admin.setName("App Admin");
appGroupRepository.save(admin);  // This belongs in a service!
```

### Rule 6: Authorization Uses PermissionService

Every protected endpoint must check permissions through `PermissionService`, not by manually comparing user IDs.

```java
// ✅ CORRECT — Use PermissionService
if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
    return ResponseEntity.status(403).build();
}

// ❌ WRONG — Manual role check
if (!currentUser.getRole().equals("ADMIN")) { ... }
```

### Rule 7: Every Route Is Inside a Feature Module (Frontend)

No components live outside `core/`, `shared/`, or `features/`. Everything user-facing goes in `features/<module>/`.

```
✅ src/app/features/billing/pages/invoice-list/
❌ src/app/invoice-list/          ← component at app root level
```

---

## 2. Backend Rules & Patterns

### 2.1 Module Folder Structure

Every module follows this exact structure:

```
modules/<module-name>/
├── controller/          ← REST endpoints (@RestController)
├── dto/
│   ├── request/         ← Incoming payloads (@Valid)
│   └── response/        ← Outgoing payloads
├── model/               ← MongoDB entities (@Document)
├── repository/          ← Spring Data interfaces (MongoRepository)
├── service/             ← Business logic (@Service)
└── permission/          ← Permission enums (if module has RBAC)
```

**Rules:**
- One `@RestController` per API resource (not per HTTP method)
- One `@Document` model per MongoDB collection
- Repository interfaces extend `MongoRepository<T, String>`
- Services are `@Service` annotated
- Permission enums live in their own `permission/` package

### 2.2 Controller Pattern

Every controller in the new `/adaptive` API follows this pattern:

```java
@RestController
@RequestMapping("/adaptive/domains/{slug}/resource")  // ← always under a domain
public class ResourceController {

    @Autowired
    private OrganisationRepository organisationRepository; // or OrganisationLookupPort

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ResourceRepository resourceRepository;

    // ── LIST ──────────────────────────────────
    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug) {
        Organisation domain = requireDomain(slug);                              // 1. resolve domain
        if (!permissionService.hasDomainPermission(                             // 2. check permissions
                domain.getId(), DomainPermission.DOMAIN_USE_APP)) {
            return ResponseEntity.status(403).build();
        }
        List<Resource> items = resourceRepository.findByDomainId(domain.getId()); // 3. fetch data
        return ResponseEntity.ok(items);                                        // 4. return response
    }

    // ── CREATE ────────────────────────────────
    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable String slug,
            @Valid @RequestBody CreateResourceRequest request) {                 // ← always use @Valid
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(
                domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        // ... business logic ...
        return ResponseEntity.ok(new ResourceResponse(saved));                  // ← return DTO
    }

    // ── HELPER (every controller has this) ────
    private Organisation requireDomain(String slug) {
        return organisationRepository.findBySlug(slugify(slug))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Domain not found"));
    }

    private String slugify(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }
}
```

**Key points:**
1. Always resolve the domain first via `requireDomain(slug)`
2. Always check permissions before any data access
3. Always use `@Valid` on request bodies
4. Always return `ResponseEntity<?>` for flexibility
5. Include `slugify()` helper for slug normalization

### 2.3 Service Pattern — Provisioning

When creating a resource that needs default sub-resources (groups, settings, etc.), use a **Provisioning Service**:

```java
@Service
public class ResourceProvisioningService {

    @Autowired
    private SubResourceRepository subResourceRepository;

    public void provisionDefaults(Resource resource, String ownerUserId) {
        if (resource == null || resource.getId() == null) return;

        // Only provision if not already done (idempotent)
        List<SubResource> existing = subResourceRepository.findByResourceId(resource.getId());
        if (existing.isEmpty()) {
            SubResource admin = buildSubResource(resource.getId(), "Admin", allPermissions());
            SubResource viewer = buildSubResource(resource.getId(), "Viewer", readPermissions());
            subResourceRepository.saveAll(List.of(admin, viewer));

            // Auto-assign the creator
            if (ownerUserId != null) {
                assignUser(admin, ownerUserId);
            }
        }
    }

    private SubResource buildSubResource(String resourceId, String name, EnumSet<Permission> perms) {
        SubResource sr = new SubResource();
        sr.setResourceId(resourceId);
        sr.setName(name);
        sr.setPermissions(perms);
        return sr;
    }
}
```

**Existing examples:**
- `DomainProvisioningService` — creates "Domain Admin" and "Domain Contributor" groups
- `ApplicationProvisioningService` — creates "App Admin", "App Editor", "App Viewer" groups

### 2.4 DTO Pattern

**Request DTO** — Uses Jakarta Validation:

```java
package com.adaptivebp.modules.mymodule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateResourceRequest {
    @NotBlank
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name;

    @NotBlank
    @Size(min = 3, max = 50)
    private String slug;

    @Size(max = 500)
    private String description;

    // Constructors + Getters + Setters
    public CreateResourceRequest() {}
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ...
}
```

**Response DTO** — Maps from model:

```java
package com.adaptivebp.modules.mymodule.dto.response;

public class ResourceResponse {
    private String id;
    private String name;
    private String slug;
    private Date createdAt;

    public ResourceResponse() {}

    public ResourceResponse(Resource resource) {   // ← Constructor from model
        this.id = resource.getId();
        this.name = resource.getName();
        this.slug = resource.getSlug();
        this.createdAt = resource.getCreatedAt();
    }

    // Getters + Setters
}
```

### 2.5 Model Pattern

```java
package com.adaptivebp.modules.mymodule.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import jakarta.validation.constraints.NotBlank;

@Document(collection = "my_resources")        // ← explicit collection name
public class MyResource {
    @Id
    private String id;                         // ← MongoDB generates this

    @NotBlank
    private String name;

    @Indexed(unique = true)                    // ← add indexes for frequently queried fields
    private String slug;

    @Indexed
    private String domainId;                   // ← always link to domain for multi-tenancy

    private Date createdAt;

    public MyResource() {
        this.createdAt = new Date();
    }

    // Getters + Setters (no Lombok — project uses manual getters/setters)
}
```

### 2.6 Repository Pattern

```java
package com.adaptivebp.modules.mymodule.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MyResourceRepository extends MongoRepository<MyResource, String> {
    List<MyResource> findByDomainId(String domainId);             // ← always have this
    Optional<MyResource> findByDomainIdAndSlug(String domainId, String slug);
    Boolean existsByDomainIdAndSlug(String domainId, String slug);
}
```

**Rules:**
- Always include `findByDomainId()` for multi-tenant queries
- Use `Optional<T>` for single-item lookups
- Use `Boolean existsBy...()` for existence checks before creation
- Method names follow Spring Data naming conventions (no custom queries needed)

### 2.7 Permission Pattern

```java
package com.adaptivebp.modules.mymodule.permission;

public enum MyResourcePermission {
    RESOURCE_READ,
    RESOURCE_WRITE,
    RESOURCE_DELETE,
    RESOURCE_MANAGE_USERS
}
```

Use `EnumSet` in group definitions for efficient permission storage.

---

## 3. Frontend Rules & Patterns

### 3.1 Where Does Code Go?

| Type | Location | Imported By |
|---|---|---|
| **Singleton services** (AuthService, DomainService, ApiService) | `core/services/` | Root module only |
| **Guards** (AuthGuard, DomainGuard) | `core/guards/` | Root routing only |
| **Interceptors** (AuthInterceptor) | `core/interceptors/` | Root module only |
| **Shared interfaces/models** | `core/models/` | Any module |
| **Reusable UI components** (navbar, footer) | `shared/components/` | Any feature module |
| **Feature pages & components** | `features/<module>/pages/` | Only within that module |
| **Feature-scoped components** | `features/<module>/components/` | Only within that module |

### 3.2 Creating a New Feature Module

**Step 1:** Create the folder structure:

```
src/app/features/billing/
├── billing.module.ts
├── pages/
│   ├── invoice-list/
│   │   ├── invoice-list.component.ts
│   │   ├── invoice-list.component.html
│   │   └── invoice-list.component.css
│   └── invoice-detail/
│       ├── invoice-detail.component.ts
│       ├── invoice-detail.component.html
│       └── invoice-detail.component.css
└── components/                     ← optional: components used only within this feature
    └── invoice-row/
        ├── invoice-row.component.ts
        ├── invoice-row.component.html
        └── invoice-row.component.css
```

**Step 2:** Create the module with child routes:

```typescript
// billing.module.ts
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { InvoiceListComponent } from './pages/invoice-list/invoice-list.component';
import { InvoiceDetailComponent } from './pages/invoice-detail/invoice-detail.component';

const routes: Routes = [
    { path: '', component: InvoiceListComponent },
    { path: ':invoiceId', component: InvoiceDetailComponent },
];

@NgModule({
    declarations: [
        InvoiceListComponent,
        InvoiceDetailComponent,
    ],
    imports: [
        SharedModule,           // ← always import SharedModule (gives you CommonModule + shared components)
        RouterModule.forChild(routes),  // ← forChild, NOT forRoot
    ],
})
export class BillingModule { }
```

**Step 3:** Register in `app-routing.module.ts` with lazy loading:

```typescript
// In app-routing.module.ts
{
    path: 'domain/:slug/billing',
    loadChildren: () => import('./features/billing/billing.module').then(m => m.BillingModule),
    canActivate: [AuthGuard],       // ← always guard protected routes
},
```

### 3.3 Service Call Pattern

All HTTP calls go through `BaseService` (`core/services/api.service.ts`):

```typescript
// In your component:
import { BaseService } from '../../../../core/services/api.service';

export class InvoiceListComponent implements OnInit {
    invoices: any[] = [];

    constructor(private baseService: BaseService) {}

    ngOnInit(): void {
        this.loadInvoices();
    }

    loadInvoices(): void {
        this.baseService.get('/adaptive/domains/' + this.slug + '/invoices')
            .subscribe({
                next: (data: any) => {
                    this.invoices = data;
                },
                error: (err: any) => {
                    console.error('Failed to load invoices', err);
                }
            });
    }
}
```

**Rules:**
- Always use `BaseService` for HTTP calls — never use `HttpClient` directly
- Always provide explicit types for error callbacks (`: any`)
- Use relative paths with `../../../../core/services/` (not absolute `src/app/...`)

### 3.4 Import Path Rules

```typescript
// ✅ CORRECT — relative paths
import { AuthService } from '../../../../core/services/auth.service';
import { FormModel } from '../../../../core/models/form.model';
import { BaseService } from '../../../../core/services/api.service';

// ❌ WRONG — absolute paths (break in builds)
import { AuthService } from 'src/app/core/services/auth.service';
import { AuthService } from 'src/app/services/auth.service';
```

---

## 4. Walkthrough: Adding a New Feature End-to-End

### Example: Add a "Notes" feature to domains

This walkthrough adds a Notes feature where domain users can create and view notes.

### Step 1: Backend — Model

```java
// modules/notes/model/Note.java
package com.adaptivebp.modules.notes.model;

@Document(collection = "notes")
public class Note {
    @Id
    private String id;

    @NotBlank
    private String title;

    private String content;

    @Indexed
    private String domainId;

    private String createdBy;
    private Date createdAt = new Date();

    // Getters + Setters
}
```

### Step 2: Backend — Repository

```java
// modules/notes/repository/NoteRepository.java
package com.adaptivebp.modules.notes.repository;

public interface NoteRepository extends MongoRepository<Note, String> {
    List<Note> findByDomainId(String domainId);
    Optional<Note> findByIdAndDomainId(String id, String domainId);
}
```

### Step 3: Backend — DTOs

```java
// modules/notes/dto/request/CreateNoteRequest.java
public class CreateNoteRequest {
    @NotBlank @Size(min = 1, max = 200)
    private String title;
    @Size(max = 5000)
    private String content;
    // Getters + Setters
}

// modules/notes/dto/response/NoteResponse.java
public class NoteResponse {
    private String id;
    private String title;
    private String content;
    private String createdBy;
    private Date createdAt;

    public NoteResponse(Note note) {
        this.id = note.getId();
        this.title = note.getTitle();
        this.content = note.getContent();
        this.createdBy = note.getCreatedBy();
        this.createdAt = note.getCreatedAt();
    }
    // Getters + Setters
}
```

### Step 4: Backend — Controller

```java
// modules/notes/controller/NoteController.java
@RestController
@RequestMapping("/adaptive/domains/{slug}/notes")
public class NoteController {

    @Autowired private OrganisationRepository organisationRepository;
    @Autowired private PermissionService permissionService;
    @Autowired private NoteRepository noteRepository;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String slug) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_USE_APP)) {
            return ResponseEntity.status(403).build();
        }
        List<NoteResponse> notes = noteRepository.findByDomainId(domain.getId())
                .stream().map(NoteResponse::new).collect(Collectors.toList());
        return ResponseEntity.ok(notes);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug,
                                     @Valid @RequestBody CreateNoteRequest request) {
        Organisation domain = requireDomain(slug);
        if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_MANAGE_APPS)) {
            return ResponseEntity.status(403).build();
        }
        Note note = new Note();
        note.setDomainId(domain.getId());
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        // note.setCreatedBy(currentPrincipalId()); // set from authenticated user
        Note saved = noteRepository.save(note);
        return ResponseEntity.ok(new NoteResponse(saved));
    }

    private Organisation requireDomain(String slug) { /* same as other controllers */ }
    private String slugify(String input) { /* same as other controllers */ }
}
```

### Step 5: Frontend — Feature Module

```typescript
// features/notes/notes.module.ts
@NgModule({
    declarations: [NoteListComponent, NoteCreateComponent],
    imports: [SharedModule, RouterModule.forChild([
        { path: '', component: NoteListComponent },
        { path: 'create', component: NoteCreateComponent },
    ])],
})
export class NotesModule { }
```

### Step 6: Frontend — Register Route

```typescript
// app-routing.module.ts — add this entry
{
    path: 'domain/:slug/notes',
    loadChildren: () => import('./features/notes/notes.module').then(m => m.NotesModule),
    canActivate: [AuthGuard],
},
```

### Step 7: Update WebSecurityConfig

```java
// In WebSecurityConfig.java, the /adaptive/** pattern already covers new endpoints.
// No changes needed unless you need special auth rules.
```

**Result:** The Notes feature is fully isolated, follows all patterns, and integrates cleanly.

---

## 5. Anti-Patterns (What NOT to Do)

### ❌ Don't put components at `src/app/` root level

```
❌ src/app/my-component/
✅ src/app/features/my-feature/pages/my-component/
```

### ❌ Don't import `HttpClient` directly in components

```typescript
❌ constructor(private http: HttpClient) {}
✅ constructor(private baseService: BaseService) {}
```

### ❌ Don't use absolute imports in TypeScript

```typescript
❌ import { X } from 'src/app/core/services/x.service';
✅ import { X } from '../../../../core/services/x.service';
```

### ❌ Don't access another module's repository directly

```java
❌ // In appmanagement controller:
@Autowired OrganisationRepository orgRepo;  // Belongs to organisation module!

✅ // Use an interface/port or inject the module's service
@Autowired OrganisationLookupPort orgLookup;
```

### ❌ Don't put business logic in controllers

```java
❌ // In controller:
DomainGroup group = new DomainGroup();
group.setName("Admin");
group.setPermissions(EnumSet.allOf(DomainPermission.class));
domainGroupRepository.save(group);

✅ // In controller — delegate to service:
domainProvisioningService.provisionDefaults(organisation, ownerId);
```

### ❌ Don't skip permission checks

```java
❌ @GetMapping
public ResponseEntity<?> list(@PathVariable String slug) {
    return ResponseEntity.ok(resourceRepo.findAll());  // No auth check!
}

✅ @GetMapping
public ResponseEntity<?> list(@PathVariable String slug) {
    Organisation domain = requireDomain(slug);
    if (!permissionService.hasDomainPermission(domain.getId(), DomainPermission.DOMAIN_USE_APP)) {
        return ResponseEntity.status(403).build();
    }
    return ResponseEntity.ok(resourceRepo.findByDomainId(domain.getId()));
}
```

### ❌ Don't use wildcard Angular versions

```json
❌ "@angular/animations": "*",
✅ "@angular/animations": "^17.3.9",
```

---

## 6. Quick Reference Cheat Sheet

### New Backend Endpoint Checklist

- [ ] Created `model/` entity with `@Document`, `@Id`, `domainId` field
- [ ] Created `repository/` interface extending `MongoRepository` with `findByDomainId()`
- [ ] Created `dto/request/` with `@NotBlank`, `@Size` validation
- [ ] Created `dto/response/` with constructor from model
- [ ] Created `controller/` with `@RequestMapping("/adaptive/domains/{slug}/...")"`
- [ ] Added `requireDomain()` and `slugify()` helpers
- [ ] Added `PermissionService` check in every endpoint
- [ ] Used `@Valid` on all `@RequestBody` parameters
- [ ] Returning DTOs (not raw models) from controller

### New Frontend Feature Checklist

- [ ] Created `features/<name>/` directory with proper structure
- [ ] Created `<name>.module.ts` with `RouterModule.forChild(routes)`
- [ ] Imported `SharedModule` in the feature module
- [ ] Added lazy-loaded route in `app-routing.module.ts`
- [ ] Applied `AuthGuard` (or other guard) to the route
- [ ] Used `BaseService` for all HTTP calls
- [ ] Used **relative** import paths (not `src/app/...`)
- [ ] Added explicit `: any` types on error callbacks

### File Naming Conventions

| Type | Backend Pattern | Frontend Pattern |
|---|---|---|
| **Controller/Component** | `ResourceController.java` | `resource-name.component.ts` |
| **Service** | `ResourceService.java` | `resource-name.service.ts` |
| **Model/Interface** | `Resource.java` | `resource.model.ts` |
| **Repository** | `ResourceRepository.java` | — |
| **DTOs** | `CreateResourceRequest.java` | — |
| **Module** | — | `resource.module.ts` |
| **Guard** | — | `resource.guard.ts` |

### URL Pattern Standards

```
# Backend API
/adaptive/domains/{slug}/<resource>           ← list, create
/adaptive/domains/{slug}/<resource>/{id}      ← get, update, delete
/adaptive/domains/{slug}/<resource>/{id}/sub  ← nested resources

# Frontend Routes
/domain/:slug/<feature>                       ← list page
/domain/:slug/<feature>/create                ← create page
/domain/:slug/<feature>/:id                   ← detail page
```
