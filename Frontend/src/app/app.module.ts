import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { UtilModule } from './util/util.module';
import { RenderFormComponent } from './render-form/render-form.component';
import { DataComponent } from './data/data.component';
import { NaviBarComponent } from './navi-bar/navi-bar.component';
import { NotFoundPageComponent } from './not-found-page/not-found-page.component';
import { RouterModule } from '@angular/router';
import { ModelPageComponent } from './model-page/model-page.component';
import { HomePageComponent } from './home-page/home-page.component';
import { NaviDataComponent } from './navi-data/navi-data.component';
import { ModelOptionsComponent } from './model-options/model-options.component';
import { ModelRenderComponent } from './model-render/model-render.component';
import { LoginComponent } from './login/login.component';
import { AuthService } from './services/auth.service';
import { CommonModule } from '@angular/common';
import { httpInterceptorProviders } from './services/httpinterceptor';
import { AuthGuard } from './services/AuthGuard';
import { DomainCreateComponent } from './domain-create/domain-create.component';
import { DomainHomeComponent } from './domain-home/domain-home.component';
import { DomainService } from './services/domain.service';

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
      { path: '', component: HomePageComponent, canActivate: [AuthGuard] },
      { path: 'create-domain', component: DomainCreateComponent, canActivate: [AuthGuard] },
      { path: 'domain/:slug', component: DomainHomeComponent, canActivate: [AuthGuard] },
      { path: 'model', component: ModelPageComponent , canActivate: [AuthGuard] },
      { path: 'data', component: DataComponent , canActivate: [AuthGuard] },
      { path: '**', component: NotFoundPageComponent , canActivate: [AuthGuard] },
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
