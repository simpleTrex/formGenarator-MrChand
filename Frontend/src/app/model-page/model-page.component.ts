//This class render full Model/Class object in UI

import { Component, OnInit } from '@angular/core';
import { environment } from 'src/environments/environment';
import { CustomForm, FIELD_TYPES, FormField, Process } from '../model/form.model';
import { BaseService } from '../services/base.service';

@Component({
  selector: 'model-page',
  templateUrl: './model-page.component.html',
  styleUrls: ['./model-page.component.css']
})
export class ModelPageComponent implements OnInit {

  availableFormTemplates: Array<CustomForm> = new Array<CustomForm>();
  newForm: CustomForm = new CustomForm();
  newField: FormField = new FormField();
  availableFormFieldTypes = FIELD_TYPES;

  constructor(
    private baseService: BaseService,
  ) {
  }

  ngOnInit(): void {
    this.loadAllFormTemplates();
  }

  addFieldToForm(): void {
    this.newForm.fields?.push(this.newField);
    this.newField = new FormField();
  }

  /** Add/Update Model Form */
  saveFormTemplate(): void {
    if (this.newForm.name === undefined || this.newForm.name.trim() === '') {
      return alert('Please fill form name');
    }
    console.log(this.newForm);
    this.baseService.post(`${environment.api}/model/create`, false, this.newForm).subscribe(result => {
      console.log(result);
      alert('Successfully created');
      this.loadAllFormTemplates();
    });
  }

  loadAllFormTemplates(): void {
    this.baseService.get(`${environment.api}/model/all`, false).subscribe(result => {
      this.availableFormTemplates = result;
    });
  }

  setSelectedForm(template: CustomForm): void {
    this.newForm = template;
    if (!this.newForm.process) {
      this.newForm.process = new Process();
      this.newForm.process.action.elementType = 'select';
      this.newForm.process.action.name = 'Action/Status';
    }
  }
  onSelected(value: String): void {
    console.log("onModelPage:" + value);
    this.newField.model = new String(value);
  }

}
