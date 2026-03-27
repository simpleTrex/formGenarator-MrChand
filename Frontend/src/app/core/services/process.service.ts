import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { BaseService } from './api.service';
import {
  ProcessDefinitionResponse,
  ProcessInstanceResponse,
  NodeViewResponse,
  ProcessInstance,
} from '../models/process.model';

@Injectable({ providedIn: 'root' })
export class ProcessService {

  private base(slug: string, appSlug: string): string {
    return `${environment.adaptiveApi}/domains/${slug}/apps/${appSlug}`;
  }

  // ── Process Definition (1 per app) ───────────────────────────────────────

  getProcess(slug: string, appSlug: string): Observable<ProcessDefinitionResponse> {
    return this.api.get(`${this.base(slug, appSlug)}/process`, true);
  }

  listProcesses(slug: string, appSlug: string): Observable<ProcessDefinitionResponse[]> {
    return this.getProcess(slug, appSlug).pipe(map(res => [res]));
  }

  createProcess(slug: string, appSlug: string, payload: any): Observable<ProcessDefinitionResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/process`, true, payload);
  }

  updateProcess(slug: string, appSlug: string, payload: any): Observable<ProcessDefinitionResponse> {
    return this.api.put(`${this.base(slug, appSlug)}/process`, true, payload);
  }

  deleteProcess(slug: string, appSlug: string): Observable<any> {
    return this.api.delete(`${this.base(slug, appSlug)}/process`, true, null);
  }

  publishProcess(slug: string, appSlug: string): Observable<ProcessDefinitionResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/process/publish`, true, {});
  }

  archiveProcess(slug: string, appSlug: string): Observable<any> {
    return this.api.post(`${this.base(slug, appSlug)}/process/archive`, true, {});
  }

  // ── Process Templates ─────────────────────────────────────────────────────

  listTemplates(slug: string, appSlug: string): Observable<any[]> {
    return this.api.get(`${this.base(slug, appSlug)}/process/templates`, true);
  }

  createFromTemplate(slug: string, appSlug: string, templateId: string, linkedModelIds?: string[]): Observable<ProcessDefinitionResponse> {
    const payload: any = { templateId };
    if (linkedModelIds && linkedModelIds.length > 0) {
      payload.linkedModelIds = linkedModelIds;
    }
    return this.api.post(`${this.base(slug, appSlug)}/process/from-template`, true, payload);
  }

  // ── Process Instances ─────────────────────────────────────────────────────

  startProcess(slug: string, appSlug: string): Observable<ProcessInstanceResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/instances/start`, true, {});
  }

  listInstances(slug: string, appSlug: string): Observable<ProcessInstance[]> {
    return this.api.get(`${this.base(slug, appSlug)}/instances`, true);
  }

  getInstance(slug: string, appSlug: string, instanceId: string): Observable<ProcessInstance> {
    return this.api.get(`${this.base(slug, appSlug)}/instances/${instanceId}`, true);
  }

  getNodeView(slug: string, appSlug: string, instanceId: string): Observable<NodeViewResponse> {
    return this.api.get(`${this.base(slug, appSlug)}/instances/${instanceId}/current-node`, true);
  }

  submitNode(slug: string, appSlug: string, instanceId: string, payload: any): Observable<ProcessInstanceResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/instances/${instanceId}/submit`, true, payload);
  }

  saveDraft(slug: string, appSlug: string, instanceId: string, nodeId: string, data: any): Observable<any> {
    return this.api.post(`${this.base(slug, appSlug)}/instances/${instanceId}/save-draft`, true, { nodeId, data });
  }

  cancelInstance(slug: string, appSlug: string, instanceId: string): Observable<any> {
    return this.api.post(`${this.base(slug, appSlug)}/instances/${instanceId}/cancel`, true, {});
  }

  constructor(private api: BaseService) {}
}
