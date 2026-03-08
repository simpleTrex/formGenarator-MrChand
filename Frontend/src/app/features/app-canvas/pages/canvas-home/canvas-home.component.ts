import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomainService } from '../../../../core/services/domain.service';

interface LayoutCell {
    componentId: string;
    row: number;
    col: number;
    w: number;
    h: number;
    // runtime (non-saved)
    compName?: string;
    compIcon?: string;
}

@Component({
    selector: 'app-canvas-home',
    templateUrl: './canvas-home.component.html',
    styleUrls: ['./canvas-home.component.css'],
})
export class CanvasHomeComponent implements OnInit {
    domainSlug = '';
    appSlug = '';

    pages: any[] = [];
    components: any[] = [];
    selectedPage: any = null;
    layoutCells: LayoutCell[] = [];

    newPageName = '';
    addingPage = false;

    loading = false;
    saving = false;
    error = '';
    saveMsg = '';

    // Drag state
    draggingCompId = ''; // component being dragged from tray
    draggingCellIdx = -1; // cell being dragged within canvas

    // Grid config
    readonly COLS = 12;
    readonly ROWS = 10;

    primitiveIcons: Record<string, string> = {
        ITEM_CARD: '🃏', DATA_TABLE: '📋', ENTRY_FORM: '📝',
        STAT_CARD: '📊', NAVBAR: '🧭', SIGN_IN_FORM: '🔐',
        GALLERY_GRID: '🖼️', DETAIL_VIEW: '🔍',
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
        this.loadData();
    }

    private loadData(): void {
        this.loading = true;
        Promise.all([
            this.domainService.getPages(this.domainSlug, this.appSlug).toPromise(),
            this.domainService.getComponents(this.domainSlug, this.appSlug).toPromise(),
        ]).then(([pages, components]) => {
            this.pages = pages || [];
            this.components = components || [];
            if (this.pages.length > 0) {
                this.selectPage(this.pages[0]);
            }
            this.loading = false;
        }).catch(() => {
            this.error = 'Failed to load data';
            this.loading = false;
        });
    }

    selectPage(page: any): void {
        this.selectedPage = page;
        this.layoutCells = [];
        this.loadPageLayout(page.id);
    }

    private loadPageLayout(pageId: string): void {
        this.domainService.getPageLayout(this.domainSlug, this.appSlug, pageId).subscribe({
            next: (layout) => {
                const items = layout?.layout || [];
                this.layoutCells = items.map((item: any) => ({
                    ...item,
                    compName: this.getCompName(item.componentId),
                    compIcon: this.getCompIcon(item.componentId),
                }));
            },
            error: () => { this.layoutCells = []; },
        });
    }

    // ─── Page Management ──────────────────────────────────────────────────────

    startAddPage(): void { this.addingPage = true; this.newPageName = ''; }
    cancelAddPage(): void { this.addingPage = false; }

    confirmAddPage(): void {
        if (!this.newPageName.trim()) return;
        this.domainService.createPage(this.domainSlug, this.appSlug, {
            name: this.newPageName.trim(),
            order: this.pages.length,
        }).subscribe({
            next: (page) => {
                this.pages.push(page);
                this.addingPage = false;
                this.newPageName = '';
                this.selectPage(page);
            },
            error: (err: any) => { this.error = err?.error?.message || 'Failed to create page'; },
        });
    }

    deletePage(page: any): void {
        if (!confirm(`Delete page "${page.name}"?`)) return;
        this.domainService.deletePage(this.domainSlug, this.appSlug, page.id).subscribe({
            next: () => {
                this.pages = this.pages.filter(p => p.id !== page.id);
                if (this.selectedPage?.id === page.id) {
                    this.selectedPage = this.pages[0] || null;
                    this.layoutCells = [];
                    if (this.selectedPage) this.loadPageLayout(this.selectedPage.id);
                }
            },
            error: () => { this.error = 'Failed to delete page'; },
        });
    }

    // ─── Save Layout ──────────────────────────────────────────────────────────

    saveLayout(): void {
        if (!this.selectedPage) return;
        this.saving = true;
        this.saveMsg = '';
        const payload = this.layoutCells.map(cell => ({
            componentId: cell.componentId,
            row: cell.row,
            col: cell.col,
            w: cell.w,
            h: cell.h,
        }));
        this.domainService.savePageLayout(this.domainSlug, this.appSlug, this.selectedPage.id, payload).subscribe({
            next: () => {
                this.saving = false;
                this.saveMsg = 'Layout saved!';
                setTimeout(() => (this.saveMsg = ''), 2500);
            },
            error: () => { this.error = 'Failed to save layout'; this.saving = false; },
        });
    }

    // ─── Drag & Drop (Tray → Canvas) ─────────────────────────────────────────

    onTrayDragStart(comp: any, event: DragEvent): void {
        this.draggingCompId = comp.id;
        if (event.dataTransfer) { event.dataTransfer.effectAllowed = 'copy'; }
    }

    onCanvasDrop(row: number, col: number, event: DragEvent): void {
        event.preventDefault();
        if (!this.draggingCompId) return;
        // Check slot not taken
        const taken = this.layoutCells.some(c => c.row === row && c.col === col);
        if (taken) { this.draggingCompId = ''; return; }
        const comp = this.components.find(c => c.id === this.draggingCompId);
        if (!comp) { this.draggingCompId = ''; return; }
        this.layoutCells.push({
            componentId: comp.id,
            row, col,
            w: 4, h: 2,
            compName: comp.name,
            compIcon: this.primitiveIcons[comp.primitiveType] || '🧩',
        });
        this.draggingCompId = '';
    }

    onCanvasDragOver(event: DragEvent): void { event.preventDefault(); }

    removeCell(idx: number): void { this.layoutCells.splice(idx, 1); }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    getGridRows(): number[] { return Array.from({ length: this.ROWS }, (_, i) => i + 1); }
    getGridCols(): number[] { return Array.from({ length: this.COLS }, (_, i) => i + 1); }

    getCellAt(row: number, col: number): LayoutCell | undefined {
        return this.layoutCells.find(c => c.row === row && c.col === col);
    }

    isCellShadow(row: number, col: number): boolean {
        return this.layoutCells.some(c =>
            row >= c.row && row < c.row + c.h &&
            col > c.col && col < c.col + c.w
        );
    }

    getCompName(id: string): string {
        return this.components.find(c => c.id === id)?.name || id;
    }

    getCompIcon(id: string): string {
        const comp = this.components.find(c => c.id === id);
        return comp ? (this.primitiveIcons[comp.primitiveType] || '🧩') : '🧩';
    }

    goBack(): void {
        this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug]);
    }

    goToComponents(): void {
        this.router.navigate(['/domain', this.domainSlug, 'app', this.appSlug, 'components', 'library']);
    }
}
