import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { BaseService } from './api.service';

export interface CreateDomainPayload {
  name: string;
  slug: string;
  description?: string;
  industry?: string;
}

export interface CreateApplicationPayload {
  name: string;
  slug: string;
  ownerUserId?: string;
}

export type DomainFieldType =
  | 'STRING'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'DATE'
  | 'DATETIME'
  | 'REFERENCE'
  | 'OBJECT'
  | 'ARRAY';

export interface DomainModelField {
  key: string;
  type: DomainFieldType;
  required?: boolean;
  unique?: boolean;
  config?: Record<string, any>;
}

export interface CreateDomainModelPayload {
  name: string;
  slug: string;
  description?: string;
  sharedWithAllApps?: boolean;
  allowedAppIds?: string[];
  fields?: DomainModelField[];
}

export interface UpdateDomainModelPayload {
  name?: string;
  description?: string;
  sharedWithAllApps?: boolean;
  allowedAppIds?: string[];
  fields?: DomainModelField[];
}

@Injectable({ providedIn: 'root' })
export class DomainService {
  private adaptive = environment.adaptiveApi;

  constructor(private baseService: BaseService) { }

  createDomain(payload: CreateDomainPayload): Observable<any> {
    return this.baseService.post(`${environment.api}/domain`, true, payload);
  }

  getMyDomains(): Observable<any> {
    return this.baseService.get(`${environment.api}/domain`, true);
  }

  getBySlug(slug: string): Observable<any> {
    return this.baseService.get(`${environment.api}/domain/slug/${slug}`, true);
  }

  getDomainGroups(slug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/groups`, true);
  }

  getDomainRoles(slug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/access/me`, true);
  }

  getDomainUsersWithGroups(slug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/groups/users`, true);
  }

  getGroupMembers(slug: string, groupId: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/groups/${groupId}/members`, true);
  }

  getUserGroups(slug: string, userId: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/groups/users/${userId}`, true);
  }

  addDomainGroupMember(slug: string, groupId: string, username: string): Observable<any> {
    return this.baseService.post(`${this.adaptive}/domains/${slug}/groups/${groupId}/members`, true, { username });
  }

  removeDomainGroupMember(slug: string, groupId: string, userId: string): Observable<any> {
    return this.baseService.delete(`${this.adaptive}/domains/${slug}/groups/${groupId}/members/${userId}`, true, {});
  }

  getApplications(slug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/apps`, true);
  }

  createApplication(slug: string, payload: CreateApplicationPayload): Observable<any> {
    return this.baseService.post(`${this.adaptive}/domains/${slug}/apps`, true, payload);
  }

  getApplication(slug: string, appSlug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/apps/${appSlug}`, true);
  }

  getAppGroups(slug: string, appSlug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/apps/${appSlug}/groups`, true);
  }

  getAppUsersWithGroups(slug: string, appSlug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/apps/${appSlug}/groups/users`, true);
  }

  getAppGroupMembers(slug: string, appSlug: string, groupId: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/apps/${appSlug}/groups/${groupId}/members`, true);
  }

  getUserAppGroups(slug: string, appSlug: string, userId: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${slug}/apps/${appSlug}/groups/users/${userId}`, true);
  }

  addAppGroupMember(slug: string, appSlug: string, groupId: string, username: string): Observable<any> {
    return this.baseService.post(`${this.adaptive}/domains/${slug}/apps/${appSlug}/groups/${groupId}/members`, true, { username });
  }

  removeAppGroupMember(slug: string, appSlug: string, groupId: string, userId: string): Observable<any> {
    return this.baseService.delete(`${this.adaptive}/domains/${slug}/apps/${appSlug}/groups/${groupId}/members/${userId}`, true, {});
  }

  // Domain Models (Domain-level storage; UI is within App Builder)
  getDomainModels(domainSlug: string, appSlug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${domainSlug}/models?appSlug=${encodeURIComponent(appSlug)}`, true);
  }

  getDomainModel(domainSlug: string, appSlug: string, modelSlug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${domainSlug}/models/${encodeURIComponent(modelSlug)}?appSlug=${encodeURIComponent(appSlug)}`, true);
  }

  createDomainModel(domainSlug: string, appSlug: string, payload: CreateDomainModelPayload): Observable<any> {
    return this.baseService.post(`${this.adaptive}/domains/${domainSlug}/models?appSlug=${encodeURIComponent(appSlug)}`, true, payload);
  }

  updateDomainModel(domainSlug: string, appSlug: string, modelSlug: string, payload: UpdateDomainModelPayload): Observable<any> {
    return this.baseService.put(`${this.adaptive}/domains/${domainSlug}/models/${encodeURIComponent(modelSlug)}?appSlug=${encodeURIComponent(appSlug)}`, true, payload);
  }

  deleteDomainModel(domainSlug: string, appSlug: string, modelSlug: string): Observable<any> {
    return this.baseService.delete(`${this.adaptive}/domains/${domainSlug}/models/${encodeURIComponent(modelSlug)}?appSlug=${encodeURIComponent(appSlug)}`, true, {});
  }

  // ─── Primitive Templates ───────────────────────────────────────────────────
  getPrimitives(): Observable<any> {
    return this.baseService.get(`${this.adaptive}/primitives`, true);
  }

  getPrimitiveSchema(type: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/primitives/${type}`, true);
  }

  // ─── Component Definitions ────────────────────────────────────────────────
  getComponents(domainSlug: string, appSlug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/components`, true);
  }

  getComponent(domainSlug: string, appSlug: string, compId: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/components/${compId}`, true);
  }

  createComponent(domainSlug: string, appSlug: string, payload: any): Observable<any> {
    return this.baseService.post(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/components`, true, payload);
  }

  updateComponent(domainSlug: string, appSlug: string, compId: string, payload: any): Observable<any> {
    return this.baseService.put(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/components/${compId}`, true, payload);
  }

  deleteComponent(domainSlug: string, appSlug: string, compId: string): Observable<any> {
    return this.baseService.delete(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/components/${compId}`, true, {});
  }

  // ─── App Pages ────────────────────────────────────────────────────────────
  getPages(domainSlug: string, appSlug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/pages`, true);
  }

  createPage(domainSlug: string, appSlug: string, payload: { name: string; slug?: string; order?: number }): Observable<any> {
    return this.baseService.post(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/pages`, true, payload);
  }

  updatePage(domainSlug: string, appSlug: string, pageId: string, payload: any): Observable<any> {
    return this.baseService.put(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/pages/${pageId}`, true, payload);
  }

  deletePage(domainSlug: string, appSlug: string, pageId: string): Observable<any> {
    return this.baseService.delete(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/pages/${pageId}`, true, {});
  }

  // ─── Page Layout ──────────────────────────────────────────────────────────
  getPageLayout(domainSlug: string, appSlug: string, pageId: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/pages/${pageId}/layout`, true);
  }

  savePageLayout(domainSlug: string, appSlug: string, pageId: string, layout: any[]): Observable<any> {
    return this.baseService.put(`${this.adaptive}/domains/${domainSlug}/apps/${appSlug}/pages/${pageId}/layout`, true, { layout });
  }

  // ─── Domain Records ───────────────────────────────────────────────────────
  getRecords(domainSlug: string, appSlug: string, modelSlug: string): Observable<any> {
    return this.baseService.get(`${this.adaptive}/domains/${domainSlug}/models/${modelSlug}/records?appSlug=${encodeURIComponent(appSlug)}`, true);
  }

  createRecord(domainSlug: string, appSlug: string, modelSlug: string, data: any): Observable<any> {
    return this.baseService.post(`${this.adaptive}/domains/${domainSlug}/models/${modelSlug}/records?appSlug=${encodeURIComponent(appSlug)}`, true, { data });
  }

  updateRecord(domainSlug: string, appSlug: string, modelSlug: string, recordId: string, data: any): Observable<any> {
    return this.baseService.put(`${this.adaptive}/domains/${domainSlug}/models/${modelSlug}/records/${recordId}?appSlug=${encodeURIComponent(appSlug)}`, true, { data });
  }

  deleteRecord(domainSlug: string, appSlug: string, modelSlug: string, recordId: string): Observable<any> {
    return this.baseService.delete(`${this.adaptive}/domains/${domainSlug}/models/${modelSlug}/records/${recordId}?appSlug=${encodeURIComponent(appSlug)}`, true, {});
  }
}

export const _domainService = [
  { provide: DomainService, useClass: DomainService, multi: true },
];
