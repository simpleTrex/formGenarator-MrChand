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

interface DomainGroup {
  id: string;
  name: string;
  permissions: string[];
  defaultGroup: boolean;
}

@Component({
  selector: 'app-domain-users',
  templateUrl: './domain-users.component.html',
  styleUrls: ['./domain-users.component.css']
})
export class DomainUsersComponent implements OnInit {
  slug = '';
  loading = false;
  error = '';

  users: DomainUser[] = [];
  groups: DomainGroup[] = [];

  draggedUser: DomainUser | null = null;
  draggedFromGroupId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private domainService: DomainService
  ) { }

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
      this.domainService.getDomainGroups(this.slug).toPromise(),
      this.domainService.getDomainUsersWithGroups(this.slug).toPromise()
    ]).then(([groups, users]) => {
      this.groups = groups || [];
      this.users = users || [];
      this.loading = false;
    }).catch(err => {
      this.error = 'Failed to load domain data';
      this.loading = false;
      console.error(err);
    });
  }

  getUsersInGroup(groupId: string): DomainUser[] {
    return this.users.filter(user =>
      user.groups.some(g => g.groupId === groupId)
    );
  }

  getUsersWithoutGroups(): DomainUser[] {
    return this.users.filter(user => user.groups.length === 0);
  }

  onDragStart(event: DragEvent, user: DomainUser, fromGroupId: string | null): void {
    this.draggedUser = user;
    this.draggedFromGroupId = fromGroupId;
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/html', event.target as any);
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

    const alreadyInTarget = user.groups.some(g => g.groupId === targetGroupId);
    if (alreadyInTarget) {
      this.draggedUser = null;
      this.draggedFromGroupId = null;
      return;
    }

    this.domainService.addDomainGroupMember(this.slug, targetGroupId, user.username).toPromise()
    .then(() => {
      this.loadData();
    }).catch(err => {
      this.error = 'Failed to update user group membership';
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

  removeUserFromGroup(user: DomainUser, groupId: string): void {
    if (!confirm(`Remove ${user.username} from this group?`)) {
      return;
    }

    this.domainService.removeDomainGroupMember(this.slug, groupId, user.id)
      .toPromise()
      .then(() => {
        this.loadData();
      })
      .catch(err => {
        this.error = 'Failed to remove user from group';
        console.error(err);
      });
  }

  permissionLabel(permission: string): string {
    const labels: Record<string, string> = {
      DOMAIN_MANAGE_USERS: 'Manage Members & Groups',
      DOMAIN_MANAGE_APPS: 'Manage Applications',
      DOMAIN_MANAGE_WORKFLOWS: 'Manage Workflows',
      DOMAIN_USE_APP: 'Use Apps'
    };
    return labels[permission] || permission;
  }
}
