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

    createSimpleWorkflow() {
        if (!this.newWorkflow.name || !this.newWorkflow.modelId) {
            this.error = 'Please fill in name and select a model';
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

        // Create a simple two-state workflow (Draft -> Completed)
        const workflow = {
            name: this.newWorkflow.name,
            description: this.newWorkflow.description || '',
            modelId: this.newWorkflow.modelId,
            domainId: this.domain?.id, // Include domainId for OWNER users
            icon: 'workflow',
            states: [
                {
                    id: 'draft',
                    name: 'Draft',
                    description: 'Initial draft state',
                    initial: true,
                    final: false,
                    color: '#9E9E9E',
                    positionX: 100,
                    positionY: 100
                },
                {
                    id: 'completed',
                    name: 'Completed',
                    description: 'Final completed state',
                    initial: false,
                    final: true,
                    color: '#4CAF50',
                    positionX: 400,
                    positionY: 100
                }
            ],
            transitions: [
                {
                    id: 'complete',
                    name: 'Complete',
                    fromState: 'draft',
                    toState: 'completed',
                    actionType: 'COMPLETE',
                    allowedRoles: ['USER'],
                    requiredFields: []
                }
            ]
        };

        console.log('Sending Workflow Payload:', workflow);

        this.loading = true;
        this.workflowService.createWorkflow(workflow).subscribe({
            next: () => {
                this.message = 'Workflow created successfully!';
                this.error = '';
                this.showCreateForm = false;
                this.loadWorkflows();
            },
            error: (err) => {
                console.error('Create failed:', err);
                this.error = err?.error?.message || 'Failed to create workflow';
                this.loading = false;
            }
        });
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
                this.error = err?.error?.message || 'Failed to delete workflow';
            }
        });
    }
}
