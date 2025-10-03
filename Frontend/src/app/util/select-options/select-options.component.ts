import { Component, Input, OnInit } from '@angular/core';
import { CustomOptions } from 'src/app/model/form.model';

@Component({
  selector: 'select-options',
  templateUrl: './select-options.component.html',
  styleUrls: ['./select-options.component.css']
})
export class SelectOptionsComponent implements OnInit {

  @Input() isRadio = false;
  @Input() availableOptions?: Array<CustomOptions>;
  @Input() textLabel = "Option";

  constructor() {

  }

  ngOnInit(): void {
  }

  addNewOptions(): void {
    const option = new CustomOptions();
    option._id = new Date().getTime();
    this.availableOptions?.push(option);
  }

}
