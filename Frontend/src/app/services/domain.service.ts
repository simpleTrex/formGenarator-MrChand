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

@Injectable({ providedIn: 'root' })
export class DomainService {
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
}

export const _domainService = [
  { provide: DomainService, useClass: DomainService, multi: true },
];
