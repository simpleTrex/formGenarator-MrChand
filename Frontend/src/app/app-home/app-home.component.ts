import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomainService } from '../services/domain.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-app-home',
  templateUrl: './app-home.component.html',
  styleUrls: ['./app-home.component.css']
})
export class AppHomeComponent implements OnInit {
  domainSlug = '';
  appSlug = '';
  domain: any = null;
  app: any = null;
  error = '';

  appGroups: any[] = [];
  appGroupsLoading = false;
  groupError = '';
  memberInputs: Record<string, string> = {};

  appUsers: any[] = [];
  usersLoading = false;
  draggedUser: any = null;
  draggedFromGroupId: string | null = null;

  domainAccess = { permissions: [] as string[], groups: [] as string[] };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private domainService: DomainService,
    public auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.domainSlug = this.route.snapshot.params['slug'];
    this.appSlug = this.route.snapshot.params['appSlug'];
    
    if (this.domainSlug && this.appSlug) {
      this.loadDomain();
    }
  }

  private loadDomain() {
    this.domainService.getBySlug(this.domainSlug).subscribe({
      next: (res) => {
        this.domain = res;
        this.loadApplication();
      },
      error: (err) => this.error = err?.error?.message || 'Domain not found'
    });
  }

  private loadApplication() {
    this.domainService.getApplication(this.domainSlug, this.appSlug).subscribe({
      next: (res) => {
        this.app = res;
        this.initializeAccess();
      },
      error: (err) => this.error = err?.error?.message || 'Application not found'
    });
  }

  private initializeAccess() {
    if (!this.domain || !this.app) {
      return;
    }
    if (this.isOwnerContext()) {
      this.domainAccess.permissions = [
        'DOMAIN_MANAGE_APPS',
        'DOMAIN_USE_APP'
      ];
      this.domainAccess.groups = ['Domain Admin'];
      this.loadAppGroups();
      return;
    }
    if (this.auth.isLoggedIn()) {
      this.domainService.getDomainRoles(this.domainSlug).subscribe({
        next: res => {
          this.domainAccess = res || { permissions: [], groups: [] };
          if (this.canManageApp) {
            this.loadAppGroups();
          }
        },
        error: () => { /* ignore */ }
      });
    }
  }

  goBackToDomain() {
    this.router.navigate(['/domain', this.domainSlug]);
  }

  private contextMatchesDomain(): boolean {
    const ctx = this.auth.getContext();
    return !!(ctx && this.domain && ctx.domainId === this.domain.id);
  }

  isOwnerContext(): boolean {
    const ctx = this.auth.getContext();
    return !!(ctx && ctx.principalType === 'OWNER');
  }

  get canManageApp(): boolean {
    return this.isOwnerContext() || this.hasPermission('DOMAIN_MANAGE_APPS');
  }

  private hasPermission(code: string): boolean {
    return this.domainAccess.permissions.includes(code);
  }

  // Load app groups and users
  loadAppGroups() {
    this.appGroupsLoading = true;
    this.groupError = '';

    this.domainService.getAppGroups(this.domainSlug, this.appSlug).subscribe({
      next: (groups) => {
        this.appGroups = groups || [];
        this.loadAppUsers();
      },
      error: (err) => {
        this.groupError = err?.error?.message || 'Failed to load groups';
        this.appGroupsLoading = false;
      }
    });
  }

  loadAppUsers() {
    this.usersLoading = true;
    this.domainService.getAppUsersWithGroups(this.domainSlug, this.appSlug).subscribe({
      next: (users) => {
        this.appUsers = users || [];
        this.usersLoading = false;
        this.appGroupsLoading = false;
      },
      error: (err) => {
        console.error('Failed to load users:', err);
        this.usersLoading = false;
        this.appGroupsLoading = false;
      }
    });
  }

  // Drag and drop handlers
  onDragStart(event: DragEvent, user: any, fromGroupId: string | null) {
    this.draggedUser = user;
    this.draggedFromGroupId = fromGroupId;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onDrop(event: DragEvent, targetGroupId: string) {
    event.preventDefault();
    
    if (!this.draggedUser) {
      return;
    }

    // Check if user is already in this group
    const isAlreadyInGroup = this.draggedUser.appGroups?.some(
      (g: any) => g.groupId === targetGroupId
    );

    if (isAlreadyInGroup) {
      alert('User is already in this group');
      this.draggedUser = null;
      this.draggedFromGroupId = null;
      return;
    }

    // Add user to the target group
    this.domainService.addAppGroupMember(
      this.domainSlug,
      this.appSlug,
      targetGroupId,
      this.draggedUser.username
    ).subscribe({
      next: () => {
        this.loadAppGroups();
        this.draggedUser = null;
        this.draggedFromGroupId = null;
      },
      error: (err) => {
        alert(err?.error?.message || 'Failed to add user to group');
        this.draggedUser = null;
        this.draggedFromGroupId = null;
      }
    });
  }

  onDragEnd() {
    this.draggedUser = null;
    this.draggedFromGroupId = null;
  }

  // Helper methods to filter users
  getUsersInGroup(groupId: string): any[] {
    return this.appUsers.filter(user =>
      user.appGroups?.some((g: any) => g.groupId === groupId)
    );
  }

  getUsersWithoutGroups(): any[] {
    return this.appUsers.filter(user =>
      !user.appGroups || user.appGroups.length === 0
    );
  }

  // Remove user from group
  removeUserFromGroup(userId: string, groupId: string) {
    if (!confirm('Remove this user from the group?')) {
      return;
    }

    this.domainService.removeAppGroupMember(
      this.domainSlug,
      this.appSlug,
      groupId,
      userId
    ).subscribe({
      next: () => {
        this.loadAppGroups();
      },
      error: (err) => {
        alert(err?.error?.message || 'Failed to remove user from group');
      }
    });
  }
}
