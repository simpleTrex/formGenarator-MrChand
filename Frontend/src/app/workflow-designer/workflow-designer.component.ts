import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkflowService } from '../services/workflow.service';

interface CanvasState {
    id: string;
    name: string;
    description: string;
    isInitial: boolean;
    isFinal: boolean;
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
}

@Component({
    selector: 'app-workflow-designer',
    templateUrl: './workflow-designer.component.html',
    styleUrls: ['./workflow-designer.component.css']
})
export class WorkflowDesignerComponent implements OnInit {
    workflowId: string = '';
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
        private workflowService: WorkflowService
    ) { }

    ngOnInit(): void {
        this.workflowId = this.route.snapshot.params['workflowId'];

        if (this.workflowId && this.workflowId !== 'new') {
            this.loadWorkflow();
        } else {
            // New workflow - start with two default states
            this.states = [
                {
                    id: 'draft',
                    name: 'Draft',
                    description: 'Initial state',
                    isInitial: true,
                    isFinal: false,
                    color: '#9E9E9E',
                    x: 100,
                    y: 100
                },
                {
                    id: 'completed',
                    name: 'Completed',
                    description: 'Final state',
                    isInitial: false,
                    isFinal: true,
                    color: '#4CAF50',
                    x: 400,
                    y: 100
                }
            ];
        }
    }

    loadWorkflow() {
        this.loading = true;
        this.workflowService.getWorkflow(this.workflowId).subscribe({
            next: (wf: any) => {
                this.workflow = wf;
                this.states = wf.states || [];
                this.transitions = wf.transitions || [];
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
            isInitial: false,
            isFinal: false,
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
                    actionType: 'SUBMIT'
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
        const workflowData = {
            states: this.states.map(s => ({
                id: s.id,
                name: s.name,
                description: s.description,
                isInitial: s.isInitial,
                isFinal: s.isFinal,
                color: s.color,
                positionX: s.x,
                positionY: s.y,
                allowedRoles: ['USER']
            })),
            transitions: this.transitions.map(t => ({
                id: t.id,
                name: t.name,
                fromState: t.fromStateId,
                toState: t.toStateId,
                actionType: t.actionType,
                allowedRoles: ['USER'],
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
            this.message = 'Use the "Create Workflow" form to save a new workflow';
        }
    }

    goBack() {
        this.router.navigate(['../../'], { relativeTo: this.route });
    }
}
