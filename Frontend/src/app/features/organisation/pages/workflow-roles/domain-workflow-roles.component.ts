import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DomainService } from '../../../../core/services/domain.service';

interface GroupMembership {
  groupId: string;
  groupName: string;
  assignedAt: string;
}

interface DomainUser {
  id: string;
  username: string;
  email: string;
  status: string;
  groups: GroupMembership[];
}

interface WorkflowRole {
  id: string;
  name: string;
}

@Component({
  selector: 'app-domain-workflow-roles',
  templateUrl: './domain-workflow-roles.component.html',
  styleUrls: ['./domain-workflow-roles.component.css']
})
export class DomainWorkflowRolesComponent implements OnInit {
  slug = '';
  loading = false;
  error = '';

  users: DomainUser[] = [];
  roles: WorkflowRole[] = [];

  draggedUser: DomainUser | null = null;

  showRoleForm = false;
  roleFormMode: 'create' | 'edit' = 'create';
  editingRole: WorkflowRole | null = null;
  roleName = '';
  savingRole = false;

  constructor(
    private route: ActivatedRoute,
    private domainService: DomainService
  ) {}

  ngOnInit(): void {
    this.slug = this.route.snapshot.paramMap.get('slug') || '';
    if (this.slug) {
      this.loadData();
    }
  }

  loadData(): void {
    this.loading = true;
    this.error = '';

    Promise.all([
      this.domainService.getWorkflowRoles(this.slug).toPromise(),
      this.domainService.getUsersWithWorkflowRoles(this.slug).toPromise()
    ]).then(([roles, users]) => {
      this.roles = roles || [];
      this.users = users || [];
      this.loading = false;
    }).catch(err => {
      this.error = err?.error?.message || err?.error || 'Failed to load workflow roles';
      this.loading = false;
      console.error(err);
    });
  }

  getUsersInRole(roleId: string): DomainUser[] {
    return this.users.filter(user => user.groups.some(g => g.groupId === roleId));
  }

  getUsersWithoutRoles(): DomainUser[] {
    return this.users.filter(user => user.groups.length === 0);
  }

  onDragStart(event: DragEvent, user: DomainUser): void {
    this.draggedUser = user;
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

  onDrop(event: DragEvent, roleId: string): void {
    event.preventDefault();
    event.stopPropagation();

    if (!this.draggedUser) return;

    const user = this.draggedUser;
    const alreadyInRole = user.groups.some(g => g.groupId === roleId);
    if (alreadyInRole) {
      this.draggedUser = null;
      return;
    }

    this.domainService.addWorkflowRoleMember(this.slug, roleId, user.username).toPromise()
      .then(() => this.loadData())
      .catch(err => {
        this.error = err?.error?.message || err?.error || 'Failed to assign role';
      })
      .finally(() => {
        this.draggedUser = null;
      });
  }

  onDragEnd(): void {
    this.draggedUser = null;
  }

  removeUserFromRole(user: DomainUser, roleId: string): void {
    if (!confirm(`Remove ${user.username} from this role?`)) return;

    this.domainService.removeWorkflowRoleMember(this.slug, roleId, user.id).toPromise()
      .then(() => this.loadData())
      .catch(err => {
        this.error = err?.error?.message || err?.error || 'Failed to remove role assignment';
      });
  }

  openCreateRole(): void {
    this.showRoleForm = true;
    this.roleFormMode = 'create';
    this.editingRole = null;
    this.roleName = '';
    this.error = '';
  }

  openEditRole(role: WorkflowRole): void {
    this.showRoleForm = true;
    this.roleFormMode = 'edit';
    this.editingRole = role;
    this.roleName = role.name;
    this.error = '';
  }

  cancelRoleForm(): void {
    if (this.savingRole) return;
    this.showRoleForm = false;
    this.editingRole = null;
    this.roleName = '';
  }

  saveRole(): void {
    const name = (this.roleName || '').trim();
    if (!name) {
      this.error = 'Role name is required';
      return;
    }

    this.savingRole = true;
    this.error = '';

    const request = this.roleFormMode === 'edit' && this.editingRole
      ? this.domainService.updateWorkflowRole(this.slug, this.editingRole.id, { name }).toPromise()
      : this.domainService.createWorkflowRole(this.slug, { name }).toPromise();

    request.then(() => {
      this.cancelRoleForm();
      this.loadData();
    }).catch((err: any) => {
      this.error = err?.error?.message || err?.error || 'Failed to save role';
    }).finally(() => {
      this.savingRole = false;
    });
  }

  deleteRole(role: WorkflowRole): void {
    if (!confirm(`Delete workflow role ${role.name}?`)) return;

    this.domainService.deleteWorkflowRole(this.slug, role.id).toPromise()
      .then(() => this.loadData())
      .catch((err: any) => {
        this.error = err?.error?.message || err?.error || 'Failed to delete role';
      });
  }
}
