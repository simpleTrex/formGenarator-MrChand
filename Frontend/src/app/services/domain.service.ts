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
}

export const _domainService = [
  { provide: DomainService, useClass: DomainService, multi: true },
];
