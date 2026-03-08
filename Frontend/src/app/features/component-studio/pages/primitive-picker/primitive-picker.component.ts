import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomainService } from '../../../../core/services/domain.service';

@Component({
    selector: 'app-primitive-picker',
    templateUrl: './primitive-picker.component.html',
    styleUrls: ['./primitive-picker.component.css'],
})
export class PrimitivePickerComponent implements OnInit {
    domainSlug = '';
    appSlug = '';
    primitives: any[] = [];
    loading = true;
    error = '';

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private domainService: DomainService,
    ) { }

    ngOnInit(): void {
        // params (slug, appSlug) come from grandparent routes - merge all
        const rootParams = this.route.snapshot.pathFromRoot
            .reduce((acc, r) => ({ ...acc, ...r.params }), {} as any);
        this.domainSlug = rootParams['slug'] || '';
        this.appSlug = rootParams['appSlug'] || '';
        this.loadPrimitives();
    }

    private loadPrimitives(): void {
        this.domainService.getPrimitives().subscribe({
            next: (data) => {
                this.primitives = data;
                this.loading = false;
            },
            error: () => {
                this.error = 'Failed to load primitives';
                this.loading = false;
            },
        });
    }

    selectPrimitive(type: string): void {
        this.router.navigate(['new', type], { relativeTo: this.route });
    }

    goToLibrary(): void {
        this.router.navigate(['library'], { relativeTo: this.route });
    }

    goBack(): void {
        this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
    }
}
