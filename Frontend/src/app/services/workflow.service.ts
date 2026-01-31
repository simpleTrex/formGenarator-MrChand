import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { BaseService } from './base.service';

@Injectable({
    providedIn: 'root'
})
export class WorkflowService {
    private baseUrl = `${environment.api}/workflows`;

    constructor(private baseService: BaseService) { }

    // Get all workflows for current domain
    getWorkflows(domainId?: string): Observable<any[]> {
        let url = this.baseUrl;
        if (domainId) {
            url += `?domainId=${domainId}`;
        }
        return this.baseService.get(url, true);
    }

    // Get a specific workflow
    getWorkflow(id: string, domainId?: string): Observable<any> {
        let url = `${this.baseUrl}/${id}`;
        if (domainId) {
            url += `?domainId=${domainId}`;
        }
        return this.baseService.get(url, true);
    }

    // Create a new workflow
    createWorkflow(workflow: any): Observable<any> {
        return this.baseService.post(this.baseUrl, true, workflow);
    }

    // Update a workflow
    updateWorkflow(id: string, workflow: any): Observable<any> {
        return this.baseService.put(`${this.baseUrl}/${id}`, true, workflow);
    }

    // Delete a workflow
    deleteWorkflow(id: string, domainId?: string): Observable<any> {
        let url = `${this.baseUrl}/${id}`;
        if (domainId) {
            url += `?domainId=${domainId}`;
        }
        return this.baseService.delete(url, true, {});
    }

    // Create a workflow instance
    createInstance(workflowId: string, data: any): Observable<any> {
        return this.baseService.post(`${this.baseUrl}/${workflowId}/instances`, true, data);
    }

    // Execute a transition
    executeTransition(instanceId: string, transitionId: string, data: any): Observable<any> {
        return this.baseService.post(
            `${this.baseUrl}/instances/${instanceId}/transitions/${transitionId}`,
            true,
            data
        );
    }

    // Get available actions for an instance
    getAvailableActions(instanceId: string): Observable<any> {
        return this.baseService.get(`${this.baseUrl}/instances/${instanceId}/actions`, true);
    }

    // Get instance details
    getInstance(instanceId: string): Observable<any> {
        return this.baseService.get(`${this.baseUrl}/instances/${instanceId}`, true);
    }

    // Get my tasks
    getMyTasks(): Observable<any[]> {
        return this.baseService.get(`${this.baseUrl}/instances/my-tasks`, true);
    }
}
