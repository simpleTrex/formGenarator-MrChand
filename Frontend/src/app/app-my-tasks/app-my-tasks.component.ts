import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomainService } from '../services/domain.service';
import { WorkflowService } from '../services/workflow.service';
import { AuthService } from '../services/auth.service';

@Component({
    selector: 'app-my-tasks',
    templateUrl: './app-my-tasks.component.html',
    styleUrls: ['./app-my-tasks.component.css']
})
export class AppMyTasksComponent implements OnInit {
    domainSlug = '';
    appSlug = '';

    tasks: any[] = [];
    loading = true;
    error = '';

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private workflowService: WorkflowService,
        public auth: AuthService
    ) { }

    ngOnInit(): void {
        this.domainSlug = this.route.snapshot.params['slug'];
        this.appSlug = this.route.snapshot.params['appSlug'];
        this.loadTasks();
    }

    loadTasks() {
        this.loading = true;
        this.workflowService.getMyTasks().subscribe({
            next: (tasks: any[]) => {
                this.tasks = tasks;
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Failed to load tasks';
                this.loading = false;
                console.error(err);
            }
        });
    }

    openTask(task: any) {
        this.router.navigate(['instance', task.id], { relativeTo: this.route });
    }

    goBack() {
        this.router.navigate(['../../'], { relativeTo: this.route });
    }
}
