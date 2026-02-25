# Next Steps — AdaptiveBP Roadmap

Post-refactoring roadmap organised into phases. Each phase builds on the previous.

---

## Phase 1 — Stabilisation _(Immediate)_

> Get the refactored app into a reliable, testable state.

- [ ] **End-to-end smoke test** — Manually test all critical user flows:
  - Owner signup → login → create domain
  - Domain user signup → domain login → dashboard
  - Create application → open app → manage models
  - Form builder → add fields → preview form → submit data
- [ ] **Fix CORS `cors()` deprecation** — Replace `HttpSecurity.cors()` in `WebSecurityConfig.java` with the new `cors(Customizer.withDefaults())` API
- [ ] **Set logging to INFO** — Change `logging.level.root=DEBUG` to `INFO` in `application.properties` (DEBUG floods the console)
- [ ] **Git commit** — Commit all changes on `feature/modular-monolith` branch with a clear message
- [ ] **Create PR** — Open a pull request to merge into `main`

---

## Phase 2 — Code Quality _(1–2 weeks)_

> Improve maintainability, testability, and developer experience.

- [ ] **Add unit tests** — Old tests were deleted (referenced legacy packages). Write new tests for:
  - `JwtTokenProvider` — token generation, parsing, validation
  - `AuthController` / `OwnerAuthController` — signup, login, error cases
  - `DomainProvisioningService` — domain creation, user linking
  - `ApplicationProvisioningService` — app creation, permissions
- [ ] **Add integration tests** — Use `@SpringBootTest` + embedded MongoDB for repository tests
- [ ] **Environment-based config** — Move sensitive values (JWT secret, MongoDB URI) to environment variables; use Spring profiles (`dev`, `prod`)
- [ ] **API documentation** — Add Swagger/OpenAPI with `springdoc-openapi` for auto-generated API docs
- [ ] **Frontend linting** — Run `ng lint` and fix remaining issues; add ESLint rules to CI
- [ ] **Optimise bundle size** — The initial bundle is 2.70 MB. Consider:
  - Lazy-load Bootstrap/jQuery only where needed
  - Replace IDS Enterprise with lighter components
  - Tree-shake unused D3.js modules

---

## Phase 3 — Feature Enhancements _(2–4 weeks)_

> Add capabilities that improve the user experience.

- [ ] **User profile management** — Settings page for bio, full name, profile picture (backend endpoints exist, frontend needs pages)
- [ ] **Role-based UI** — Show/hide features based on user roles (OWNER vs DOMAIN_USER vs ADMIN)
- [ ] **Form data export** — Export submitted form data to CSV/Excel
- [ ] **Real-time form validation** — Add client-side validation rules (required, min/max, regex) in the form builder
- [ ] **Notifications system** — In-app notifications for domain invites, form submissions
- [ ] **Search & filter** — Add search across domains, applications, and forms
- [ ] **Dark mode** — Implement dark theme toggle using CSS variables

---

## Phase 4 — Production Readiness _(4–6 weeks)_

> Prepare for deployment and scaling.

- [ ] **CI/CD pipeline** — GitHub Actions for:
  - Backend: `mvn test` → `mvn package` → Docker build → deploy
  - Frontend: `npm test` → `ng build --prod` → deploy to CDN
- [ ] **Dockerise** — Create `Dockerfile` for backend and `docker-compose.yml` for local dev (backend + MongoDB)
- [ ] **Rate limiting** — Add API rate limiting for auth endpoints to prevent brute-force attacks
- [ ] **Audit logging** — Log all security events (login/logout, role changes, data access) to a dedicated audit collection
- [ ] **HTTPS configuration** — Enable SSL/TLS with proper certificates (the `ssl-server.jks` placeholder exists)
- [ ] **Monitoring** — Add Spring Boot Actuator + health checks for production monitoring
- [ ] **Error tracking** — Integrate Sentry or similar for frontend and backend error reporting

---

## Phase 5 — Scale & Extend _(6+ weeks)_

> Evolve the architecture for growth.

- [ ] **Process engine module** — Re-introduce the deferred process/workflow engine as a new module in `modules/processengine/`
- [ ] **Multi-tenancy** — Isolate data per domain at the database level (currently shared collections)
- [ ] **WebSocket support** — Real-time updates for collaborative form editing
- [ ] **Plugin system** — Allow third-party field types in the form builder
- [ ] **Mobile-responsive UI** — Full responsive redesign for tablets and phones
- [ ] **Internationalisation (i18n)** — Multi-language support for UI and form labels

---

## Architecture Decisions to Revisit

| Decision | Current | Consider Changing When... |
|---|---|---|
| Modular Monolith | Single deployable JAR | > 5 teams working on different modules |
| MongoDB | Shared collections per domain | Need strict data isolation per tenant |
| JWT in cookies | `HttpOnly` cookie | Need OAuth2/SSO integration |
| Angular 17 | Module-based lazy loading | Migrate to standalone components (Angular 18+) |
| Bootstrap + jQuery | Global scripts | Bundle size > 3 MB or need SSR |
