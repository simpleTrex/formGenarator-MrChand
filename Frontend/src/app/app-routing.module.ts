import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { ArchitectureComponent } from './features/architecture/architecture.component';
import { WorkflowExplainerComponent } from './features/application/pages/workflow-explainer/workflow-explainer.component';

const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule),
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule),
  },
  // Legacy routes — redirect to new auth paths
  { path: 'login', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'owner-login', redirectTo: 'auth/owner-login', pathMatch: 'full' },
  { path: 'owner-signup', redirectTo: 'auth/owner-signup', pathMatch: 'full' },
  { path: 'domain-login', redirectTo: 'auth/domain-login', pathMatch: 'full' },
  { path: 'domain-signup', redirectTo: 'auth/domain-signup', pathMatch: 'full' },
  {
    path: 'create-domain',
    redirectTo: 'domain/create',
    pathMatch: 'full',
  },
  {
    path: 'domain',
    loadChildren: () => import('./features/organisation/organisation.module').then(m => m.OrganisationModule),
    canActivate: [AuthGuard],
    data: { principalTypes: ['OWNER', 'DOMAIN_USER'] },
  },
  {
    path: 'domain/:slug/app',
    loadChildren: () => import('./features/application/application.module').then(m => m.ApplicationModule),
    canActivate: [AuthGuard],
    data: { principalTypes: ['OWNER', 'DOMAIN_USER'] },
  },
  {
    path: 'form-builder',
    loadChildren: () => import('./features/form-builder/form-builder.module').then(m => m.FormBuilderModule),
    canActivate: [AuthGuard],
  },
  { path: 'architecture', component: ArchitectureComponent },
  { path: 'workflow', component: WorkflowExplainerComponent },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule { }
