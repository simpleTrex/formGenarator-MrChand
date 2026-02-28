# Next Steps — AdaptiveBP Roadmap

Post-refactoring roadmap. Each phase builds on the previous.

> **What's done:** Modular monolith architecture, frontend restructure, dependency cleanup, documentation (`README.md`, `PROJECT_ARCHITECTURE.md`, `DEVELOPER_GUIDE.md`).

---

## Phase 1 — Stabilisation & Legacy Cleanup _(Now)_

> Make the refactored app reliable and remove dead code.

- [ ] **End-to-end smoke test** — Test all critical flows:
  - Owner signup → login → create domain
  - Domain user signup → domain login → dashboard
  - Create application → open app → manage models
  - Form builder → add fields → preview → submit data
- [ ] **Fix deprecation warnings** — Replace `HttpSecurity.cors()` with `cors(Customizer.withDefaults())` in `WebSecurityConfig.java`
- [ ] **Set logging to INFO** — Change `logging.level.root=DEBUG` to `INFO` in `application.properties`
- [ ] **Remove legacy `/custom_form` API** — Currently two APIs exist side-by-side:
  - Migrate any frontend calls still using `/custom_form/**` to `/adaptive/**`
  - Delete `AuthController.java`, `UserController.java` (identity module)
  - Delete `CustomFormController.java` (formbuilder module)
  - Delete `UserDetailsImpl.java`, `UserDetailsServiceImpl.java` (shared/security)
  - Delete `User.java`, `Role.java`, `ERole.java` (identity/model)
  - Delete all files in `formbuilder/model/legacy/`
  - Delete legacy repositories (`CustomFormRepository`, `FormFieldRepository`, etc.)
  - Update `JwtAuthFilter` to only handle `AdaptiveUserDetails` tokens
- [ ] **Pin Angular versions** — Replace `"*"` wildcard versions with `"^17.3.9"` for `@angular/animations`, `@angular/common`, `@angular/compiler`, `@angular/core`, `rxjs`, `tslib`
- [ ] **Merge to main** — Create PR from `feature/modular-monolith` → `main`

---

## Phase 2 — Testing & Code Quality _(1–2 weeks)_

> Add tests, documentation, and developer tooling.

- [ ] **Unit tests** — Priority targets:
  - `JwtTokenProvider` — token generation, parsing, expiry, invalid tokens
  - `OwnerAuthController` — signup, login, duplicate email, wrong password
  - `DomainAuthController` — signup, login, domain not found
  - `DomainProvisioningService` — default groups created, idempotency
  - `ApplicationProvisioningService` — default groups, owner auto-assigned
  - `PermissionService` — owner gets all permissions, group-based checks
- [ ] **Integration tests** — `@SpringBootTest` + embedded MongoDB (`de.flapdoodle.embed.mongo`) for repository layer
- [ ] **API documentation** — Add `springdoc-openapi-starter-webmvc-ui` for auto-generated Swagger docs at `/swagger-ui.html`
- [ ] **Spring profiles** — Create `application-dev.properties` and `application-prod.properties` for environment-specific config
- [ ] **Frontend linting** — Set up ESLint via `ng lint`, fix issues, enforce in CI
- [ ] **Error handling** — Standardise all API errors to a consistent JSON format via `GlobalExceptionHandler`

---

## Phase 3 — Feature Development _(2–4 weeks)_

> Build out user-facing features following the patterns in `DEVELOPER_GUIDE.md`.

- [ ] **User profile management** — `/adaptive/profile` endpoints + frontend settings page (bio, full name, avatar)
- [ ] **Domain settings page** — Edit domain name, description, industry; transfer ownership
- [ ] **Role-based UI** — Show/hide navbar items, buttons, and pages based on `PermissionService` responses via `/access/me` endpoint
- [ ] **Form builder improvements:**
  - Client-side validation rules (required, min/max, regex) on form fields
  - Field reordering (drag & drop)
  - Form data export to CSV
- [ ] **Search & filter** — Search across domains, apps, and models with debounced input
- [ ] **Notifications** — In-app notification system for domain invites, form submissions
- [ ] **Dark mode** — CSS variable-based theme toggle

---

## Phase 4 — Production Readiness _(4–6 weeks)_

> Prepare for deployment.

- [ ] **CI/CD pipeline** — GitHub Actions:
  - Backend: `mvn test` → `mvn package` → Docker build → deploy
  - Frontend: `ng lint` → `ng build --configuration production` → deploy to CDN/S3
- [ ] **Docker** — `Dockerfile` for backend, `docker-compose.yml` for local dev (backend + MongoDB)
- [ ] **Security hardening:**
  - Rate limiting on `/auth/**` endpoints (prevent brute-force)
  - HTTPS with proper certificates (replace `ssl-server.jks` placeholder)
  - CSRF protection for cookie-based JWT
- [ ] **Observability:**
  - Spring Boot Actuator + health checks
  - Structured JSON logging
  - Error tracking (Sentry or similar)
- [ ] **Audit logging** — Log security events (login, logout, role changes) to dedicated `audit_log` collection

---

## Phase 5 — Scale & Extend _(6+ weeks)_

> Evolve the platform.

- [ ] **Process engine module** — Workflow/BPM engine as `modules/processengine/` (deferred from initial refactoring)
- [ ] **Multi-tenancy at DB level** — Database-per-domain or collection prefixing for strict data isolation
- [ ] **WebSocket** — Real-time collaborative form editing
- [ ] **Plugin system** — Third-party field types in the form builder
- [ ] **Mobile-responsive redesign** — Full responsive UI for tablets and phones
- [ ] **i18n** — Multi-language support for UI and form labels
- [ ] **Angular standalone migration** — Migrate from NgModules to standalone components (Angular 18+)

---

## Architecture Decisions to Revisit

| Decision | Current | Revisit When... |
|---|---|---|
| Modular Monolith | Single JAR | > 5 developers on different modules |
| Flat module folders | `controller/`, `service/`, `model/` | Modules grow past 20+ files per folder → switch to DDD 4-layer (`domain/`, `application/`, `infrastructure/`, `api/`) |
| MongoDB | Shared collections | Need strict per-tenant data isolation |
| JWT in cookies | `HttpOnly` cookie | Need OAuth2 / SSO / third-party auth |
| Angular 17 modules | `NgModule` + lazy loading | Angular 19+ drops NgModule support |
| Bootstrap CSS | Global stylesheet | Bundle > 1 MB or need SSR → switch to Tailwind or CSS modules |
