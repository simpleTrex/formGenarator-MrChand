import { Component, OnInit } from '@angular/core';
import { environment } from 'src/environments/environment';
import { CustomForm } from '../model/form.model';
import { BaseService } from '../services/base.service';

@Component({
  selector: 'navi-data',
  templateUrl: './navi-data.component.html',
  styleUrls: ['./navi-data.component.css']
})
export class NaviDataComponent implements OnInit {

  dataTables: Array<CustomForm> = new Array<CustomForm>();
  newForm: CustomForm = new CustomForm();
  datalist: Array<CustomForm> = new Array<CustomForm>();

  constructor(
    private baseService: BaseService,
  ) {

  }

  ngOnInit(): void {
    this.loadAllDataForms();
  }

  /**Load all Model objects on startup*/
  loadAllDataForms(): void {
    this.baseService.get(`${environment.api}/data/all`, false).subscribe(result => {
      this.dataTables = result;
    });
  }

  /**Load all data in data table for this model by model name*/
  loadDataofSelectedModel(form: CustomForm): void {
    console.log("loadSelectedTable: " + form.name);

    this.baseService.get(`${environment.api}/data/` + form.name, false).subscribe(result => {
      console.log("loadSelectedTable:results: " + result);
      this.datalist = result;
    });
  }

  /**
   * Configure current clicked form as the FORM model object
   * and load all data in the data table for this model object
   * disconnect local reference object from model object's id
   */
  loadSelectedTable(form: CustomForm): void {
    console.log("loadSelectedTable: " + form.name);
    this.loadDataofSelectedModel(form);
    this.newForm = form;
    this.newForm.id = 0;
  }

  /***Load data object from data table for the select item in list data table */
  loadSeletedObject(newForm: CustomForm): void {
    console.log(newForm);
    this.baseService.get(`${environment.api}/data/` + newForm.name + '/' + newForm.id, false).subscribe(result => {
      console.log(result);
      this.newForm = result;
    });
  }

  /**add a new data to data table of this model */
  insertData() {
    console.log(JSON.stringify(this.newForm));
    this.baseService.post(`${environment.api}/data/add`, false, this.newForm).subscribe(result => {
      console.log(result);
      alert('Successfully Created/Updated');
      this.loadDataofSelectedModel(this.newForm);
      this.newForm = new CustomForm();
    });
  }
}
