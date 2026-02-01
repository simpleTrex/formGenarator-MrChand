import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { DomainService } from '../services/domain.service';
import { WorkflowService } from '../services/workflow.service';

@Component({
    selector: 'app-app-workflows',
    templateUrl: './app-workflows.component.html',
    styleUrls: ['./app-workflows.component.css']
})
export class AppWorkflowsComponent implements OnInit {
    domainSlug = '';
    appSlug = '';

    domain: any = null;
    app: any = null;

    workflows: any[] = [];
    models: any[] = [];
    loading = false;
    error = '';
    message = '';

    showCreateForm = false;

    // Simple workflow form
    newWorkflow = {
        name: '',
        description: '',
        modelId: ''
    };

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private domainService: DomainService,
        private workflowService: WorkflowService,
        public auth: AuthService
    ) { }

    ngOnInit(): void {
        this.domainSlug = this.route.snapshot.params['slug'];
        this.appSlug = this.route.snapshot.params['appSlug'];

        if (!this.domainSlug || !this.appSlug) {
            this.error = 'Missing domain/app context';
            return;
        }

        this.loadContext();
    }

    goBackToApp(): void {
        this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
    }

    private loadContext() {
        this.loading = true;
        this.error = '';

        this.domainService.getBySlug(this.domainSlug).subscribe({
            next: (domain) => {
                this.domain = domain;
                // Ensure id is set (MongoDB returns _id usually)
                if (this.domain && !this.domain.id && this.domain._id) {
                    this.domain.id = this.domain._id;
                }

                this.domainService.getApplication(this.domainSlug, this.appSlug).subscribe({
                    next: (app) => {
                        this.app = app;
                        this.loadModelsAndWorkflows();
                    },
                    error: (err) => {
                        this.loading = false;
                        this.error = err?.error?.message || 'Application not found';
                    }
                });
            },
            error: (err) => {
                this.loading = false;
                this.error = err?.error?.message || 'Domain not found';
            }
        });
    }

    private loadModelsAndWorkflows() {
        // Load models first
        this.domainService.getDomainModels(this.domainSlug, this.appSlug).subscribe({
            next: (models) => {
                this.models = models || [];
                this.loadWorkflows();
            },
            error: () => {
                this.models = [];
                this.loadWorkflows();
            }
        });
    }

    private loadWorkflows() {
        // Pass domainId if available (required for OWNER)
        const domainId = this.domain?.id;
        this.workflowService.getWorkflows(domainId).subscribe({
            next: (workflows) => {
                this.workflows = workflows || [];
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading workflows:', err);
                this.workflows = [];
                this.loading = false;
            }
        });
    }

    toggleCreateForm() {
        this.showCreateForm = !this.showCreateForm;
        this.error = '';
        this.message = '';
        if (this.showCreateForm) {
            this.newWorkflow = { name: '', description: '', modelId: '' };
        }
    }

    createWorkflow() {
        if (!this.newWorkflow.name || !this.newWorkflow.modelId) {
            alert('Name and Model are required');
            return;
        }

        console.log('Current Domain Object:', this.domain);
        if (!this.domain || !this.domain.id) {
            console.error('Domain ID missing!', this.domain);
            // Fallback: try to find id from _id or similar if structure is different
            if (this.domain && this.domain._id) {
                this.domain.id = this.domain._id;
            }
        }

        const workflow = {
            name: this.newWorkflow.name,
            description: this.newWorkflow.description || '',
            modelId: this.newWorkflow.modelId,
            domainId: this.domain?.id,
            icon: 'workflow',
            states: [
                {
                    id: 'draft',
                    name: 'Draft',
                    description: 'Initial state',
                    isInitial: true,
                    isFinal: false,
                    color: '#808080',
                    positionX: 50,
                    positionY: 100,
                    permissions: {}
                },
                {
                    id: 'completed',
                    name: 'Completed',
                    description: 'Final state',
                    isInitial: false,
                    isFinal: true,
                    color: '#28a745',
                    positionX: 400,
                    positionY: 100,
                    permissions: {}
                }
            ],
            transitions: [
                {
                    id: 'submit',
                    name: 'Submit',
                    description: 'Submit for completion',
                    fromState: 'draft',
                    toState: 'completed',
                    actionType: 'approve',
                    requiredRole: '',
                    allowedRoles: []
                }
            ]
        };

        this.workflowService.createWorkflow(workflow).subscribe({
            next: () => {
                this.showCreateForm = false; // Changed from showCreateModal
                this.newWorkflow = { name: '', description: '', modelId: '' };
                this.loadWorkflows();
            },
            error: (err) => console.error(err)
        });
    }

    // seedWorkflows functionality removed

    // Run Dialog
    showRunDialog = false;
    selectedWorkflow: any = null;
    newInstanceData = { recordId: '', note: '' };

    openRunDialog(workflow: any) {
        this.selectedWorkflow = workflow;
        this.newInstanceData = {
            recordId: `${workflow.modelId}-${Math.floor(Math.random() * 10000)}`,
            note: 'Generated from UI'
        };
        this.showRunDialog = true;
    }

    closeRunDialog() {
        this.showRunDialog = false;
        this.selectedWorkflow = null;
    }

    startInstance() {
        if (!this.selectedWorkflow) return;

        const data = {
            recordId: this.newInstanceData.recordId,
            data: {
                initialNote: this.newInstanceData.note,
                requester: this.auth.currentUser?.username || 'User'
            }
        };

        this.workflowService.createInstance(this.selectedWorkflow.id, data).subscribe({
            next: (instance) => {
                this.showRunDialog = false;
                // Navigate to instance view
                this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'tasks', 'instance', instance.id]);
            },
            error: (err) => {
                alert('Failed to start instance: ' + (err.error?.message || err.message));
            }
        });
    }

    // Designer navigation
    openDesigner(workflow: any) {
        if (!confirm(`Open workflow "${workflow.name}" in designer?`)) { // Changed confirmation message
            return;
        }

        const domainId = this.domain?.id;
        this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'workflows', 'designer', workflow.id]);
    }

    editWorkflow(workflow: any) {
        alert('Edit functionality coming soon! Use Designer for structural changes.');
        // TODO: logical edit (rename etc)
    }

    deleteWorkflow(workflow: any) {
        if (!confirm(`Delete workflow "${workflow.name}"?`)) {
            return;
        }

        const domainId = this.domain?.id;
        this.workflowService.deleteWorkflow(workflow.id, domainId).subscribe({
            next: () => {
                this.message = 'Workflow deleted';
                this.loadWorkflows();
            },
            error: (err) => {
                this.error = 'Failed to delete workflow: ' + (err.error?.message || err.message);
            }
        });
    }
}
