# Application (Workflow) System - Current State & Requirements

## Current Implementation Status

### ✅ ALREADY IMPLEMENTED (Backend):

#### 1. **Application Model** (`Application.java`)
- URL pattern: `/adaptive/domains/{slug}/apps/{appSlug}`
- Fields: id, domainId, slug, name, ownerUserId, metadata
- Unique constraint: domainId + slug

#### 2. **Application Groups** (`AppGroup.java`)
- Similar to Domain Groups
- Fields: id, appId, name, permissions (AppPermission enum), defaultGroup
- Three default groups provisioned automatically:
  - **App Admin** - full access (APP_READ, APP_WRITE, APP_EXECUTE)
  - **App Editor** - read/write (APP_READ, APP_WRITE)
  - **App Viewer** - read only (APP_READ)

#### 3. **Application Group Members** (`AppGroupMember.java`)
- Links users to app groups
- Fields: id, groupId, appId, userId, assignedBy

#### 4. **Existing Endpoints**:
- `GET /adaptive/domains/{slug}/apps` - List all apps in domain
- `POST /adaptive/domains/{slug}/apps` - Create new app
- `GET /adaptive/domains/{slug}/apps/{appSlug}/groups` - List app groups
- `POST /adaptive/domains/{slug}/apps/{appSlug}/groups` - Create app group
- `POST /adaptive/domains/{slug}/apps/{appSlug}/groups/{groupId}/members` - Add member
- `DELETE /adaptive/domains/{slug}/apps/{appSlug}/groups/{groupId}/members/{userId}` - Remove member

#### 5. **Permission System**:
- `AppPermission` enum: APP_READ, APP_WRITE, APP_EXECUTE
- Permission checking via `PermissionService`

---

## ❌ MISSING ENDPOINTS (Backend):

### Required for User Management UI:

1. **GET `/adaptive/domains/{slug}/apps/{appSlug}/users`**
   - Get all domain users with their app group memberships
   - Similar to domain users endpoint
   - Returns: List of users with their app groups

2. **GET `/adaptive/domains/{slug}/apps/{appSlug}/groups/{groupId}/members`**
   - Get all members of a specific app group
   - Returns: List of group members with details

3. **GET `/adaptive/domains/{slug}/apps/{appSlug}/users/{userId}/groups`**
   - Get all app groups a specific user belongs to
   - Returns: List of app groups

4. **GET `/adaptive/domains/{slug}/apps/{appSlug}`**
   - Get single application details
   - Returns: Application info

---

## 🔧 ENHANCEMENTS NEEDED:

### 1. **Public Access Feature** (New Requirement):
Add to `Application` model:
```java
public enum PublicAccessLevel {
    NO_ACCESS,      // Only domain users can access
    VIEW_ONLY,      // Public can view (no login)
    USE_AS_GUEST    // Public can use as default user
}

private PublicAccessLevel publicAccess = PublicAccessLevel.NO_ACCESS;
```

### 2. **Update AppPermission Enum**:
```java
public enum AppPermission {
    APP_READ,
    APP_WRITE,
    APP_EXECUTE,
    APP_MANAGE_USERS,    // NEW: Manage user permissions
    APP_MANAGE_SETTINGS  // NEW: Manage app settings
}
```

### 3. **Update Default Groups**:
Change from:
- App Admin, App Editor, App Viewer

To match your requirement:
- **App Admin** - All permissions including MANAGE_USERS, MANAGE_SETTINGS
- **App Contributor** - APP_READ, APP_WRITE, APP_EXECUTE (can design/edit)
- **App User** - APP_READ, APP_EXECUTE (default user - just use)

---

## 📱 MISSING FRONTEND:

### Components Needed:

1. **`app-home.component`** (Main App Page)
   - Similar to domain-home
   - URL: `/domain/{slug}/app/{appSlug}`
   - Shows app info, groups, and user management

2. **`app-users.component`** or integrate into app-home
   - Drag-and-drop user management
   - Same UI pattern as domain users
   - Shows users in different app groups

3. **Update `domain-home.component`**
   - Make app cards clickable to navigate to app page
   - Show app count and status

### Service Updates Needed in `domain.service.ts`:

```typescript
// Application/Workflow management
getApplication(domainSlug: string, appSlug: string): Observable<any>
getApplicationGroups(domainSlug: string, appSlug: string): Observable<any>
getApplicationUsersWithGroups(domainSlug: string, appSlug: string): Observable<any>
getAppGroupMembers(domainSlug: string, appSlug: string, groupId: string): Observable<any>
getUserAppGroups(domainSlug: string, appSlug: string, userId: string): Observable<any>
addAppGroupMember(domainSlug: string, appSlug: string, groupId: string, username: string): Observable<any>
removeAppGroupMember(domainSlug: string, appSlug: string, groupId: string, userId: string): Observable<any>
updateAppPublicAccess(domainSlug: string, appSlug: string, accessLevel: string): Observable<any>
```

---

## 📋 IMPLEMENTATION PLAN:

### Phase 1: Backend Missing Endpoints
1. Create DTOs: `AppUserResponse`, `AppGroupMemberResponse`
2. Add missing GET endpoints to `AppGroupController`
3. Add GET endpoint to `ApplicationController` for single app
4. Write unit tests

### Phase 2: Public Access Feature
1. Add `PublicAccessLevel` enum
2. Update `Application` model
3. Add endpoint to update public access
4. Update permission checks to handle public users

### Phase 3: Frontend
1. Update `domain.service.ts` with new methods
2. Create `app-home` component
3. Integrate drag-and-drop user management
4. Add routing for apps
5. Make app cards in domain-home clickable

### Phase 4: Enhanced Permissions
1. Update `AppPermission` enum
2. Update default groups provisioning
3. Update permission checking logic

---

## 🎯 SUMMARY:

**What you call "Workflows" = Applications (already exists!)**

✅ Structure already exists
✅ Group management exists
✅ Basic CRUD exists

❌ Missing: User listing endpoints
❌ Missing: Public access feature
❌ Missing: Complete frontend UI
❌ Need: Better permission granularity

**Recommendation**: Start with Phase 1 (missing endpoints), then Phase 3 (frontend), then add public access (Phase 2).
