//This class render full Data object in UI for a model-options object
import { Component, Input, OnInit, Output } from '@angular/core';
import { environment } from 'src/environments/environment';
import { CustomForm, CustomOptions, FIELD_TYPES, FormField } from '../model/form.model';
import { BaseService } from '../services/base.service';

@Component({
  selector: 'model-render',
  templateUrl: './model-render.component.html',
  styleUrls: ['./model-render.component.css']
})
export class ModelRenderComponent implements OnInit {

  @Input() element: FormField = new FormField();

  model: String = new String();
  availableFormTemplates: Array<FormField> = new Array<FormField>();
  constructor(private baseService: BaseService,) {
  }

  ngOnInit(): void {
    this.model = new String(this.element?.model);
    this.loadAllFormTemplates(String(this.element?.model));
  }

  onSelected(value: string): void {
    console.log("onSelected" + value);
    this.element.value = value;
  }

  /**Load all models */
  loadAllFormTemplates(model: String): void {
    console.log("loadAllFormTemplates: " + model);
    this.baseService.get(`${environment.api}/data/` + model, false).subscribe(result => {
      console.log("loadAllFormTemplates:results " + model + ": " + JSON.stringify(result));
      let forms = result as Array<CustomForm>;
      forms.forEach((form) => {
        let fields = form.fields as Array<FormField>;
        this.availableFormTemplates.push(fields[0]);//TODO assume first field as identifier
      });
    });
  }
}

