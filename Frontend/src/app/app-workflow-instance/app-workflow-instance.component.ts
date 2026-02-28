import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkflowService } from '../services/workflow.service';
import { AuthService } from '../services/auth.service';

@Component({
    selector: 'app-workflow-instance',
    templateUrl: './app-workflow-instance.component.html',
    styleUrls: ['./app-workflow-instance.component.css']
})
export class AppWorkflowInstanceComponent implements OnInit {
    instanceId = '';
    instance: any = null;
    availableTransitions: any[] = [];
    loading = true;
    error = '';
    processing = false;

    // For action dialog
    showActionDialog = false;
    selectedTransition: any = null;
    actionComment = '';

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private workflowService: WorkflowService,
        public auth: AuthService
    ) { }

    ngOnInit(): void {
        this.instanceId = this.route.snapshot.params['instanceId'];
        if (this.instanceId) {
            this.loadInstance();
        } else {
            this.error = 'Instance ID missing';
            this.loading = false;
        }
    }

    loadInstance() {
        this.loading = true;
        this.workflowService.getInstance(this.instanceId).subscribe({
            next: (instance) => {
                this.instance = instance;
                this.loadActions();
            },
            error: (err) => {
                this.error = 'Failed to load instance';
                this.loading = false;
                console.error(err);
            }
        });
    }

    loadActions() {
        this.workflowService.getAvailableActions(this.instanceId).subscribe({
            next: (res) => {
                this.availableTransitions = res.availableTransitions || [];
                this.loading = false;
            },
            error: (err) => {
                console.error('Failed to load actions', err);
                this.loading = false;
            }
        });
    }

    initiateAction(transition: any) {
        this.selectedTransition = transition;
        this.actionComment = '';
        this.showActionDialog = true;
    }

    confirmAction() {
        if (!this.selectedTransition) return;

        this.processing = true;
        this.workflowService.executeTransition(
            this.instanceId,
            this.selectedTransition.id,
            { comment: this.actionComment }
        ).subscribe({
            next: (updatedInstance) => {
                this.instance = updatedInstance;
                this.processing = false;
                this.showActionDialog = false;
                this.selectedTransition = null;
                // Reload actions as state changed
                this.loadActions();
            },
            error: (err) => {
                alert('Action failed: ' + (err.error?.message || err.message));
                this.processing = false;
            }
        });
    }

    cancelAction() {
        this.showActionDialog = false;
        this.selectedTransition = null;
    }

    goBack() {
        this.router.navigate(['../../'], { relativeTo: this.route });
    }

    // Helper to format data keys
    formatKey(key: string): string {
        return key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
    }

    // Helper to get object keys
    getKeys(obj: any): string[] {
        return obj ? Object.keys(obj) : [];
    }
}
