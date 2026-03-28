import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-workflow-explainer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './workflow-explainer.component.html',
  styleUrls: ['./workflow-explainer.component.css']
})
export class WorkflowExplainerComponent implements OnInit {
  domainSlug = '';
  appSlug = '';
  dataShapeExample = `{
  "workflowDefinition": {
    "steps": [
      {
        "id": "step_1",
        "fields": [{ "key": "amount", "type": "NUMBER", "required": true }],
        "edges": [{ "id": "approve", "targetStepId": "step_2", "allowedRoles": ["App Admin"] }]
      }
    ]
  },
  "workflowInstance": {
    "currentStepId": "step_2",
    "stepRecords": {
      "step_1": { "amount": 1000, "requestor": "testabc123" }
    },
    "history": [
      { "stepId": "step_1", "edgeName": "approve", "performedBy": "testabc123" }
    ]
  }
}`;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];
    this.appSlug = this.route.snapshot.params['appSlug'];
  }

  goBackToBuilder(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'workflows', 'builder']);
  }
}
