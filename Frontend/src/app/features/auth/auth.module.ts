import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { LoginComponent } from './pages/login/login.component';
import { OwnerSignupComponent } from './pages/owner-signup/owner-signup.component';
import { DomainLoginComponent } from './pages/domain-login/domain-login.component';
import { DomainSignupComponent } from './pages/domain-signup/domain-signup.component';

const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'owner-login', component: LoginComponent },
    { path: 'owner-signup', component: OwnerSignupComponent },
    { path: 'domain-login', component: DomainLoginComponent },
    { path: 'domain-signup', component: DomainSignupComponent },
];

@NgModule({
    declarations: [
        LoginComponent,
        OwnerSignupComponent,
        DomainLoginComponent,
        DomainSignupComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(routes),
    ],
})
export class AuthModule { }
