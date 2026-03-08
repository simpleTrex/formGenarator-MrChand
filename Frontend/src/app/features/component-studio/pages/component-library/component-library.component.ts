import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomainService } from '../../../../core/services/domain.service';

@Component({
    selector: 'app-component-library',
    templateUrl: './component-library.component.html',
    styleUrls: ['./component-library.component.css'],
})
export class ComponentLibraryComponent implements OnInit {
    domainSlug = '';
    appSlug = '';
    components: any[] = [];
    loading = true;
    error = '';
    deleteError = '';

    primitiveIcons: Record<string, string> = {
        ITEM_CARD: '🃏',
        DATA_TABLE: '📋',
        ENTRY_FORM: '📝',
        STAT_CARD: '📊',
        NAVBAR: '🧭',
        SIGN_IN_FORM: '🔐',
        GALLERY_GRID: '🖼️',
        DETAIL_VIEW: '🔍',
    };

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private domainService: DomainService,
    ) { }

    ngOnInit(): void {
        const rootParams = this.route.snapshot.pathFromRoot
            .reduce((acc, r) => ({ ...acc, ...r.params }), {} as any);
        this.domainSlug = rootParams['slug'] || '';
        this.appSlug = rootParams['appSlug'] || '';
        this.loadComponents();
    }

    loadComponents(): void {
        this.loading = true;
        this.domainService.getComponents(this.domainSlug, this.appSlug).subscribe({
            next: (data) => {
                this.components = data;
                this.loading = false;
            },
            error: () => {
                this.error = 'Failed to load components';
                this.loading = false;
            },
        });
    }

    getIcon(type: string): string {
        return this.primitiveIcons[type] || '🧩';
    }

    editComponent(compId: string): void {
        this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'components', 'edit', compId]);
    }

    deleteComponent(compId: string): void {
        if (!confirm('Delete this component?')) return;
        this.domainService.deleteComponent(this.domainSlug, this.appSlug, compId).subscribe({
            next: () => this.loadComponents(),
            error: () => { this.deleteError = 'Failed to delete component'; },
        });
    }

    addNew(): void {
        this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'components']);
    }

    goBack(): void {
        this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
    }
}
