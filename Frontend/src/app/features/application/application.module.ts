import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ModernButtonComponent } from '../../shared/components/modern-button/modern-button.component';
import { ModernCardComponent } from '../../shared/components/modern-card/modern-card.component';
import { AppHomeComponent } from './pages/app-home/app-home.component';
import { ProcessBuilderComponent } from './pages/process-builder/process-builder.component';
import { InstanceListComponent } from './pages/instance-list/instance-list.component';
import { InstanceViewComponent } from './pages/instance-view/instance-view.component';
import { StartWorkflowComponent } from './pages/start-workflow/start-workflow.component';
import { WorkflowExplainerComponent } from './pages/workflow-explainer/workflow-explainer.component';

const routes: Routes = [
    { path: ':appSlug',                          component: AppHomeComponent },
    { path: ':appSlug/start',                    component: StartWorkflowComponent },
    { path: ':appSlug/workflows',                component: ProcessBuilderComponent },
    { path: ':appSlug/workflows/builder',        component: ProcessBuilderComponent },
    { path: ':appSlug/workflows/explainer',      component: WorkflowExplainerComponent },
    { path: ':appSlug/tasks',                    component: InstanceListComponent, data: { mode: 'tasks' } },
    { path: ':appSlug/instances',                component: InstanceListComponent, data: { mode: 'instances' } },
    { path: ':appSlug/instances/:instanceId',     component: InstanceViewComponent },
    // Legacy paths kept for compatibility.
    { path: ':appSlug/process',                  component: ProcessBuilderComponent },
    { path: ':appSlug/processes',                component: ProcessBuilderComponent },
];

@NgModule({
    declarations: [
        AppHomeComponent,
        ProcessBuilderComponent,
        WorkflowExplainerComponent,
        InstanceListComponent,
        InstanceViewComponent,
        StartWorkflowComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(routes),
        ModernCardComponent,
        ModernButtonComponent,
    ],
})
export class ApplicationModule { }
