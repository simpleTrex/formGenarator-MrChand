import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-workflow-explainer',
  templateUrl: './workflow-explainer.component.html',
  styleUrls: ['./workflow-explainer.component.css']
})
export class WorkflowExplainerComponent implements OnInit {
  domainSlug = '';
  appSlug = '';

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
