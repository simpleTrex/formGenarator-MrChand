import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RegularExpressionComponent } from './regular-expression/regular-expression.component';
import { FormsModule } from '@angular/forms';
import { SelectOptionsComponent } from './select-options/select-options.component';

@NgModule({
  declarations: [
    RegularExpressionComponent,
    SelectOptionsComponent
  ],
  imports: [
    CommonModule,
    FormsModule
  ],
  exports: [
    RegularExpressionComponent,
    SelectOptionsComponent
  ]
})
export class UtilModule { }
