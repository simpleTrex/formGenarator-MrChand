import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { BaseService } from './base.service';

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

  constructor(private baseService: BaseService) {}

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
}

export const _domainService = [
  { provide: DomainService, useClass: DomainService, multi: true },
];
