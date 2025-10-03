//This class render an existing domain model object as a sub dropdown list inside Model view UI
import { Component, Input, OnInit, Output } from '@angular/core';
import { environment } from 'src/environments/environment';
import { CustomForm, CustomOptions, FIELD_TYPES } from '../model/form.model';
import { BaseService } from '../services/base.service';

@Component({
  selector: 'model-options',
  templateUrl: './model-options.component.html',
  styleUrls: ['./model-options.component.css']
})
export class ModelOptionsComponent implements OnInit {

  /**Model list on Model page */
  model: String = new String();
  availableFormTemplates: Array<CustomForm> = new Array<CustomForm>();
  constructor(private baseService: BaseService,) {
  }

  ngOnInit(): void {
    this.loadAllFormTemplates();
  }

  onSelected(value: String): void {
    console.log("onSelected" + value);
    this.model = new String(value);
  }

  /**Load all models */
  loadAllFormTemplates(): void {
    this.baseService.get(`${environment.api}/model/all`, false).subscribe(result => {
      this.availableFormTemplates = result;
    });
  }
}


