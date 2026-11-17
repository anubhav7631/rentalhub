import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatBadgeModule } from '@angular/material/badge';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { AppRole } from '../../core/models/user.models';

export interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles: string[]; // ← which roles see this item
  badge?: number;
  dividerAfter?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatTooltipModule,
    MatDividerModule,
    MatBadgeModule,
  ],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss'],
})
export class SidebarComponent {
  @Output() closeSidenav = new EventEmitter<void>();

  constructor(
    private router: Router,
    private authService: AuthService,
  ) {}

  
  private allNavItems: NavItem[] = [
    {
      label: 'Dashboard',
      icon: 'dashboard',
      route: '/dashboard',
      roles: [AppRole.ADMIN, AppRole.OWNER, AppRole.TENANT],
    },

    {
      label: 'Properties',
      icon: 'apartment',
      route: '/properties',
      roles: [AppRole.ADMIN, AppRole.OWNER],
    },

    {
      label: 'Tenants',
      icon: 'people',
      route: '/tenants',
      roles: [AppRole.ADMIN, AppRole.OWNER],
    },

    {
      label: 'Agreements',
      icon: 'description',
      route: '/agreements',
      roles: [AppRole.ADMIN, AppRole.OWNER, AppRole.TENANT],
      dividerAfter: true,
    },

    {
      label: 'Rent & Payments',
      icon: 'payments',
      route: '/payments',
      roles: [AppRole.ADMIN, AppRole.OWNER, AppRole.TENANT],
    },

    {
      label: 'Maintenance',
      icon: 'construction',
      route: '/maintenance',
      roles: [AppRole.ADMIN, AppRole.OWNER, AppRole.TENANT],
      dividerAfter: true,
    },

    {
      label: 'Notifications',
      icon: 'notifications',
      route: '/notifications',
      roles: [AppRole.ADMIN, AppRole.OWNER, AppRole.TENANT],
      badge: 3,
    },

    {
      label: 'Reports',
      icon: 'bar_chart',
      route: '/reports',
      roles: [AppRole.ADMIN, AppRole.OWNER],
      dividerAfter: true,
    },

    {
      label: 'Users',
      icon: 'manage_accounts',
      route: '/admin/users',
      roles: [AppRole.ADMIN],
    },

    {
      label: 'Settings',
      icon: 'settings',
      route: '/settings',
      roles: [AppRole.ADMIN, AppRole.OWNER, AppRole.TENANT],
    },
  ];

  
  get visibleNavItems(): NavItem[] {
    return this.allNavItems.filter((item) =>
      this.authService.hasAnyRole(item.roles),
    );
  }

  isActive(route: string): boolean {
    return this.router.url.startsWith(route);
  }

  onNavClick(): void {
    this.closeSidenav.emit();
  }
}
