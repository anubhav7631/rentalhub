import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TenantService } from '../tenant.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TenantResponse } from '../../../core/models/tenant.models';

@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './tenant-list.component.html',
  styleUrls: ['./tenant-list.component.scss'],
})
export class TenantListComponent implements OnInit {
  displayedColumns = ['fullName', 'property', 'phone', 'moveInDate', 'actions'];
  tenants: TenantResponse[] = [];

  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  isLoading = true;

  constructor(
    private tenantService: TenantService,
    private notification: NotificationService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadTenants();
  }

  loadTenants(): void {
    this.isLoading = true;
    this.tenantService.list(this.pageIndex, this.pageSize).subscribe({
      next: (response) => {
        this.tenants = response.content;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.notification.error('Failed to load tenants');
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadTenants();
  }

  goToAssign(): void {
    this.router.navigate(['/tenants/new']);
  }

  goToDetail(id: number): void {
    this.router.navigate(['/tenants', id]);
  }

  calculateDuration(moveInDate: string): string {
    const start = new Date(moveInDate);
    const now = new Date();
    const months =
      (now.getFullYear() - start.getFullYear()) * 12 +
      (now.getMonth() - start.getMonth());
    if (months < 1) return 'Less than a month';
    if (months === 1) return '1 month';
    if (months < 12) return `${months} months`;
    const years = Math.floor(months / 12);
    const remMonths = months % 12;
    return `${years} ${years === 1 ? 'year' : 'years'}${remMonths ? `, ${remMonths} mo` : ''}`;
  }
}
