import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ModernButtonComponent } from '../../shared/components/modern-button/modern-button.component';
import { ModernCardComponent } from '../../shared/components/modern-card/modern-card.component';
import { AppHomeComponent } from './pages/app-home/app-home.component';
import { AppModelsComponent } from './pages/app-models/app-models.component';

const routes: Routes = [
    { path: ':appSlug', component: AppHomeComponent },
    { path: ':appSlug/models', component: AppModelsComponent },
    {
        path: ':appSlug/components',
        loadChildren: () => import('../component-studio/component-studio.module').then(m => m.ComponentStudioModule)
    },
    {
        path: ':appSlug/canvas',
        loadChildren: () => import('../app-canvas/app-canvas.module').then(m => m.AppCanvasModule)
    },
];

@NgModule({
    declarations: [
        AppHomeComponent,
        AppModelsComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(routes),
        ModernCardComponent,
        ModernButtonComponent,
    ],
})
export class ApplicationModule { }
