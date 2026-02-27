# Project Walkthrough — formGenarator-MrChand

## What Was Done

A full codebase exploration and architecture review of the project was conducted on 2026-02-17.

---

## Project Summary

**Multi-tenant adaptive business process / form generator platform.**

| Layer | Tech | Version |
|---|---|---|
| Backend | Spring Boot + Spring Security + JWT | 3.3.1 |
| Database | MongoDB | 6.0 |
| Frontend | Angular (NgModule-based) | 17 |

### Architecture Style: **Monolithic (Two-Tier)**

- Single Spring Boot JAR backend (76 Java files)
- Separate Angular SPA frontend (45 TypeScript files)
- Single shared MongoDB database
- **Verdict:** Monolithic is the correct choice for this project's current size and team

---

## Codebase Inventory

### Backend (76 Java files)

| Layer | Count | Key Files |
|---|---|---|
| Controllers | 11 | `DomainController`, `AuthController`, `CustomFormController`, `AppGroupController` |
| Services | 5 | `PermissionService`, `DomainProvisioningService`, `ApplicationProvisioningService` |
| Repositories | 16 | `DomainRepository`, `CustomFormRepository`, `AppGroupRepository` |
| Models | 15+ | `CustomForm`, `FormField`, `Domain`, `Application`, `AppGroup` |
| DTOs | 10 | `AuthResponse`, `DomainUserResponse`, `CreateApplicationRequest` |
| Security | 8 | `WebSecurityConfig`, `AuthTokenFilter`, `UserDetailsImpl` |
| Tests | 12 | Controller, service, and security tests |

### Frontend (45 TypeScript files)

| Category | Components |
|---|---|
| Auth | `login`, `owner-signup`, `domain-login`, `domain-signup` |
| Domain | `domain-home`, `domain-users`, `domain-create`, `app-home` |
| Forms | `model-page`, `model-render`, `model-options`, `render-form`, `data` |
| Navigation | `navi-bar`, `navi-data`, `home-page`, `not-found-page` |
| Services | `auth.service`, `base.service`, `domain.service`, `httpinterceptor` |

---

## Issues Found

### 🔴 Critical

1. **Credentials in `.env`** — MongoDB password, JWT secret committed to git
2. **Duplicate routing** — Two `RouterModule.forRoot()` calls (in `app-routing.module.ts` and `app.module.ts`)
3. **Redundant auth** — Token set in both `BaseService` and `HttpInterceptor`

### 🟡 Moderate

4. **Wrong frontend deps** — `express`, `cors`, `http` in Angular `package.json`
5. **Wildcard versions** — Angular deps using `"*"` (non-reproducible)
6. **God Component** — `domain-home.component.ts` at 332 lines
7. **No form service layer** — `CustomFormController` injects repositories directly
8. **Clutter files** — `SecondGitPushCodes.md`, `log` (325KB), `DOCKER_AUTHENTICATION_LOG.md`
9. **Triple token storage** — Cookie + sessionStorage + localStorage

### 🟢 Strengths

- Clean enum-based permission system (`PermissionService`)
- Multi-tenant RBAC (Owner → Domain → App → Groups)
- 12 backend tests
- Proper security config (stateless JWT, granular endpoints)
- Auto-provisioning of default groups

---

## Changes Made During Review

| Change | File |
|---|---|
| Architecture analysis document | `ARCHITECTURE_ANALYSIS.md` |
| Updated `.gitignore` | Added `.env`, `log`, AI tool dirs, removed stray `C4` |

---

## Recommended Next Steps

### Phase 1: Security
- Remove `.env` from git (`git rm --cached .env`)
- Rotate all credentials
- Fix duplicate routing

### Phase 2: Cleanup
- Delete clutter files (`SecondGitPushCodes.md`, `log`, `DOCKER_AUTHENTICATION_LOG.md`)
- Remove wrong npm deps (`express`, `cors`, `http`)
- Pin Angular versions

### Phase 3: Refactor
- Extract `CustomFormService` from controller
- Split `domain-home.component.ts` into sub-components
- Consolidate token storage
- Move routes to `app-routing.module.ts`

### Phase 4: Future
- Organize code into modular monolith structure
- Migrate to Angular standalone components
- Update `jjwt` to latest version
