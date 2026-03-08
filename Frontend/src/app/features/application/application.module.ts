import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { ModernButtonComponent } from '../../shared/components/modern-button/modern-button.component';
import { ModernCardComponent } from '../../shared/components/modern-card/modern-card.component';

// ── Existing pages ──────────────────────────────────────────────────────────
import { AppHomeComponent } from './pages/app-home/app-home.component';
import { AppModelsComponent } from './pages/app-models/app-models.component';

// ── Component Studio ────────────────────────────────────────────────────────
import { PrimitivePickerComponent } from '../component-studio/pages/primitive-picker/primitive-picker.component';
import { ComponentConfigComponent } from '../component-studio/pages/component-config/component-config.component';
import { ComponentLibraryComponent } from '../component-studio/pages/component-library/component-library.component';

// ── App Canvas ───────────────────────────────────────────────────────────────
import { CanvasHomeComponent } from '../app-canvas/pages/canvas-home/canvas-home.component';

const routes: Routes = [
    { path: ':appSlug', component: AppHomeComponent },
    { path: ':appSlug/models', component: AppModelsComponent },

    // Component Studio routes
    { path: ':appSlug/components', component: PrimitivePickerComponent },
    { path: ':appSlug/components/new/:primitiveType', component: ComponentConfigComponent },
    { path: ':appSlug/components/edit/:compId', component: ComponentConfigComponent },
    { path: ':appSlug/components/library', component: ComponentLibraryComponent },

    // App Canvas route
    { path: ':appSlug/canvas', component: CanvasHomeComponent },
];

@NgModule({
    declarations: [
        AppHomeComponent,
        AppModelsComponent,
        PrimitivePickerComponent,
        ComponentConfigComponent,
        ComponentLibraryComponent,
        CanvasHomeComponent,
    ],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        SharedModule,
        RouterModule.forChild(routes),
        ModernCardComponent,
        ModernButtonComponent,
    ],
})
export class ApplicationModule { }
