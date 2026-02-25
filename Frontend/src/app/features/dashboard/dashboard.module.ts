import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { HomePageComponent } from './pages/home-page/home-page.component';

const routes: Routes = [
    { path: '', component: HomePageComponent },
];

@NgModule({
    declarations: [
        HomePageComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(routes),
    ],
})
export class DashboardModule { }
