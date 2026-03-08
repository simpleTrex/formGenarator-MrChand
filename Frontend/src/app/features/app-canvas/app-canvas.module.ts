import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CanvasHomeComponent } from './pages/canvas-home/canvas-home.component';

const routes: Routes = [
    { path: '', component: CanvasHomeComponent },
];

@NgModule({
    declarations: [CanvasHomeComponent],
    imports: [
        CommonModule,
        FormsModule,
        RouterModule.forChild(routes),
    ],
})
export class AppCanvasModule { }
