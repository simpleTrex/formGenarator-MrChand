import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ModernButtonComponent } from '../../shared/components/modern-button/modern-button.component';
import { ModernCardComponent } from '../../shared/components/modern-card/modern-card.component';
import { AppHomeComponent } from './pages/app-home/app-home.component';
import { AppModelsComponent } from './pages/app-models/app-models.component';
import { ProcessBuilderComponent } from './pages/process-builder/process-builder.component';
import { InstanceListComponent } from './pages/instance-list/instance-list.component';
import { InstanceViewComponent } from './pages/instance-view/instance-view.component';
import { ProcessListComponent } from './pages/process-list/process-list.component';

const routes: Routes = [
    { path: ':appSlug',                          component: AppHomeComponent },
    { path: ':appSlug/models',                   component: AppModelsComponent },
    { path: ':appSlug/process/:processSlug',     component: ProcessBuilderComponent },
    { path: ':appSlug/processes',                component: ProcessListComponent },
    { path: ':appSlug/instances',                component: InstanceListComponent },
    { path: ':appSlug/instances/:instanceId',     component: InstanceViewComponent },
];

@NgModule({
    declarations: [
        AppHomeComponent,
        AppModelsComponent,
        ProcessBuilderComponent,
        ProcessListComponent,
        InstanceListComponent,
        InstanceViewComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(routes),
        ModernCardComponent,
        ModernButtonComponent,
    ],
})
export class ApplicationModule { }
