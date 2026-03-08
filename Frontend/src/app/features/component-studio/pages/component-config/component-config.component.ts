import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DomainService } from '../../../../core/services/domain.service';

@Component({
    selector: 'app-component-config',
    templateUrl: './component-config.component.html',
    styleUrls: ['./component-config.component.css'],
})
export class ComponentConfigComponent implements OnInit {
    domainSlug = '';
    appSlug = '';
    primitiveType = '';
    compId = '';      // set if editing
    isEditing = false;

    schema: any = null;
    models: any[] = [];
    pages: any[] = [];
    selectedModelFields: any[] = [];

    form!: FormGroup;
    loading = true;
    saving = false;
    error = '';

    primitiveLabels: Record<string, string> = {
        ITEM_CARD: '🃏 Item Card',
        DATA_TABLE: '📋 Data Table',
        ENTRY_FORM: '📝 Entry Form',
        STAT_CARD: '📊 Stat Card',
        NAVBAR: '🧭 Navbar',
        SIGN_IN_FORM: '🔐 Sign-In Form',
        GALLERY_GRID: '🖼️ Gallery Grid',
        DETAIL_VIEW: '🔍 Detail View',
    };

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private fb: FormBuilder,
        private domainService: DomainService,
    ) { }

    ngOnInit(): void {
        const rootParams = this.route.snapshot.pathFromRoot
            .reduce((acc, r) => ({ ...acc, ...r.params }), {} as any);
        this.domainSlug = rootParams['slug'] || '';
        this.appSlug = rootParams['appSlug'] || '';

        this.route.params.subscribe(params => {
            // also reread in case of param change
            const rp = this.route.snapshot.pathFromRoot
                .reduce((acc, r) => ({ ...acc, ...r.params }), {} as any);
            this.domainSlug = rp['slug'] || this.domainSlug;
            this.appSlug = rp['appSlug'] || this.appSlug;
            this.primitiveType = params['primitiveType'] || rp['primitiveType'] || '';
            this.compId = params['compId'] || rp['compId'] || '';
            this.isEditing = !!this.compId;
            this.initializeForm();
        });
    }

    private initializeForm(): void {
        this.form = this.fb.group({ name: ['', Validators.required] });
        this.loading = true;
        this.error = '';

        const loaders: Promise<any>[] = [
            this.domainService.getDomainModels(this.domainSlug, this.appSlug).toPromise(),
            this.domainService.getPages(this.domainSlug, this.appSlug).toPromise(),
        ];

        if (this.isEditing) {
            loaders.push(this.domainService.getComponent(this.domainSlug, this.appSlug, this.compId).toPromise());
        } else {
            loaders.push(this.domainService.getPrimitiveSchema(this.primitiveType).toPromise());
        }

        Promise.all(loaders).then(([models, pages, schemaOrComp]) => {
            this.models = models || [];
            this.pages = pages || [];

            if (this.isEditing) {
                const comp = schemaOrComp;
                this.primitiveType = comp.primitiveType;
                // Load schema after knowing the type
                this.domainService.getPrimitiveSchema(this.primitiveType).subscribe(schema => {
                    this.schema = schema;
                    this.buildFormControls();
                    this.patchFormValues(comp);
                    this.loading = false;
                });
            } else {
                this.schema = schemaOrComp;
                this.buildFormControls();
                this.loading = false;
            }
        }).catch(() => {
            this.error = 'Failed to load form data';
            this.loading = false;
        });
    }

    private buildFormControls(): void {
        if (!this.schema?.fields) return;
        for (const field of this.schema.fields) {
            if (field.key === 'name') continue; // already added
            const ctrl = field.required ? this.fb.control('', Validators.required) : this.fb.control('');
            this.form.addControl(field.key, ctrl);
        }
    }

    private patchFormValues(comp: any): void {
        this.form.patchValue({ name: comp.name });
        if (comp.modelId) {
            this.form.patchValue({ modelId: comp.modelId });
            this.onModelChange(comp.modelId);
        }
        if (comp.config) {
            this.form.patchValue(comp.config);
        }
    }

    onModelChange(modelId: string): void {
        const model = this.models.find(m => m.id === modelId);
        this.selectedModelFields = model?.fields || [];
        this.form.patchValue({ modelId });
    }

    getSchemaField(key: string): any {
        return this.schema?.fields?.find((f: any) => f.key === key);
    }

    isMultiSelect(fieldType: string): boolean {
        return fieldType === 'FIELD_MULTISELECT';
    }

    getMultiselectValues(key: string): string[] {
        const v = this.form.get(key)?.value;
        if (!v) return [];
        if (Array.isArray(v)) return v;
        return [v];
    }

    toggleMultiselect(key: string, value: string): void {
        let current: string[] = this.getMultiselectValues(key);
        if (current.includes(value)) {
            current = current.filter(v => v !== value);
        } else {
            current = [...current, value];
        }
        this.form.get(key)?.setValue(current);
    }

    save(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }
        this.saving = true;
        this.error = '';

        const { name, modelId, ...configFields } = this.form.value;
        const payload = {
            name,
            primitiveType: this.primitiveType,
            modelId: modelId || null,
            config: configFields,
        };

        const request$ = this.isEditing
            ? this.domainService.updateComponent(this.domainSlug, this.appSlug, this.compId, payload)
            : this.domainService.createComponent(this.domainSlug, this.appSlug, payload);

        request$.subscribe({
            next: () => {
                this.saving = false;
                this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'components', 'library']);
            },
            error: (err: any) => {
                this.error = err?.error?.message || 'Failed to save component';
                this.saving = false;
            },
        });
    }

    cancel(): void {
        if (this.isEditing) {
            this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'components', 'library']);
        } else {
            this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'components']);
        }
    }

    getPrimitiveLabel(): string {
        return this.primitiveLabels[this.primitiveType] || this.primitiveType;
    }
}
