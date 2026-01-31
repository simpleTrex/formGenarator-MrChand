import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomainService } from '../services/domain.service';
import { WorkflowService } from '../services/workflow.service';

interface CanvasState {
    id: string;
    name: string;
    description: string;
    initial: boolean;
    final: boolean;
    color: string;
    x: number;
    y: number;
}

interface CanvasTransition {
    id: string;
    name: string;
    fromStateId: string;
    toStateId: string;
    actionType: string;
    allowedRoles: string; // Comma separated string for UI
}

@Component({
    selector: 'app-workflow-designer',
    templateUrl: './workflow-designer.component.html',
    styleUrls: ['./workflow-designer.component.css']
})
export class WorkflowDesignerComponent implements OnInit {
    workflowId: string = '';
    domainSlug: string = '';
    domainId: string = '';

    workflow: any = null;

    states: CanvasState[] = [];
    transitions: CanvasTransition[] = [];

    selectedState: CanvasState | null = null;
    selectedTransition: CanvasTransition | null = null;

    // Connection mode
    connectMode = false;
    connectFromState: CanvasState | null = null;

    // Dragging
    draggedState: CanvasState | null = null;
    dragOffsetX = 0;
    dragOffsetY = 0;

    error = '';
    message = '';
    loading = false;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private domainService: DomainService,
        private workflowService: WorkflowService
    ) { }

    ngOnInit(): void {
        // Get params from parent routes if necessary using pathFromRoot or assuming proper hierarchy
        // Actually, for a child route, we might need to look at parent params
        this.domainSlug = this.route.snapshot.paramMap.get('slug') ||
            this.route.parent?.snapshot.paramMap.get('slug') || '';

        this.workflowId = this.route.snapshot.params['workflowId'];

        if (this.domainSlug) {
            this.loadDomainAndWorkflow();
        } else {
            this.error = 'Domain context missing';
        }
    }

    loadDomainAndWorkflow() {
        this.loading = true;
        this.domainService.getBySlug(this.domainSlug).subscribe({
            next: (domain: any) => {
                this.domainId = domain.id || domain._id;
                if (this.workflowId && this.workflowId !== 'new') {
                    this.loadWorkflow();
                } else {
                    this.initializeNewWorkflow();
                    this.loading = false;
                }
            },
            error: (err) => {
                this.error = 'Failed to load domain context';
                this.loading = false;
            }
        });
    }

    initializeNewWorkflow() {
        this.states = [
            {
                id: 'draft',
                name: 'Draft',
                description: 'Initial state',
                initial: true,
                final: false,
                color: '#9E9E9E',
                x: 100,
                y: 100
            },
            {
                id: 'completed',
                name: 'Completed',
                description: 'Final state',
                initial: false,
                final: true,
                color: '#4CAF50',
                x: 400,
                y: 100
            }
        ];
    }

    loadWorkflow() {
        this.workflowService.getWorkflow(this.workflowId, this.domainId).subscribe({
            next: (wf: any) => {
                this.workflow = wf;
                // Map backend properties (initial/final) to canvas state
                this.states = (wf.states || []).map((s: any) => ({
                    ...s,
                    initial: s.initial !== undefined ? s.initial : s.isInitial,
                    final: s.final !== undefined ? s.final : s.isFinal,
                    x: s.positionX || 100, // Map positionX back to x
                    y: s.positionY || 100  // Map positionY back to y
                }));
                this.transitions = (wf.transitions || []).map((t: any) => ({
                    ...t,
                    fromStateId: t.fromState, // Map backend fromState to fromStateId
                    toStateId: t.toState,      // Map backend toState to toStateId
                    allowedRoles: (t.allowedRoles || ['USER']).join(', ') // Convert to string
                }));
                this.loading = false;
            },
            error: (err: any) => {
                this.error = err?.error?.message || 'Failed to load workflow';
                this.loading = false;
            }
        });
    }

    // Add new state
    addState() {
        const newState: CanvasState = {
            id: 'state_' + Date.now(),
            name: 'New State',
            description: '',
            initial: false,
            final: false,
            color: '#2196F3',
            x: 200 + (this.states.length * 50),
            y: 200
        };
        this.states.push(newState);
        this.selectState(newState);
    }

    // Select state
    selectState(state: CanvasState) {
        this.selectedState = state;
        this.selectedTransition = null;
    }

    // Delete state
    deleteState(state: CanvasState) {
        if (!confirm(`Delete state "${state.name}"?`)) {
            return;
        }

        // Remove transitions connected to this state
        this.transitions = this.transitions.filter(
            t => t.fromStateId !== state.id && t.toStateId !== state.id
        );

        // Remove state
        this.states = this.states.filter(s => s.id !== state.id);

        if (this.selectedState?.id === state.id) {
            this.selectedState = null;
        }
    }

    // Start connecting states
    startConnect() {
        this.connectMode = true;
        this.connectFromState = null;
    }

    // Click state in connect mode
    onStateClickInConnectMode(state: CanvasState) {
        if (!this.connectMode) return;

        if (!this.connectFromState) {
            this.connectFromState = state;
        } else {
            // Create connection
            if (this.connectFromState.id !== state.id) {
                const transition: CanvasTransition = {
                    id: 'trans_' + Date.now(),
                    name: 'Transition',
                    fromStateId: this.connectFromState.id,
                    toStateId: state.id,
                    actionType: 'SUBMIT',
                    allowedRoles: 'USER'
                };
                this.transitions.push(transition);
            }

            // Reset
            this.connectMode = false;
            this.connectFromState = null;
        }
    }

    // Delete transition
    deleteTransition(transition: CanvasTransition) {
        this.transitions = this.transitions.filter(t => t.id !== transition.id);
        if (this.selectedTransition?.id === transition.id) {
            this.selectedTransition = null;
        }
    }

    // Dragging
    onMouseDown(event: MouseEvent, state: CanvasState) {
        if (this.connectMode) return;

        event.preventDefault();
        this.draggedState = state;
        this.dragOffsetX = event.clientX - state.x;
        this.dragOffsetY = event.clientY - state.y;
    }

    onMouseMove(event: MouseEvent) {
        if (this.draggedState) {
            this.draggedState.x = event.clientX - this.dragOffsetX;
            this.draggedState.y = event.clientY - this.dragOffsetY;
        }
    }

    onMouseUp() {
        this.draggedState = null;
    }

    // Get transition line coordinates
    getTransitionLine(transition: CanvasTransition) {
        const fromState = this.states.find(s => s.id === transition.fromStateId);
        const toState = this.states.find(s => s.id === transition.toStateId);

        if (!fromState || !toState) return null;

        return {
            x1: fromState.x + 75,  // Center of state box (150px wide / 2)
            y1: fromState.y + 30,  // Center of state box (60px tall / 2)
            x2: toState.x + 75,
            y2: toState.y + 30
        };
    }

    // Save workflow
    saveWorkflow() {
        // Update workflow metadata if dirty (not implemented in UI yet but good practice)
        // Here we just update structure

        const workflowData = {
            ...this.workflow, // Keep existing metadata
            domainId: this.domainId, // Ensure domainId is sent for OWNER
            states: this.states.map(s => ({
                id: s.id,
                name: s.name,
                description: s.description,
                initial: s.initial,
                final: s.final,
                color: s.color,
                positionX: s.x,
                positionY: s.y
                // No allowedRoles here!
            })),
            transitions: this.transitions.map(t => ({
                id: t.id,
                name: t.name,
                fromState: t.fromStateId,
                toState: t.toStateId,
                actionType: t.actionType,
                allowedRoles: t.allowedRoles.split(',').map(r => r.trim()).filter(r => r.length > 0), // Split string
                requiredFields: []
            }))
        };

        if (this.workflowId && this.workflowId !== 'new') {
            // Update existing
            this.workflowService.updateWorkflow(this.workflowId, workflowData).subscribe({
                next: () => {
                    this.message = 'Workflow updated!';
                    this.error = '';
                },
                error: (err: any) => {
                    this.error = err?.error?.message || 'Failed to save';
                }
            });
        } else {
            // We generally wouldn't be in designer for 'new' unless we implemented create-first logic
            // But if we support it:
            this.workflowService.createWorkflow(workflowData).subscribe({
                next: (res) => {
                    this.message = 'Workflow created!';
                    this.workflowId = res.id;
                    this.workflow = res;
                },
                error: (err) => {
                    this.error = err?.error?.message || 'Failed to save';
                }
            });
        }
    }

    goBack() {
        // Navigate back to workflow list
        this.router.navigate(['../../'], { relativeTo: this.route });
    }
}
