import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ModelPageComponent } from './pages/form-designer/model-page.component';
import { RenderFormComponent } from '../../shared/components/render-form/render-form.component';
import { DataComponent } from './pages/form-data/data.component';
import { ModelOptionsComponent } from './components/field-properties/model-options.component';
import { RegularExpressionComponent } from './components/regular-expression/regular-expression.component';
import { SelectOptionsComponent } from './components/select-options/select-options.component';

const routes: Routes = [
    { path: '', component: ModelPageComponent },
    { path: 'preview', component: RenderFormComponent },
    { path: 'data', component: DataComponent },
];

@NgModule({
    declarations: [
        ModelPageComponent,
        DataComponent,
        ModelOptionsComponent,
        RegularExpressionComponent,
        SelectOptionsComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(routes),
    ],
})
export class FormBuilderModule { }

