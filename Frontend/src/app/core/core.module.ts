import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { AuthService } from './services/auth.service';
import { AuthGuard } from './guards/auth.guard';
import { DomainGuard } from './guards/domain.guard';
import { DomainService } from './services/domain.service';
import { httpInterceptorProviders } from './interceptors/auth.interceptor';
import { BaseService } from './services/api.service';

@NgModule({
  imports: [
    CommonModule,
    HttpClientModule,
  ],
  providers: [
    AuthService,
    AuthGuard,
    DomainGuard,
    DomainService,
    BaseService,
    httpInterceptorProviders,
  ],
})
export class CoreModule {}
