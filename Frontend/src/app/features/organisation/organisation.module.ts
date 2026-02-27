import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { DomainCreateComponent } from './pages/org-create/domain-create.component';
import { DomainHomeComponent } from './pages/org-home/domain-home.component';
import { DomainUsersComponent } from './pages/org-members/domain-users.component';

const routes: Routes = [
    { path: 'create', component: DomainCreateComponent },
    { path: ':slug', component: DomainHomeComponent },
    { path: ':slug/users', component: DomainUsersComponent },
];

@NgModule({
    declarations: [
        DomainCreateComponent,
        DomainUsersComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(routes),
        DomainHomeComponent,
    ],
})
export class OrganisationModule { }
