import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { PropertyService } from '../property.service';
import { TenantService } from '../../tenant/tenant.service';
import { NotificationService } from '../../../core/services/notification.service';
import { PropertyResponse } from '../../../core/models/property.models';
import { TenantResponse } from '../../../core/models/tenant.models';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-property-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDialogModule,
  ],
  templateUrl: './property-detail.component.html',
  styleUrls: ['./property-detail.component.scss'],
})
export class PropertyDetailComponent implements OnInit {
  property: PropertyResponse | null = null;
  currentTenant: TenantResponse | null = null;
  isLoading = true;
  activeImageIndex = 0;

  constructor(
    private propertyService: PropertyService,
    private tenantService: TenantService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.propertyService.getById(id).subscribe({
      next: (property) => {
        this.property = property;
        this.isLoading = false;
        if (property.status === 'OCCUPIED') {
          this.loadCurrentTenant(id);
        }
      },
      error: () => {
        this.isLoading = false;
        this.notification.error('Property not found');
        this.router.navigate(['/properties']);
      },
    });
  }

  private loadCurrentTenant(propertyId: number): void {
    this.tenantService.getCurrentTenant(propertyId).subscribe({
      next: (tenant) => (this.currentTenant = tenant),
      error: () => {}, // non-critical — silently ignore
    });
  }

  goToTenant(): void {
    if (this.currentTenant)
      this.router.navigate(['/tenants', this.currentTenant.id]);
  }

  assignTenant(): void {
    this.router.navigate(['/tenants/new'], {
      queryParams: { propertyId: this.property!.id },
    });
  }

  getImageUrl(filePath: string): string {
    return this.propertyService.getImageUrl(filePath);
  }

  get activeImage(): string | null {
    if (!this.property?.images.length) return null;
    return this.getImageUrl(
      this.property.images[this.activeImageIndex].filePath,
    );
  }

  selectImage(index: number): void {
    this.activeImageIndex = index;
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(value);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'status-badge--success';
      case 'OCCUPIED':
        return 'status-badge--info';
      case 'MAINTENANCE':
        return 'status-badge--warning';
      case 'INACTIVE':
        return 'status-badge--danger';
      default:
        return '';
    }
  }

  edit(): void {
    this.router.navigate(['/properties', this.property!.id, 'edit']);
  }

  confirmDelete(): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete property?',
        message: `"${this.property!.propertyName}" will be marked inactive.`,
        confirmLabel: 'Delete',
        confirmColor: 'warn',
      },
    });
    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.propertyService.delete(this.property!.id).subscribe({
          next: () => {
            this.notification.success('Property deleted');
            this.router.navigate(['/properties']);
          },
          error: (err) =>
            this.notification.error(err.error?.message || 'Failed to delete'),
        });
      }
    });
  }

  back(): void {
    this.router.navigate(['/properties']);
  }
}
