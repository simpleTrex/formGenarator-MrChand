# Dynamic Model Implementation Details: User-Created Forms for Workflows

## Overview
This document details the dynamic model creation feature in the Form Generator application, where users can build custom form models (schemas) for workflows. These models define the structure of forms used in business processes, allowing dynamic data collection and submission. The system ensures secure, app-scoped access with RBAC, storing models in MongoDB and enabling CRUD operations via REST APIs.

## Architecture for User-Created Models
- **Dynamic Creation**: Users with APP_WRITE permission can create/edit models via the Angular frontend (e.g., in Edit mode of App Home or Domain Home).
- **Model Structure**: Models are JSON schemas with fields (e.g., text, email, number), validations, and metadata.
- **Workflow Integration**: Models are used to generate forms for workflows, such as approval processes or data entry tasks.
- **Security**: App-scoped permissions prevent unauthorized modifications; data is isolated per app.

## Security Access Control for Dynamic Models

### Permission Requirements
- **Create/Edit/Delete Models**: Requires APP_WRITE permission on the specific app.
- **View Models**: Requires APP_READ permission.
- **Admin Access**: APP_ADMIN allows managing groups/users who can access models.

### Access Control Flow
1. User authenticates and selects an app.
2. Frontend checks permissions via PermissionService.
3. If authorized, UI enables model builder; otherwise, shows read-only or hides features.
4. API endpoints enforce permissions server-side.

### Role-Based Restrictions
- **Normal Users**: Can view/submit forms but not create models.
- **Contributors**: Can create/edit models in assigned apps.
- **Admins**: Full control, including access management.

## Data Storage for User-Created Models

### MongoDB Collection: models
Models are stored as documents in the `models` collection, scoped to apps.

### Model Document Schema
```json
{
  "_id": ObjectId("..."),
  "appId": ObjectId("..."),  // Links to the app
  "name": "WorkflowApprovalForm",
  "description": "Form for workflow approvals",
  "schema": {
    "fields": [
      {
        "name": "requestorName",
        "type": "string",
        "label": "Requestor Name",
        "required": true,
        "validation": {"minLength": 2}
      },
      {
        "name": "amount",
        "type": "number",
        "label": "Amount",
        "required": true,
        "validation": {"min": 0}
      },
      {
        "name": "approvalStatus",
        "type": "select",
        "label": "Status",
        "options": ["Pending", "Approved", "Rejected"],
        "required": true
      }
    ]
  },
  "createdBy": "user@example.com",
  "createdAt": ISODate("2023-01-01T00:00:00Z"),
  "updatedAt": ISODate("2023-01-01T00:00:00Z")
}
```

### Storage Details
- **App Scoping**: Each model belongs to an app via `appId`, ensuring isolation.
- **Indexing**: Indexed on `appId` and `name` for efficient queries.
- **Versioning**: `updatedAt` tracks changes; no full versioning yet, but can be added.

## How Data is Saved: CRUD for Dynamic Models

### Creating a Model
- **UI Process**: In Edit mode, user drags fields, sets properties, and saves.
- **API Call**: `POST /api/domains/{domainId}/apps/{appSlug}/models`
- **Steps**:
  1. Frontend validates permissions and builds JSON schema.
  2. Sends request with model data.
  3. Backend checks APP_WRITE, creates document, saves to MongoDB.
  4. Returns model ID; UI updates list.

### Reading Models
- **UI Process**: Loads models in Preview/Edit modes.
- **API Call**: `GET /api/domains/{domainId}/apps/{appSlug}/models`
- **Steps**:
  1. Frontend fetches list.
  2. Backend filters by app and user permissions.
  3. Returns JSON array of models.

### Updating a Model
- **UI Process**: Edit schema in model builder.
- **API Call**: `PUT /api/domains/{domainId}/apps/{appSlug}/models/{modelId}`
- **Steps**:
  1. Frontend sends updated schema.
  2. Backend validates permissions, updates document.
  3. Saves changes; returns updated model.

### Deleting a Model
- **UI Process**: Delete button in Edit mode.
- **API Call**: `DELETE /api/domains/{domainId}/apps/{appSlug}/models/{modelId}`
- **Steps**:
  1. Backend checks permissions, removes document.
  2. UI refreshes list.

### Form Submission (Workflow Data)
- **Separate Collection**: Submitted form data is saved in a `submissions` collection.
- **Schema**: Matches the model's schema, with additional metadata (e.g., submitter, timestamp).
- **API**: `POST /api/submit/{modelId}` – validates against schema, saves data.

## Frontend Implementation

### Model Builder Component
- **AppModelsComponent**: Drag-and-drop interface for creating fields.
- **Field Types**: String, number, email, select, date, etc.
- **Validation**: Client-side checks before saving.

### Workflow Usage
- Models generate forms in workflows (e.g., via ModelRenderComponent).
- Users submit data, which is stored separately from the model definition.

## Examples

### Creating a Workflow Model
1. User with APP_WRITE enters Edit mode in an app.
2. Adds fields: "Employee Name" (string, required), "Leave Days" (number), "Reason" (textarea).
3. Saves model via API.
4. Model stored in MongoDB, ready for workflow forms.

### Security Example
- User without APP_WRITE tries to edit: UI disables save button; API rejects with 403.

## Conclusion
Dynamic models enable users to create custom forms for workflows securely. Data is app-scoped, permissions enforced, and operations handled via REST APIs. This supports flexible business processes while maintaining data integrity.