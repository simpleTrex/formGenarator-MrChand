import { Component, Input, OnInit } from '@angular/core';
import { CustomForm, FIELD_TYPES } from '../../../core/models/form.model';
import { DomainService } from '../../../core/services/domain.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
    selector: 'render-form',
    templateUrl: './render-form.component.html',
    styleUrls: ['./render-form.component.css']
})
export class RenderFormComponent implements OnInit {

    @Input() customForm?: CustomForm = new CustomForm();
    @Input() domainSlug?: string; // Required for loading employees

    elementTypes: string[] = [];
    employees: any[] = [];
    loadingEmployees = false;

    constructor(
        private domainService: DomainService,
        private authService: AuthService
    ) {
    }

    ngOnInit(): void {
        this.prepareElementTypes();
        this.loadEmployeesIfNeeded();
    }

    prepareElementTypes(): void {
        this.elementTypes = [];
        FIELD_TYPES.forEach(type => {
            this.elementTypes.push(type.value);
        });
    }

    private loadEmployeesIfNeeded(): void {
        // Check if the form has any EMPLOYEE_REFERENCE fields
        const hasEmployeeReference = this.customForm?.fields?.some(field =>
            field.elementType === 'EMPLOYEE_REFERENCE'
        );

        if (hasEmployeeReference && this.getDomainSlug()) {
            this.loadEmployees();
        }
    }

    private getDomainSlug(): string | null {
        // First try explicit input, then try to get from URL or context
        if (this.domainSlug) {
            return this.domainSlug;
        }

        // Try to extract from current URL path as fallback
        const currentPath = window.location.pathname;
        const domainMatch = currentPath.match(/\/domain\/([^\/]+)/);
        return domainMatch ? domainMatch[1] : null;
    }

    private loadEmployees(): void {
        const domainSlug = this.getDomainSlug();
        if (!domainSlug) {
            return;
        }

        this.loadingEmployees = true;
        this.domainService.getEmployees(domainSlug).subscribe({
            next: (employees: any[]) => {
                this.employees = employees || [];
                this.loadingEmployees = false;
            },
            error: (err) => {
                console.error('Failed to load employees:', err);
                this.employees = [];
                this.loadingEmployees = false;
            }
        });
    }

    getEmployeeName(employee: any): string {
        const data = employee.data || {};
        const firstName = data.firstName || '';
        const lastName = data.lastName || '';
        const employeeId = data.employeeId || '';

        if (firstName && lastName) {
            return `${firstName} ${lastName}`;
        }
        return employeeId || 'Unknown Employee';
    }
}
