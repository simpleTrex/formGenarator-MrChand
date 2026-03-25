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

  getProcess(slug: string, appSlug: string, processSlug: string): Observable<ProcessDefinitionResponse> {
    return this.api.get(`${this.base(slug, appSlug)}/processes/${processSlug}`, true);
  }

  listProcesses(slug: string, appSlug: string): Observable<ProcessDefinitionResponse[]> {
    return this.api.get(`${this.base(slug, appSlug)}/processes`, true);
  }

  createProcess(slug: string, appSlug: string, processSlug: string, payload: any): Observable<ProcessDefinitionResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/processes/${processSlug}`, true, payload);
  }

  updateProcess(slug: string, appSlug: string, processSlug: string, payload: any): Observable<ProcessDefinitionResponse> {
    return this.api.put(`${this.base(slug, appSlug)}/processes/${processSlug}`, true, payload);
  }

  deleteProcess(slug: string, appSlug: string, processSlug: string): Observable<any> {
    return this.api.delete(`${this.base(slug, appSlug)}/processes/${processSlug}`, true, null);
  }

  publishProcess(slug: string, appSlug: string, processSlug: string): Observable<ProcessDefinitionResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/processes/${processSlug}/publish`, true, {});
  }

  archiveProcess(slug: string, appSlug: string, processSlug: string): Observable<any> {
    return this.api.post(`${this.base(slug, appSlug)}/processes/${processSlug}/archive`, true, {});
  }

  // ── Process Instances ─────────────────────────────────────────────────────

  startProcess(slug: string, appSlug: string, processSlug: string): Observable<ProcessInstanceResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/processes/${processSlug}/instances/start`, true, {});
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
