import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { UtilModule } from './util/util.module';
import { RenderFormComponent } from './render-form/render-form.component';
import { DataComponent } from './data/data.component';
import { NaviBarComponent } from './navi-bar/navi-bar.component';
import { NotFoundPageComponent } from './not-found-page/not-found-page.component';
import { ModelPageComponent } from './model-page/model-page.component';
import { HomePageComponent } from './home-page/home-page.component';
import { NaviDataComponent } from './navi-data/navi-data.component';
import { ModelOptionsComponent } from './model-options/model-options.component';
import { ModelRenderComponent } from './model-render/model-render.component';
import { LoginComponent } from './login/login.component';
import { AuthService } from './services/auth.service';
import { httpInterceptorProviders } from './services/httpinterceptor';
import { AuthGuard } from './services/AuthGuard';
import { DomainCreateComponent } from './domain-create/domain-create.component';
import { DomainHomeComponent } from './domain-home/domain-home.component';
import { DomainService } from './services/domain.service';
import { DomainLoginComponent } from './domain-login/domain-login.component';
import { OwnerSignupComponent } from './owner-signup/owner-signup.component';
import { DomainSignupComponent } from './domain-signup/domain-signup.component';
import { DomainUsersComponent } from './domain-users/domain-users.component';
import { AppHomeComponent } from './app-home/app-home.component';
import { AppModelsComponent } from './app-models/app-models.component';
import { AppWorkflowsComponent } from './app-workflows/app-workflows.component';
import { WorkflowDesignerComponent } from './workflow-designer/workflow-designer.component';
import { AppWorkflowInstanceComponent } from './app-workflow-instance/app-workflow-instance.component';
import { AppMyTasksComponent } from './app-my-tasks/app-my-tasks.component';

@NgModule({
  declarations: [
    AppComponent,
    RenderFormComponent,
    DataComponent,
    NaviBarComponent,
    NotFoundPageComponent,
    ModelPageComponent,
    HomePageComponent,
    NaviDataComponent,
    ModelOptionsComponent,
    ModelRenderComponent,
    LoginComponent,
    DomainCreateComponent,
    DomainHomeComponent,
    DomainLoginComponent,
    OwnerSignupComponent,
    DomainSignupComponent,
    DomainUsersComponent,
    AppHomeComponent,
    AppModelsComponent,
    AppWorkflowsComponent,
    WorkflowDesignerComponent,
    AppMyTasksComponent,
    AppWorkflowInstanceComponent
  ],
  imports: [
    ReactiveFormsModule,
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    CommonModule,
    HttpClientModule,
    UtilModule,
    RouterModule.forRoot([
      { path: 'login', component: LoginComponent },
      { path: 'owner-login', component: LoginComponent },
      { path: 'owner-signup', component: OwnerSignupComponent },
      { path: 'domain-login', component: DomainLoginComponent },
      { path: 'domain-signup', component: DomainSignupComponent },
      { path: '', component: HomePageComponent },
      { path: 'create-domain', component: DomainCreateComponent, canActivate: [AuthGuard] },
      { path: 'domain/:slug', component: DomainHomeComponent },
      { path: 'domain/:slug/app/:appSlug', component: AppHomeComponent },
      { path: 'domain/:slug/app/:appSlug/models', component: AppModelsComponent },
      { path: 'domain/:slug/app/:appSlug/workflows', component: AppWorkflowsComponent },
      { path: 'domain/:slug/app/:appSlug/workflows/designer/:workflowId', component: WorkflowDesignerComponent },
      { path: 'domain/:slug/app/:appSlug/tasks', component: AppMyTasksComponent },
      { path: 'domain/:slug/app/:appSlug/tasks/instance/:instanceId', component: AppWorkflowInstanceComponent },
      { path: '**', component: NotFoundPageComponent },
    ])
  ],
  providers: [
    AuthService,
    AuthGuard,
    DomainService,
    httpInterceptorProviders
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
