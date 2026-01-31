import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import {
  DomainService,
  DomainFieldType,
  DomainModelField,
  CreateDomainModelPayload,
  UpdateDomainModelPayload,
} from '../services/domain.service';

type ShareMode = 'THIS_APP' | 'SELECT_APPS' | 'ALL_APPS';

@Component({
  selector: 'app-app-models',
  templateUrl: './app-models.component.html',
  styleUrls: ['./app-models.component.css']
})
export class AppModelsComponent implements OnInit {
  domainSlug = '';
  appSlug = '';

  domain: any = null;
  app: any = null;

  appsInDomain: any[] = [];
  models: any[] = [];

  loading = false;
  error = '';
  message = '';

  appAccess = { permissions: [] as string[], groups: [] as string[] };

  selectedModel: any | null = null;

  modelForm: FormGroup;

  fieldTypes: DomainFieldType[] = ['STRING', 'NUMBER', 'BOOLEAN', 'DATE', 'DATETIME', 'REFERENCE', 'OBJECT', 'ARRAY'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private domainService: DomainService,
    public auth: AuthService,
  ) {
    this.modelForm = this.fb.group({
      name: ['', Validators.required],
      slug: ['', Validators.required],
      description: [''],
      shareMode: ['THIS_APP' as ShareMode],
      selectedAppIds: [[] as string[]],
      fields: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];
    this.appSlug = this.route.snapshot.params['appSlug'];

    if (!this.domainSlug || !this.appSlug) {
      this.error = 'Missing domain/app context';
      return;
    }

    this.loadContext();
  }

  get canManageModels(): boolean {
    return this.isOwnerContext() || this.appAccess.permissions.includes('APP_WRITE');
  }

  isOwnerContext(): boolean {
    const ctx = this.auth.getContext();
    return !!(ctx && ctx.principalType === 'OWNER');
  }

  goBackToApp(): void {
    this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
  }

  get fieldsArray(): FormArray {
    return this.modelForm.get('fields') as FormArray;
  }

  addField(initial?: Partial<DomainModelField>) {
    this.fieldsArray.push(this.fb.group({
      key: [initial?.key || '', Validators.required],
      type: [initial?.type || 'STRING', Validators.required],
      required: [!!initial?.required],
      unique: [!!initial?.unique],
    }));
  }

  removeField(index: number) {
    this.fieldsArray.removeAt(index);
  }

  private loadContext() {
    this.loading = true;
    this.error = '';
    this.message = '';

    // 1) domain
    this.domainService.getBySlug(this.domainSlug).subscribe({
      next: (domain) => {
        this.domain = domain;
        // 2) app
        this.domainService.getApplication(this.domainSlug, this.appSlug).subscribe({
          next: (app) => {
            this.app = app;
            this.loadAccessAndData();
          },
          error: (err) => {
            this.loading = false;
            this.error = err?.error?.message || 'Application not found';
          }
        });
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Domain not found';
      }
    });
  }

  private loadAccessAndData() {
    if (this.isOwnerContext()) {
      this.appAccess.permissions = ['APP_READ', 'APP_WRITE', 'APP_EXECUTE'];
      this.appAccess.groups = ['App Admin'];
      this.loadAppsAndModels();
      return;
    }

    if (!this.auth.isLoggedIn()) {
      this.loading = false;
      this.error = 'Please login';
      return;
    }

    const ctx = this.auth.getContext();
    if (!ctx?.userId) {
      this.loading = false;
      this.error = 'Please login';
      return;
    }

    this.domainService.getUserAppGroups(this.domainSlug, this.appSlug, ctx.userId).subscribe({
      next: (groups: any[]) => {
        const permissions = new Set<string>();
        const names: string[] = [];
        (groups || []).forEach(g => {
          if (g?.name) {
            names.push(g.name);
          }
          (g?.permissions || []).forEach((p: string) => permissions.add(p));
        });
        this.appAccess = { permissions: Array.from(permissions), groups: names };

        if (!this.canManageModels) {
          this.loading = false;
          this.error = 'You do not have permission to manage models in this application.';
          return;
        }

        this.loadAppsAndModels();
      },
      error: () => {
        this.appAccess = { permissions: [], groups: [] };
        this.loading = false;
        this.error = 'You do not have permission to manage models in this application.';
      }
    });
  }

  private loadAppsAndModels() {
    Promise.all([
      this.domainService.getApplications(this.domainSlug).toPromise(),
      this.domainService.getDomainModels(this.domainSlug, this.appSlug).toPromise(),
    ]).then(([apps, models]) => {
      this.appsInDomain = apps || [];
      this.models = models || [];
      this.loading = false;

      // Default new model: only current app
      this.modelForm.patchValue({ shareMode: 'THIS_APP' as ShareMode, selectedAppIds: [] });
      if (this.fieldsArray.length === 0) {
        this.addField();
      }
    }).catch((err) => {
      this.loading = false;
      this.error = err?.error?.message || 'Failed to load models/apps';
    });
  }

  selectModel(model: any) {
    this.selectedModel = model;
    this.message = '';
    this.error = '';

    // rebuild fields
    while (this.fieldsArray.length) {
      this.fieldsArray.removeAt(0);
    }
    (model?.fields || []).forEach((f: any) => this.addField(f));
    if ((model?.fields || []).length === 0) {
      this.addField();
    }

    const allowedAppIds: string[] = (model?.allowedAppIds || []) as string[];
    let shareMode: ShareMode = 'SELECT_APPS';
    if (model?.sharedWithAllApps) {
      shareMode = 'ALL_APPS';
    } else if (this.app?.id && allowedAppIds.length === 1 && allowedAppIds[0] === this.app.id) {
      shareMode = 'THIS_APP';
    }

    this.modelForm.patchValue({
      name: model?.name || '',
      slug: model?.slug || '',
      description: model?.description || '',
      shareMode,
      selectedAppIds: allowedAppIds,
    });

    // slug shouldn't be editable during update (we use model.slug as id)
    this.modelForm.get('slug')?.disable();
  }

  newModel() {
    this.selectedModel = null;
    this.message = '';
    this.error = '';

    this.modelForm.reset({
      name: '',
      slug: '',
      description: '',
      shareMode: 'THIS_APP' as ShareMode,
      selectedAppIds: []
    });

    this.modelForm.get('slug')?.enable();

    while (this.fieldsArray.length) {
      this.fieldsArray.removeAt(0);
    }
    this.addField();
  }

  private buildAccess(): { sharedWithAllApps: boolean; allowedAppIds: string[] } {
    const shareMode = this.modelForm.get('shareMode')?.value as ShareMode;
    const selectedAppIds = (this.modelForm.get('selectedAppIds')?.value || []) as string[];

    if (shareMode === 'ALL_APPS') {
      return { sharedWithAllApps: true, allowedAppIds: [] };
    }

    if (shareMode === 'THIS_APP') {
      return { sharedWithAllApps: false, allowedAppIds: this.app?.id ? [this.app.id] : [] };
    }

    return { sharedWithAllApps: false, allowedAppIds: selectedAppIds };
  }

  private buildFields(): DomainModelField[] {
    return this.fieldsArray.controls.map(ctrl => {
      const v = ctrl.value;
      return {
        key: (v.key || '').trim(),
        type: v.type,
        required: !!v.required,
        unique: !!v.unique,
        config: {},
      };
    }).filter(f => !!f.key);
  }

  save() {
    this.message = '';
    this.error = '';

    if (!this.canManageModels) {
      this.error = 'You do not have permission to manage models.';
      return;
    }

    if (this.modelForm.invalid) {
      this.error = 'Please fill required fields.';
      return;
    }

    const access = this.buildAccess();
    const fields = this.buildFields();

    if (!this.selectedModel) {
      const payload: CreateDomainModelPayload = {
        name: this.modelForm.get('name')?.value,
        slug: this.modelForm.get('slug')?.value,
        description: this.modelForm.get('description')?.value,
        sharedWithAllApps: access.sharedWithAllApps,
        allowedAppIds: access.allowedAppIds,
        fields,
      };

      this.domainService.createDomainModel(this.domainSlug, this.appSlug, payload).subscribe({
        next: () => {
          this.message = 'Model created.';
          this.newModel();
          this.refreshModels();
        },
        error: (err) => {
          this.error = err?.error?.message || err?.error || 'Failed to create model';
        }
      });
      return;
    }

    const payload: UpdateDomainModelPayload = {
      name: this.modelForm.get('name')?.value,
      description: this.modelForm.get('description')?.value,
      sharedWithAllApps: access.sharedWithAllApps,
      allowedAppIds: access.allowedAppIds,
      fields,
    };

    this.domainService.updateDomainModel(this.domainSlug, this.appSlug, this.selectedModel.slug, payload).subscribe({
      next: () => {
        this.message = 'Model updated.';
        this.refreshModels();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.error || 'Failed to update model';
      }
    });
  }

  deleteSelected() {
    if (!this.selectedModel) {
      return;
    }
    if (!this.canManageModels) {
      this.error = 'You do not have permission to delete models.';
      return;
    }
    const ok = confirm(`Delete model "${this.selectedModel.slug}"?`);
    if (!ok) {
      return;
    }

    this.domainService.deleteDomainModel(this.domainSlug, this.appSlug, this.selectedModel.slug).subscribe({
      next: () => {
        this.message = 'Model deleted.';
        this.newModel();
        this.refreshModels();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.error || 'Failed to delete model';
      }
    });
  }

  private refreshModels() {
    this.domainService.getDomainModels(this.domainSlug, this.appSlug).subscribe({
      next: (models) => this.models = models || [],
      error: () => { /* ignore */ }
    });
  }
}
