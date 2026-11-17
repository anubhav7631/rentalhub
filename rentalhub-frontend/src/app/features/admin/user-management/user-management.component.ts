import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  MatSlideToggleModule,
  MatSlideToggleChange,
} from '@angular/material/slide-toggle';
import { environment } from '../../../../environments/environment';
import { NotificationService } from '../../../core/services/notification.service';
import { PageResponse, UserResponse } from '../../../core/models/user.models';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatSlideToggleModule,
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss'],
})
export class UserManagementComponent implements OnInit {
  private readonly apiUrl = `${environment.apiUrl}/admin/users`;

  displayedColumns = [
    'id',
    'fullName',
    'email',
    'roles',
    'isActive',
    'createdAt',
  ];
  users: UserResponse[] = [];

  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;

  isLoading = true;

  constructor(
    private http: HttpClient,
    private notification: NotificationService,
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  /**
   * loadUsers — calls GET /api/admin/users?page=X&size=Y
   *
   * HttpParams: Angular's immutable query-parameter builder.
   * .set() returns a NEW HttpParams instance (immutability — same
   * principle as HttpRequest.clone() in the interceptor).
   *
   * The interceptor (Phase 7) automatically attaches the JWT.
   * If the current user is NOT an admin, this call returns 403 —
   * but roleGuard already prevented non-admins from reaching this
   * component, so in normal usage this never happens. Defense in depth
   * means the backend check still exists as a safety net.
   */
  loadUsers(): void {
    this.isLoading = true;

    const params = new HttpParams()
      .set('page', this.pageIndex.toString())
      .set('size', this.pageSize.toString())
      .set('sort', 'createdAt,desc');

    this.http
      .get<PageResponse<UserResponse>>(this.apiUrl, { params })
      .subscribe({
        next: (response) => {
          this.users = response.content;
          this.totalElements = response.totalElements;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
          this.notification.error('Failed to load users');
        },
      });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  /**
   * toggleUserStatus — calls PATCH /api/admin/users/{id}/status
   *
   * MatSlideToggleChange carries the new checked state.
   * We optimistically don't update the UI until the API confirms —
   * if the call fails, the toggle visually reverts (Material handles
   * this automatically when we don't update the bound model).
   */
  toggleUserStatus(user: UserResponse, event: MatSlideToggleChange): void {
    const newStatus = event.checked;

    this.http
      .patch<UserResponse>(`${this.apiUrl}/${user.id}/status`, {
        isActive: newStatus,
      })
      .subscribe({
        next: (updated) => {
          user.isActive = updated.isActive;
          this.notification.success(
            `${user.fullName} ${newStatus ? 'activated' : 'deactivated'}`,
          );
        },
        error: () => {
          // Revert the toggle visually on failure
          event.source.checked = user.isActive;
          this.notification.error('Failed to update user status');
        },
      });
  }

  getRoleLabel(role: string): string {
    return role.replace('ROLE_', '');
  }

  getRoleColor(role: string): string {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'admin';
      case 'ROLE_OWNER':
        return 'owner';
      case 'ROLE_TENANT':
        return 'tenant';
      default:
        return '';
    }
  }
}
