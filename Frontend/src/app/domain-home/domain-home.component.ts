import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DomainService } from '../services/domain.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-domain-home',
  templateUrl: './domain-home.component.html',
  styleUrls: ['./domain-home.component.css']
})
export class DomainHomeComponent implements OnInit {
  slug = '';
  domain: any = null;
  error = '';

  loginForm: FormGroup;
  signupForm: FormGroup;
  submittedLogin = false;
  submittedSignup = false;
  loadingLogin = false;
  loadingSignup = false;
  loginError = '';
  signupError = '';
  signupMessage = '';

  domainGroups: any[] = [];
  domainGroupsLoading = false;
  groupError = '';
  memberInputs: Record<string, string> = {};

  domainUsers: any[] = [];
  usersLoading = false;
  draggedUser: any = null;
  draggedFromGroupId: string | null = null;

  apps: any[] = [];
  appsLoading = false;
  newApp = { name: '', slug: '', ownerUserId: '' };
  appMessage = '';
  appError = '';

  domainAccess = { permissions: [] as string[], groups: [] as string[] };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private domainService: DomainService,
    private fb: FormBuilder,
    public auth: AuthService,
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
    this.signupForm = this.fb.group({
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  logout() {
    this.auth.logout();
    this.initializeAccess();
  }

  ngOnInit(): void {
    this.slug = this.route.snapshot.params['slug'];
    if (this.slug) {
      this.domainService.getBySlug(this.slug).subscribe({
        next: (res) => {
          this.domain = res;
          this.initializeAccess();
        },
        error: (err) => this.error = err?.error?.message || 'Domain not found or access denied'
      });
    }
  }

  private initializeAccess() {
    if (!this.domain) {
      return;
    }
    if (this.isOwnerContext()) {
      this.domainAccess.permissions = [
        'DOMAIN_MANAGE',
        'DOMAIN_MANAGE_USERS',
        'DOMAIN_MANAGE_APPS',
        'DOMAIN_USE_APP'
      ];
      this.domainAccess.groups = ['Domain Admin'];
      this.loadDomainGroups();
      this.loadApplications();
      return;
    }
    if (this.auth.isLoggedIn()) {
      this.domainService.getDomainRoles(this.slug).subscribe({
        next: res => {
          this.domainAccess = res || { permissions: [], groups: [] };
          if (this.canManageUsers) {
            this.loadDomainGroups();
          }
          if (this.canUseApps || this.canManageApps) {
            this.loadApplications();
          }
        },
        error: () => { /* ignore */ }
      });
    }
  }

  goBackToAdaptive() {
    this.router.navigate(['/']);
  }

  private contextMatchesDomain(): boolean {
    const ctx = this.auth.getContext();
    return !!(ctx && this.domain && ctx.domainId === this.domain.id);
  }

  isDomainUser(): boolean {
    const ctx = this.auth.getContext();
    return !!(ctx && ctx.principalType === 'DOMAIN_USER' && this.contextMatchesDomain());
  }

  isOwnerContext(): boolean {
    const ctx = this.auth.getContext();
    return !!(ctx && ctx.principalType === 'OWNER');
  }

  get canManageUsers(): boolean {
    return this.isOwnerContext() || this.hasPermission('DOMAIN_MANAGE_USERS');
  }

  get canManageApps(): boolean {
    return this.isOwnerContext() || this.hasPermission('DOMAIN_MANAGE_APPS');
  }

  get canUseApps(): boolean {
    return this.isOwnerContext() || this.hasPermission('DOMAIN_USE_APP');
  }

  private hasPermission(code: string): boolean {
    return this.domainAccess.permissions.includes(code);
  }

  get lf() { return this.loginForm.controls; }
  get sf() { return this.signupForm.controls; }

  submitLogin() {
    this.submittedLogin = true;
    if (this.loginForm.invalid) return;
    this.loadingLogin = true;
    this.auth.loginDomain(this.slug, this.lf['username'].value, this.lf['password'].value).subscribe({
      next: () => {
        this.loadingLogin = false;
        this.loginError = '';
        this.initializeAccess();
      },
      error: err => {
        this.loginError = err?.error ?? 'Login failed';
        this.loadingLogin = false;
      }
    });
  }

  submitSignup() {
    this.submittedSignup = true;
    if (this.signupForm.invalid) return;
    this.loadingSignup = true;
    this.auth.signupDomain(this.slug, this.sf['username'].value, this.sf['email'].value, this.sf['password'].value).subscribe({
      next: () => {
        this.signupMessage = 'Account created. Please log in.';
        this.signupError = '';
        this.loadingSignup = false;
        this.signupForm.reset();
        this.submittedSignup = false;
      },
      error: err => {
        this.signupError = err?.error ?? 'Signup failed';
        this.loadingSignup = false;
      }
    });
  }

  loadDomainGroups() {
    this.domainGroupsLoading = true;
    this.usersLoading = true;
    Promise.all([
      this.domainService.getDomainGroups(this.slug).toPromise(),
      this.domainService.getDomainUsersWithGroups(this.slug).toPromise()
    ]).then(([groups, users]) => {
      this.domainGroups = groups || [];
      this.domainUsers = users || [];
      this.domainGroupsLoading = false;
      this.usersLoading = false;
    }).catch(err => {
      this.groupError = err?.error ?? 'Failed to load groups and users';
      this.domainGroupsLoading = false;
      this.usersLoading = false;
    });
  }

  addMember(groupId: string) {
    const username = (this.memberInputs[groupId] || '').trim();
    if (!username) {
      return;
    }
    this.domainService.addDomainGroupMember(this.slug, groupId, username).subscribe({
      next: () => {
        this.memberInputs[groupId] = '';
        this.loadDomainGroups();
      },
      error: err => {
        this.groupError = err?.error ?? 'Failed to add member';
      }
    });
  }

  getUsersInGroup(groupId: string): any[] {
    return this.domainUsers.filter(user => 
      user.groups?.some((g: any) => g.groupId === groupId)
    );
  }

  getUsersWithoutGroups(): any[] {
    return this.domainUsers.filter(user => !user.groups || user.groups.length === 0);
  }

  onDragStart(event: DragEvent, user: any, fromGroupId: string | null): void {
    this.draggedUser = user;
    this.draggedFromGroupId = fromGroupId;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onDrop(event: DragEvent, targetGroupId: string): void {
    event.preventDefault();
    event.stopPropagation();

    if (!this.draggedUser) return;

    const user = this.draggedUser;
    const fromGroupId = this.draggedFromGroupId;

    if (fromGroupId === targetGroupId) {
      this.draggedUser = null;
      this.draggedFromGroupId = null;
      return;
    }

    const removePromise = fromGroupId 
      ? this.domainService.removeDomainGroupMember(this.slug, fromGroupId, user.id).toPromise()
      : Promise.resolve();

    removePromise.then(() => {
      return this.domainService.addDomainGroupMember(this.slug, targetGroupId, user.username).toPromise();
    }).then(() => {
      this.loadDomainGroups();
    }).catch(err => {
      this.groupError = 'Failed to update user group membership';
      console.error(err);
    }).finally(() => {
      this.draggedUser = null;
      this.draggedFromGroupId = null;
    });
  }

  onDragEnd(): void {
    this.draggedUser = null;
    this.draggedFromGroupId = null;
  }

  removeUserFromGroup(user: any, groupId: string): void {
    if (!confirm(`Remove ${user.username} from this group?`)) {
      return;
    }

    this.domainService.removeDomainGroupMember(this.slug, groupId, user.id)
      .toPromise()
      .then(() => {
        this.loadDomainGroups();
      })
      .catch(err => {
        this.groupError = 'Failed to remove user from group';
        console.error(err);
      });
  }

  loadApplications() {
    this.appsLoading = true;
    this.domainService.getApplications(this.slug).subscribe({
      next: apps => {
        this.apps = apps;
        this.appsLoading = false;
      },
      error: err => {
        this.appError = err?.error ?? 'Failed to load applications';
        this.appsLoading = false;
      }
    });
  }

  submitNewApp() {
    if (!this.newApp.name || !this.newApp.slug) {
      this.appError = 'Name and slug required';
      return;
    }
    this.appError = '';
    this.appMessage = '';
    this.domainService.createApplication(this.slug, this.newApp).subscribe({
      next: () => {
        this.appMessage = 'Application created';
        this.newApp = { name: '', slug: '', ownerUserId: '' };
        this.loadApplications();
      },
      error: err => {
        this.appError = err?.error ?? 'Failed to create application';
      }
    });
  }
}
