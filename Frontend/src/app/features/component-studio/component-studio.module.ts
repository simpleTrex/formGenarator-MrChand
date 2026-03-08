import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { PrimitivePickerComponent } from './pages/primitive-picker/primitive-picker.component';
import { ComponentConfigComponent } from './pages/component-config/component-config.component';
import { ComponentLibraryComponent } from './pages/component-library/component-library.component';

const routes: Routes = [
    { path: '', component: PrimitivePickerComponent },
    { path: 'new/:primitiveType', component: ComponentConfigComponent },
    { path: 'edit/:compId', component: ComponentConfigComponent },
    { path: 'library', component: ComponentLibraryComponent },
];

@NgModule({
    declarations: [
        PrimitivePickerComponent,
        ComponentConfigComponent,
        ComponentLibraryComponent,
    ],
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule.forChild(routes),
    ],
})
export class ComponentStudioModule { }
