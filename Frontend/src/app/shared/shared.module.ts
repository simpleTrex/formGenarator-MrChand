import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NaviBarComponent } from './components/navbar/navi-bar.component';
import { NaviDataComponent } from './components/navi-data/navi-data.component';
import { NotFoundPageComponent } from './components/not-found-page/not-found-page.component';
import { RenderFormComponent } from '../features/form-builder/pages/form-preview/render-form.component';
import { ModelRenderComponent } from '../features/form-builder/components/field-renderer/model-render.component';

@NgModule({
    declarations: [
        NaviBarComponent,
        NaviDataComponent,
        NotFoundPageComponent,
        RenderFormComponent,
        ModelRenderComponent,
    ],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule,
    ],
    exports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule,
        NaviBarComponent,
        NaviDataComponent,
        NotFoundPageComponent,
        RenderFormComponent,
        ModelRenderComponent,
    ],
})
export class SharedModule { }

