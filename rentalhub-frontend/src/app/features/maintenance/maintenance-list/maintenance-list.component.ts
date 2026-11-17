import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MaintenanceService } from '../maintenance.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { MaintenanceResponse } from '../../../core/models/maintenance.models';

@Component({
  selector: 'app-maintenance-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './maintenance-list.component.html',
  styleUrls: ['./maintenance-list.component.scss'],
})
export class MaintenanceListComponent implements OnInit {
  displayedColumns = [
    'title',
    'property',
    'tenant',
    'category',
    'status',
    'date',
    'actions',
  ];

  requests: MaintenanceResponse[] = [];
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  isLoading = true;

  statusFilter = '';
  isOwnerOrAdmin = false;

  readonly statusOptions = [
    { value: '', label: 'All Statuses' },
    { value: 'OPEN', label: 'Open' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'REJECTED', label: 'Rejected' },
  ];

  constructor(
    private maintenanceService: MaintenanceService,
    private notification: NotificationService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.isOwnerOrAdmin = this.authService.hasAnyRole([
      'ROLE_ADMIN',
      'ROLE_OWNER',
    ]);
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.maintenanceService
      .list(this.pageIndex, this.pageSize, this.statusFilter || undefined)
      .subscribe({
        next: (page) => {
          this.requests = page.content;
          this.totalElements = page.totalElements;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
          this.notification.error('Failed to load maintenance requests');
        },
      });
  }

  onFilterChange(): void {
    this.pageIndex = 0;
    this.load();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  goToCreate(): void {
    this.router.navigate(['/maintenance/new']);
  }
  goToDetail(id: number): void {
    this.router.navigate(['/maintenance', id]);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN':
        return 'chip-open';
      case 'IN_PROGRESS':
        return 'chip-inprogress';
      case 'COMPLETED':
        return 'chip-completed';
      case 'REJECTED':
        return 'chip-rejected';
      default:
        return '';
    }
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'OPEN':
        return 'fiber_new';
      case 'IN_PROGRESS':
        return 'autorenew';
      case 'COMPLETED':
        return 'check_circle';
      case 'REJECTED':
        return 'cancel';
      default:
        return 'help';
    }
  }
}
