import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { DomainService } from '../../../../core/services/domain.service';

@Component({
  selector: 'app-domain-employees',
  templateUrl: './domain-employees.component.html',
  styleUrls: ['./domain-employees.component.css']
})
export class DomainEmployeesComponent implements OnInit {
  domainSlug = '';
  domain: any = null;

  employees: any[] = [];
  employeeModel: any = null;

  loading = false;
  error = '';
  message = '';

  selectedEmployee: any | null = null;
  employeeForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private domainService: DomainService,
    public auth: AuthService,
  ) {
    this.employeeForm = this.fb.group({
      employeeId: ['', Validators.required],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      department: [''],
      position: [''],
      hireDate: [''],
      status: ['ACTIVE', Validators.required]
    });
  }

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];

    if (!this.domainSlug) {
      this.error = 'Missing domain context';
      return;
    }

    this.loadContext();
  }

  get canManageEmployees(): boolean {
    const ctx = this.auth.getContext();
    return !!(ctx && (ctx.principalType === 'OWNER' || ctx.principalType === 'DOMAIN_USER'));
  }

  goBackToDomain(): void {
    this.router.navigate(['/domain', this.domainSlug]);
  }

  private loadContext() {
    this.loading = true;
    this.error = '';
    this.message = '';

    // Load domain info
    this.domainService.getBySlug(this.domainSlug).subscribe({
      next: (domain) => {
        this.domain = domain;
        this.loadEmployeeData();
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Domain not found';
      }
    });
  }

  private loadEmployeeData() {
    if (!this.canManageEmployees) {
      this.loading = false;
      this.error = 'You do not have permission to manage employees.';
      return;
    }

    // Load employee model and records
    Promise.all([
      this.domainService.getEmployeeModel(this.domainSlug).toPromise(),
      this.domainService.getEmployees(this.domainSlug).toPromise(),
    ]).then(([model, employees]) => {
      this.employeeModel = model;
      this.employees = employees || [];
      this.loading = false;

      // Initialize form if no employee is selected
      if (!this.selectedEmployee) {
        this.resetForm();
      }
    }).catch((err) => {
      this.loading = false;
      this.error = err?.error?.message || 'Failed to load employee data';
    });
  }

  selectEmployee(employee: any) {
    this.selectedEmployee = employee;
    this.message = '';
    this.error = '';

    // Populate form with employee data
    const data = employee.data || {};
    this.employeeForm.patchValue({
      employeeId: data.employeeId || '',
      firstName: data.firstName || '',
      lastName: data.lastName || '',
      email: data.email || '',
      department: data.department || '',
      position: data.position || '',
      hireDate: data.hireDate ? new Date(data.hireDate).toISOString().split('T')[0] : '',
      status: data.status || 'ACTIVE'
    });

    // Disable employeeId during edit (it's used as unique identifier)
    this.employeeForm.get('employeeId')?.disable();
  }

  newEmployee() {
    this.selectedEmployee = null;
    this.message = '';
    this.error = '';
    this.resetForm();

    // Enable employeeId for new employee
    this.employeeForm.get('employeeId')?.enable();
  }

  private resetForm() {
    this.employeeForm.reset({
      employeeId: '',
      firstName: '',
      lastName: '',
      email: '',
      department: '',
      position: '',
      hireDate: '',
      status: 'ACTIVE'
    });
  }

  save() {
    this.message = '';
    this.error = '';

    if (!this.canManageEmployees) {
      this.error = 'You do not have permission to manage employees.';
      return;
    }

    if (this.employeeForm.invalid) {
      this.error = 'Please fill all required fields correctly.';
      return;
    }

    const formValue = this.employeeForm.getRawValue(); // getRawValue includes disabled fields
    const employeeData = {
      data: {
        employeeId: formValue.employeeId,
        firstName: formValue.firstName,
        lastName: formValue.lastName,
        email: formValue.email,
        department: formValue.department,
        position: formValue.position,
        hireDate: formValue.hireDate || null,
        status: formValue.status
      }
    };

    if (!this.selectedEmployee) {
      // Create new employee
      this.domainService.createEmployee(this.domainSlug, employeeData).subscribe({
        next: () => {
          this.message = 'Employee created successfully.';
          this.newEmployee();
          this.refreshEmployees();
        },
        error: (err) => {
          this.error = err?.error?.message || err?.error || 'Failed to create employee';
        }
      });
      return;
    }

    // Update existing employee
    this.domainService.updateEmployee(this.domainSlug, this.selectedEmployee.id, employeeData).subscribe({
      next: () => {
        this.message = 'Employee updated successfully.';
        this.refreshEmployees();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.error || 'Failed to update employee';
      }
    });
  }

  deleteSelected() {
    if (!this.selectedEmployee) {
      return;
    }
    if (!this.canManageEmployees) {
      this.error = 'You do not have permission to delete employees.';
      return;
    }

    const employeeName = this.getEmployeeName(this.selectedEmployee);
    const ok = confirm(`Delete employee "${employeeName}"?\n\nThis action cannot be undone and may affect workflows that reference this employee.`);
    if (!ok) {
      return;
    }

    this.domainService.deleteEmployee(this.domainSlug, this.selectedEmployee.id).subscribe({
      next: () => {
        this.message = 'Employee deleted successfully.';
        this.newEmployee();
        this.refreshEmployees();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.error || 'Failed to delete employee';
      }
    });
  }

  private refreshEmployees() {
    this.domainService.getEmployees(this.domainSlug).subscribe({
      next: (employees) => this.employees = employees || [],
      error: () => { /* ignore */ }
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

  getEmployeeEmail(employee: any): string {
    return employee.data?.email || '';
  }

  getEmployeeDepartment(employee: any): string {
    return employee.data?.department || 'N/A';
  }

  getEmployeeStatus(employee: any): string {
    return employee.data?.status || 'ACTIVE';
  }
}