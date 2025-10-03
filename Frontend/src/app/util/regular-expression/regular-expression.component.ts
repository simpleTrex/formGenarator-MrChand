import { Component, Input, OnInit } from '@angular/core';
import { CustomRegularExpression } from 'src/app/model/form.model';

@Component({
  selector: 'regular-expression',
  templateUrl: './regular-expression.component.html',
  styleUrls: ['./regular-expression.component.css']
})
export class RegularExpressionComponent implements OnInit {

  @Input() availableRegularExpressions?: Array<CustomRegularExpression>;
  
  constructor() { }

  ngOnInit(): void {
  }

  addNewExpression(): void {
    const expression = new CustomRegularExpression();
    expression._id = new Date().getTime();
    this.availableRegularExpressions?.push(expression);
  }

}
