import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { BaseService } from './api.service';
import {
  ExecuteEdgeResponse,
  HistoryResponse,
  ProcessDefinition,
  ProcessDefinitionResponse,
  ProcessInstance,
  StepViewResponse,
  TaskListResponse,
  WorkflowInstanceResponse,
} from '../models/process.model';

@Injectable({ providedIn: 'root' })
export class ProcessService {

  private base(slug: string, appSlug: string): string {
    return `${environment.adaptiveApi}/domains/${slug}/apps/${appSlug}`;
  }

  // ── Workflow Definition ───────────────────────────────────────────────────

  listWorkflows(slug: string, appSlug: string): Observable<ProcessDefinition[]> {
    return this.api.get(`${this.base(slug, appSlug)}/workflows`, true);
  }

  getWorkflow(slug: string, appSlug: string, workflowSlug: string, version?: number): Observable<ProcessDefinitionResponse> {
    const query = version != null ? `?version=${version}` : '';
    return this.api.get(`${this.base(slug, appSlug)}/workflows/${encodeURIComponent(workflowSlug)}${query}`, true);
  }

  createWorkflow(slug: string, appSlug: string, payload: any): Observable<ProcessDefinitionResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/workflows`, true, payload);
  }

  updateWorkflow(slug: string, appSlug: string, workflowSlug: string, payload: any): Observable<ProcessDefinitionResponse> {
    return this.api.put(`${this.base(slug, appSlug)}/workflows/${encodeURIComponent(workflowSlug)}`, true, payload);
  }

  deleteWorkflow(slug: string, appSlug: string, workflowSlug: string): Observable<any> {
    return this.api.delete(`${this.base(slug, appSlug)}/workflows/${encodeURIComponent(workflowSlug)}`, true, null);
  }

  publishWorkflow(slug: string, appSlug: string, workflowSlug: string): Observable<ProcessDefinitionResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/workflows/${encodeURIComponent(workflowSlug)}/publish`, true, {});
  }

  archiveWorkflow(slug: string, appSlug: string, workflowSlug: string): Observable<any> {
    return this.api.post(`${this.base(slug, appSlug)}/workflows/${encodeURIComponent(workflowSlug)}/archive`, true, {});
  }

  // ── Workflow Runtime ──────────────────────────────────────────────────────

  startWorkflow(slug: string, appSlug: string, workflowSlug: string, formData?: Record<string, any>): Observable<WorkflowInstanceResponse> {
    return this.api.post(
      `${this.base(slug, appSlug)}/workflows/${encodeURIComponent(workflowSlug)}/start`,
      true,
      { formData: formData || {} },
    );
  }

  listInstances(slug: string, appSlug: string): Observable<ProcessInstance[]> {
    return this.api.get(`${this.base(slug, appSlug)}/instances`, true);
  }

  listMyTasks(slug: string, appSlug: string): Observable<TaskListResponse> {
    return this.api.get(`${this.base(slug, appSlug)}/instances/my-tasks`, true);
  }

  listMyStartedInstances(slug: string, appSlug: string): Observable<ProcessInstance[]> {
    return this.api.get(`${this.base(slug, appSlug)}/instances/my-started`, true);
  }

  getInstance(slug: string, appSlug: string, instanceId: string): Observable<ProcessInstance> {
    return this.api.get(`${this.base(slug, appSlug)}/instances/${instanceId}`, true);
  }

  getStepView(slug: string, appSlug: string, instanceId: string): Observable<StepViewResponse> {
    return this.api.get(`${this.base(slug, appSlug)}/instances/${instanceId}/view`, true);
  }

  executeEdge(
    slug: string,
    appSlug: string,
    instanceId: string,
    payload: { edgeId: string; formData?: Record<string, any>; comment?: string },
  ): Observable<ExecuteEdgeResponse> {
    return this.api.post(`${this.base(slug, appSlug)}/instances/${instanceId}/execute`, true, payload);
  }

  getHistory(slug: string, appSlug: string, instanceId: string): Observable<HistoryResponse> {
    return this.api.get(`${this.base(slug, appSlug)}/instances/${instanceId}/history`, true);
  }

  // ── Backward-compatible wrappers used by legacy pages while migrating. ───

  listProcesses(slug: string, appSlug: string): Observable<ProcessDefinition[]> {
    return this.listWorkflows(slug, appSlug);
  }

  createProcess(slug: string, appSlug: string, payload: any): Observable<ProcessDefinitionResponse> {
    return this.createWorkflow(slug, appSlug, payload);
  }

  updateProcess(slug: string, appSlug: string, workflowSlug: string, payload: any): Observable<ProcessDefinitionResponse> {
    return this.updateWorkflow(slug, appSlug, workflowSlug, payload);
  }

  deleteProcess(slug: string, appSlug: string, workflowSlug: string): Observable<any> {
    return this.deleteWorkflow(slug, appSlug, workflowSlug);
  }

  publishProcess(slug: string, appSlug: string, workflowSlug: string): Observable<ProcessDefinitionResponse> {
    return this.publishWorkflow(slug, appSlug, workflowSlug);
  }

  archiveProcess(slug: string, appSlug: string, workflowSlug: string): Observable<any> {
    return this.archiveWorkflow(slug, appSlug, workflowSlug);
  }

  constructor(private api: BaseService) {}
}
