import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AppHomeComponent } from './pages/app-home/app-home.component';
import { AppModelsComponent } from './pages/app-models/app-models.component';

const routes: Routes = [
    { path: ':appSlug', component: AppHomeComponent },
    { path: ':appSlug/models', component: AppModelsComponent },
];

@NgModule({
    declarations: [
        AppHomeComponent,
        AppModelsComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(routes),
    ],
})
export class ApplicationModule { }
